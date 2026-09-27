package com.autodocs.app

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.backup.BackupException
import com.autodocs.app.data.backup.BackupManager
import com.autodocs.app.data.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRoundTripTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var manager: BackupManager

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        manager = BackupManager(context, db)
    }

    @After fun tearDown() { db.close() }

    private fun seed(): File = runBlocking {
        val photo = File(PhotoStorage.photosDir(context), "car_123.jpg").apply { writeBytes(ByteArray(5000) { (it % 251).toByte() }) }
        val recPhoto = File(PhotoStorage.photosDir(context), "rec_1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val dao = db.backupDao()
        dao.insertCars(listOf(
            Car(id = 1, name = "Старий", make = "VAZ", model = "2107", engine = "", fuelType = FuelType.UNKNOWN,
                transmissionType = TransmissionType.UNKNOWN, licensePlate = "", vin = "", isArchived = true,
                mileage = 0, mileageUpdatedAt = 10, createdAt = 11),
            Car(id = 2, name = "Мій Passat", make = "Volkswagen", model = "Passat B5+", engine = "2.0 AZM",
                fuelType = FuelType.PETROL, transmissionType = TransmissionType.MANUAL, licensePlate = "ВТ7164ВМ",
                vin = "WVWZZZ3BZ3P000000", photoUri = Uri.fromFile(photo).toString(), mileage = 358248,
                mileageUpdatedAt = 20, createdAt = 21)
        ))
        dao.insertWorkTypes(listOf(
            WorkType(id = 5, name = "Заміна масла двигуна", category = WorkItemCategory.ROBOTA),
            WorkType(id = 9, name = "Масло моторне", category = WorkItemCategory.ZAPCHASTYNA, isCustom = true)
        ))
        dao.insertRecords(listOf(ServiceRecord(id = 3, carId = 2, date = 1790380800000, mileage = 358100,
            stoName = "СТО \"Шевченка\"", notes = "рядок1\nрядок2 ✓", createdAt = 30)))
        dao.insertRecordItems(listOf(
            ServiceRecordItem(id = 7, recordId = 3, workTypeId = 5, customName = "Заміна масла двигуна", category = WorkItemCategory.ROBOTA, price = 400.0),
            ServiceRecordItem(id = 8, recordId = 3, workTypeId = null, customName = "Фільтр", category = WorkItemCategory.ZAPCHASTYNA, price = 1850.55)
        ))
        dao.insertRules(listOf(MaintenanceRule(id = 1, carId = 2, workTypeId = 5, intervalKm = 10000, intervalMonths = null, lastDoneMileage = 350000, lastDoneDate = null)))
        dao.insertMileage(listOf(MileageEntry(id = 4, carId = 2, mileage = 358248, date = 40)))
        dao.insertPhotos(listOf(Photo(id = 6, ownerType = PhotoOwnerType.SERVICE_RECORD, ownerId = 3, kind = PhotoKind.RECORD_PHOTO, uri = Uri.fromFile(recPhoto).toString(), createdAt = 50)))
        // Схема v2 (етап 9): документ із фото поліса, разовий план, закритий записом, СТО, шини.
        val policy = File(PhotoStorage.photosDir(context), "doc_1.jpg").apply { writeBytes(byteArrayOf(7, 7, 7)) }
        dao.insertDocuments(listOf(
            CarDocument(id = 11, carId = 2, type = DocumentType.OSAGO, number = "EP-123", company = "ТАС",
                validFrom = 1790380800000, validUntil = 1821830400000, price = 1450.5, notes = "е-поліс", createdAt = 60),
            CarDocument(id = 12, carId = 2, type = DocumentType.OTHER, title = "Довіреність", validUntil = 1821830400000, createdAt = 61)
        ))
        dao.insertPhotos(listOf(Photo(id = 13, ownerType = PhotoOwnerType.CAR_DOCUMENT, ownerId = 11, kind = PhotoKind.SCAN, uri = Uri.fromFile(policy).toString(), createdAt = 62)))
        dao.insertTasks(listOf(
            PlannedTask(id = 21, carId = 2, title = "Шарові опори", dueDate = 1798761600000, dueMileage = 365000, notes = "стукіт справа", createdAt = 70),
            PlannedTask(id = 22, carId = 2, title = "Заміна масла двигуна", doneAt = 1790380800000, doneRecordId = 3, createdAt = 71)
        ))
        dao.insertStations(listOf(Station(id = 31, name = "СТО \"Шевченка\"", phone = "+380501112233", address = "вул. Шевченка, 1", createdAt = 80)))
        dao.insertTireSets(listOf(TireSet(id = 41, carId = 2, name = "Зима", season = TireSeason.WINTER, brand = "Nokian", size = "205/55 R16", year = 2021, createdAt = 90)))
        dao.insertTireSwaps(listOf(TireSwap(id = 51, carId = 2, tireSetId = 41, date = 1790380800000, mileage = 358000)))
        photo
    }

    private suspend fun snapshot() = with(db.backupDao()) {
        listOf(allCars(), allWorkTypes(), allRecords(), allRecordItems(), allRules(), allMileage(), allPhotos(),
            allDocuments(), allTasks(), allStations(), allTireSets(), allTireSwaps())
    }

    @Test fun roundTrip_restoresEverythingExactly() = runBlocking {
        val photo = seed()
        val photoBytes = photo.readBytes()
        val before = snapshot()

        val out = ByteArrayOutputStream()
        val exported = manager.export(out)
        assertEquals(2, exported.cars)
        assertEquals("Мій Passat", exported.activeCarName)
        assertEquals(1, exported.records)
        assertEquals(3, exported.photos)

        // Змінюємо дані й видаляємо фото — відновлення має все повернути.
        db.backupDao().clearRecordItems()
        db.backupDao().insertCars(listOf(Car(id = 99, name = "Зайве", make = "X", model = "Y", engine = "",
            fuelType = FuelType.GAS, transmissionType = TransmissionType.ROBOT, licensePlate = "", vin = "")))
        PhotoStorage.photosDir(context).listFiles()!!.forEach { it.delete() }
        File(PhotoStorage.photosDir(context), "junk.jpg").writeBytes(byteArrayOf(9))

        val summary = manager.readSummary(ByteArrayInputStream(out.toByteArray()))
        assertEquals(2, summary.cars)

        manager.restore(ByteArrayInputStream(out.toByteArray()))
        val after = snapshot()
        assertEquals(before, after)
        assertArrayEquals(photoBytes, photo.readBytes())
        assertFalse(File(PhotoStorage.photosDir(context), "junk.jpg").exists())
        assertTrue(File(PhotoStorage.photosDir(context), "rec_1.jpg").exists())
        assertTrue(File(PhotoStorage.photosDir(context), "doc_1.jpg").exists())
    }

    /** Копія зі старої версії (схема v1, без нових масивів) відновлюється в v2: нові таблиці — порожні. */
    @Test fun v1Backup_restoresIntoV2() = runBlocking {
        seed()
        val good = ByteArrayOutputStream().also { manager.export(it) }.toByteArray()
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            ZipInputStream(ByteArrayInputStream(good)).use { zin ->
                while (true) {
                    val e = zin.nextEntry ?: break
                    z.putNextEntry(ZipEntry(e.name))
                    if (e.name == "data.json") {
                        val root = org.json.JSONObject(zin.readBytes().toString(Charsets.UTF_8))
                        listOf("carDocuments", "plannedTasks", "stations", "tireSets", "tireSwaps").forEach { root.remove(it) }
                        // Фото документа в старій версії не існувало.
                        val photos = root.getJSONArray("photos")
                        val kept = org.json.JSONArray()
                        for (i in 0 until photos.length()) {
                            val p = photos.getJSONObject(i)
                            if (p.getString("ownerType") != "CAR_DOCUMENT") kept.put(p)
                        }
                        root.put("photos", kept).put("schemaVersion", 1).put("appVersion", "0.8.0")
                        z.write(root.toString().toByteArray())
                    } else zin.copyTo(z)
                    z.closeEntry()
                }
            }
        }
        val summary = manager.restore(ByteArrayInputStream(out.toByteArray()))
        assertEquals(1, summary.schemaVersion)
        val dao = db.backupDao()
        assertEquals(2, dao.allCars().size)
        assertEquals(1, dao.allRecords().size)
        assertTrue(dao.allDocuments().isEmpty())
        assertTrue(dao.allTasks().isEmpty())
        assertTrue(dao.allTireSets().isEmpty())
        assertEquals(listOf(PhotoOwnerType.SERVICE_RECORD), dao.allPhotos().map { it.ownerType })
    }

    @Test fun garbageFile_isRejected_andDataUntouched() = runBlocking {
        seed()
        val before = snapshot()
        val bad = ByteArrayInputStream("hello, not a zip".toByteArray())
        val e = runCatching { manager.restore(bad) }.exceptionOrNull()
        assertTrue(e is BackupException)
        assertEquals(before, snapshot())
    }

    @Test fun newerSchema_isRejected() = runBlocking {
        seed()
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("data.json"))
            z.write("""{"format":"autodocs-backup","schemaVersion":99,"appVersion":"9.9"}""".toByteArray())
            z.closeEntry()
        }
        val e = runCatching { manager.readSummary(ByteArrayInputStream(out.toByteArray())) }.exceptionOrNull()
        assertTrue(e is BackupException)
        assertTrue(e!!.message!!.contains("новішою"))
    }

    @Test fun zipSlip_entriesIgnored() = runBlocking {
        seed()
        val good = ByteArrayOutputStream().also { manager.export(it) }.toByteArray()
        // Перепаковуємо, додаючи зловмисний запис.
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            ZipInputStream(ByteArrayInputStream(good)).use { zin ->
                while (true) {
                    val e = zin.nextEntry ?: break
                    z.putNextEntry(ZipEntry(e.name)); zin.copyTo(z); z.closeEntry()
                }
            }
            z.putNextEntry(ZipEntry("photos/../../evil.txt")); z.write(byteArrayOf(1)); z.closeEntry()
        }
        manager.restore(ByteArrayInputStream(out.toByteArray()))
        assertFalse(File(context.filesDir, "evil.txt").exists())
        assertFalse(File(context.cacheDir, "evil.txt").exists())
        assertEquals(2, db.backupDao().allCars().size)
    }
}
