package com.autodocs.app.data.backup

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.withTransaction
import com.autodocs.app.BuildConfig
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.AppPrefs
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.backup.BackupJson.mapObjects
import com.autodocs.app.data.backup.BackupJson.toJsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Короткий опис бекапу — для підтвердження перед відновленням і для повідомлення після експорту. */
data class BackupSummary(
    val exportedAt: Long,
    val appVersion: String,
    val schemaVersion: Int,
    val cars: Int,
    val activeCarName: String?,
    val records: Int,
    val photos: Int
)

/** Помилка з людським повідомленням українською — показується як є. */
class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * F12: резервна копія в один ZIP.
 *
 * Формат архіву:
 *  - `data.json` — { format, schemaVersion, appVersion, exportedAt, photoFiles[], cars[], workTypes[],
 *    serviceRecords[], serviceRecordItems[], maintenanceRules[], mileageEntries[], photos[] }
 *  - `photos/<ім'я>` — файли фото. У JSON фото посилаються відносним шляхом `photos/<ім'я>`.
 *
 * Відновлення — ПОВНА заміна даних (одна транзакція Room), id зберігаються як були,
 * тож усі зв'язки між таблицями лишаються цілими.
 */
class BackupManager(private val context: Context, private val db: AppDatabase) {

    companion object {
        const val FORMAT = "autodocs-backup"
        private const val DATA_ENTRY = "data.json"
        private const val PHOTOS_PREFIX = "photos/"
        private val SAFE_NAME = Regex("^[A-Za-z0-9._-]{1,120}$")
        private const val MAX_JSON_BYTES = 50L * 1024 * 1024
    }

    private val dao get() = db.backupDao()

    // ======================= ЕКСПОРТ =======================

    suspend fun export(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw BackupException("Не вдалося відкрити файл для запису")
        out.use { export(it) }
    }

    /** Джерело фото для архіву: або готовий файл, або перекодовані байти (для content://). */
    private sealed interface PhotoSource {
        data class FromFile(val file: File) : PhotoSource
        class FromBytes(val bytes: ByteArray) : PhotoSource
    }

    suspend fun export(output: OutputStream): BackupSummary = withContext(Dispatchers.IO) {
        val cars = dao.allCars()
        val workTypes = dao.allWorkTypes()
        val records = dao.allRecords()
        val items = dao.allRecordItems()
        val rules = dao.allRules()
        val mileage = dao.allMileage()
        val photos = dao.allPhotos()

        // Збираємо фото: однаковий URI → один файл в архіві.
        val pathByUri = LinkedHashMap<String, String>()
        val sources = LinkedHashMap<String, PhotoSource>() // zip-шлях → джерело
        var extCounter = 0

        fun pack(uri: String?): String? {
            if (uri.isNullOrBlank()) return null
            pathByUri[uri]?.let { return it }
            val parsed = Uri.parse(uri)
            val source: PhotoSource
            var name: String
            val file = if (parsed.scheme == "file") parsed.path?.let(::File) else null
            if (file != null && file.isFile) {
                source = PhotoSource.FromFile(file)
                name = file.name.takeIf { SAFE_NAME.matches(it) } ?: "photo_${++extCounter}.jpg"
            } else {
                // Старе посилання на галерею (content://) — перекодовуємо в JPEG, якщо ще доступне.
                val bitmap = PhotoStorage.decodeScaled(context, parsed, 1600) ?: return null
                val bytes = ByteArrayOutputStream().use { bos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, bos)
                    bos.toByteArray()
                }
                source = PhotoSource.FromBytes(bytes)
                name = "ext_${++extCounter}.jpg"
            }
            while (sources.containsKey(PHOTOS_PREFIX + name)) name = "dup${++extCounter}_$name"
            val path = PHOTOS_PREFIX + name
            sources[path] = source
            pathByUri[uri] = path
            return path
        }

        val carsJson = JSONArray()
        cars.forEach { carsJson.put(BackupJson.carToJson(it, pack(it.photoUri))) }
        val photosJson = JSONArray()
        photos.forEach { p -> pack(p.uri)?.let { path -> photosJson.put(BackupJson.photoToJson(p, path)) } }

        val exportedAt = System.currentTimeMillis()
        val root = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", AppDatabase.SCHEMA_VERSION)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("exportedAt", exportedAt)
            .put("photoFiles", JSONArray(sources.keys.toList()))
            .put("cars", carsJson)
            .put("workTypes", workTypes.toJsonArray(BackupJson::workTypeToJson))
            .put("serviceRecords", records.toJsonArray(BackupJson::recordToJson))
            .put("serviceRecordItems", items.toJsonArray(BackupJson::itemToJson))
            .put("maintenanceRules", rules.toJsonArray(BackupJson::ruleToJson))
            .put("mileageEntries", mileage.toJsonArray(BackupJson::mileageToJson))
            .put("photos", photosJson)

        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(root.toString(1).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            sources.forEach { (path, source) ->
                zip.putNextEntry(ZipEntry(path))
                when (source) {
                    is PhotoSource.FromFile -> source.file.inputStream().use { it.copyTo(zip) }
                    is PhotoSource.FromBytes -> zip.write(source.bytes)
                }
                zip.closeEntry()
            }
        }

        AppPrefs.get(context).edit().putLong(AppPrefs.KEY_LAST_BACKUP_AT, exportedAt).apply()
        summaryOf(root)
    }

    // ======================= ЧИТАННЯ ОПИСУ =======================

    /** Читає лише data.json і перевіряє, що це наш бекап підтримуваної версії. */
    suspend fun readSummary(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw BackupException("Не вдалося відкрити файл")
        input.use { readSummary(it) }
    }

    suspend fun readSummary(input: InputStream): BackupSummary = withContext(Dispatchers.IO) {
        var root: JSONObject? = null
        runCatching {
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == DATA_ENTRY) {
                        root = parseRoot(zip)
                        break
                    }
                }
            }
        }.onFailure { if (it is BackupException) throw it else throw notABackup(it) }
        val r = root ?: throw notABackup(null)
        validate(r)
        summaryOf(r)
    }

    // ======================= ВІДНОВЛЕННЯ =======================

    suspend fun restore(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw BackupException("Не вдалося відкрити файл")
        input.use { restore(it) }
    }

    suspend fun restore(input: InputStream): BackupSummary = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            // 1. Розпакувати: data.json у пам'ять, фото — у тимчасову папку (лише безпечні імена).
            var root: JSONObject? = null
            try {
                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        val name = entry.name
                        when {
                            name == DATA_ENTRY -> root = parseRoot(zip)
                            name.startsWith(PHOTOS_PREFIX) -> {
                                val base = name.removePrefix(PHOTOS_PREFIX)
                                if (SAFE_NAME.matches(base) && base != "." && base != "..") {
                                    File(tempDir, base).outputStream().use { zip.copyTo(it) }
                                }
                            }
                            // інші файли ігноруємо
                        }
                    }
                }
            } catch (e: BackupException) {
                throw e
            } catch (e: Exception) {
                throw notABackup(e)
            }
            val r = root ?: throw notABackup(null)
            validate(r)

            // 2. Перетворити JSON у сутності; шляхи фото → майбутні file:// у files/photos.
            val photosDir = PhotoStorage.photosDir(context)
            fun resolve(path: String?): String? {
                val base = path?.removePrefix(PHOTOS_PREFIX) ?: return null
                if (!SAFE_NAME.matches(base) || !File(tempDir, base).isFile) return null
                return Uri.fromFile(File(photosDir, base)).toString()
            }

            val cars = r.optJSONArray("cars").mapObjects { BackupJson.carFromJson(it, resolve(BackupJson.optPhotoPath(it))) }
            val workTypes = r.optJSONArray("workTypes").mapObjects(BackupJson::workTypeFromJson)
            val records = r.optJSONArray("serviceRecords").mapObjects(BackupJson::recordFromJson)
            val items = r.optJSONArray("serviceRecordItems").mapObjects(BackupJson::itemFromJson)
            val rules = r.optJSONArray("maintenanceRules").mapObjects(BackupJson::ruleFromJson)
            val mileage = r.optJSONArray("mileageEntries").mapObjects(BackupJson::mileageFromJson)
            val photos = r.optJSONArray("photos").mapObjects { o ->
                resolve(BackupJson.optPhotoPath(o))?.let { BackupJson.photoFromJson(o, it) }
            }.filterNotNull()

            // 3. Повна заміна даних однією транзакцією: якщо щось не так — нічого не зміниться.
            try {
                db.withTransaction {
                    dao.clearRecordItems()
                    dao.clearRules()
                    dao.clearMileage()
                    dao.clearPhotos()
                    dao.clearRecords()
                    dao.clearWorkTypes()
                    dao.clearCars()

                    dao.insertCars(cars)
                    dao.insertWorkTypes(workTypes)
                    dao.insertRecords(records)
                    dao.insertRecordItems(items)
                    dao.insertRules(rules)
                    dao.insertMileage(mileage)
                    dao.insertPhotos(photos)
                }
            } catch (e: Exception) {
                throw BackupException("Файл копії пошкоджений: дані не узгоджуються. Поточні дані не змінено.", e)
            }

            // 4. Фото: старі файли прибираємо, відновлені переносимо на місце.
            photosDir.listFiles()?.forEach { it.delete() }
            tempDir.listFiles()?.forEach { src ->
                val dst = File(photosDir, src.name)
                if (!src.renameTo(dst)) {
                    src.inputStream().use { i -> dst.outputStream().use { o -> i.copyTo(o) } }
                }
            }

            // Довідник прийшов з копії — стартовий сід більше не потрібен.
            AppPrefs.get(context).edit().putBoolean(AppPrefs.KEY_WORK_TYPES_SEEDED, true).apply()
            summaryOf(r)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ======================= службове =======================

    private fun parseRoot(zip: ZipInputStream): JSONObject {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = zip.read(buffer)
            if (n < 0) break
            total += n
            if (total > MAX_JSON_BYTES) throw BackupException("Файл копії завеликий або пошкоджений")
            bytes.write(buffer, 0, n)
        }
        return JSONObject(bytes.toString(Charsets.UTF_8.name()))
    }

    private fun validate(root: JSONObject) {
        if (root.optString("format") != FORMAT) throw notABackup(null)
        val version = root.optInt("schemaVersion", -1)
        if (version < 1) throw notABackup(null)
        if (version > AppDatabase.SCHEMA_VERSION) {
            throw BackupException("Копію створено новішою версією AutoDocs (${root.optString("appVersion")}). Спершу онови застосунок.")
        }
    }

    private fun notABackup(cause: Throwable?) =
        BackupException("Це не резервна копія AutoDocs або файл пошкоджений", cause)

    private fun summaryOf(root: JSONObject): BackupSummary {
        val cars = root.optJSONArray("cars")
        var activeName: String? = null
        if (cars != null) {
            for (i in 0 until cars.length()) {
                val c = cars.getJSONObject(i)
                if (!c.optBoolean("isArchived", false)) { activeName = c.optString("name"); break }
            }
        }
        return BackupSummary(
            exportedAt = root.optLong("exportedAt", 0L),
            appVersion = root.optString("appVersion"),
            schemaVersion = root.optInt("schemaVersion", 0),
            cars = cars?.length() ?: 0,
            activeCarName = activeName,
            records = root.optJSONArray("serviceRecords")?.length() ?: 0,
            photos = root.optJSONArray("photoFiles")?.length() ?: 0
        )
    }
}
