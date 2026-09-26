package com.autodocs.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Фото зберігаємо КОПІЄЮ у внутрішній папці застосунку (files/photos), а не
 * посиланням на галерею: посилання від Photo Picker може "протухнути" після
 * перезавантаження чи видалення фото з галереї, а копію легко покласти в бекап (етап 4).
 * Під час імпорту фото зменшується до [MAX_SIDE] px — телефонні 12-50 Мп нам не потрібні.
 */
object PhotoStorage {
    private const val MAX_SIDE = 1600
    private const val JPEG_QUALITY = 88

    /** Папка з фото застосунку (files/photos). */
    fun photosDir(context: Context): File =
        File(context.filesDir, "photos").apply { mkdirs() }

    /** Документи (скани, техпаспорт) зберігаємо в більшій роздільності — щоб читався дрібний текст. */
    const val DOCUMENT_MAX_SIDE = 2400

    /**
     * Копіює зображення з [source] у сховище застосунку. Повертає `file://` URI або null.
     * Ім'я унікальне навіть для кількох сторінок, імпортованих в ту саму мілісекунду.
     */
    suspend fun importImage(context: Context, source: Uri, prefix: String, maxSide: Int = MAX_SIDE): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(context, source, maxSide) ?: return@runCatching null
            val suffix = java.util.UUID.randomUUID().toString().take(8)
            val file = File(photosDir(context), "${prefix}_${System.currentTimeMillis()}_$suffix.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    /** Видаляє файл, якщо він наш (у папці photos). Посилання на галерею не чіпає. */
    fun deleteIfOwned(context: Context, uri: String?) {
        if (uri == null) return
        val parsed = Uri.parse(uri)
        if (parsed.scheme != "file") return
        val file = File(parsed.path ?: return)
        if (file.parentFile?.canonicalPath == photosDir(context).canonicalPath) file.delete()
    }

    /** Тимчасовий файл для знімка з камери (cache/camera) — віддається камері через FileProvider. */
    fun newCameraFile(context: Context): File =
        File(File(context.cacheDir, "camera").apply { mkdirs() }, "shot_${System.currentTimeMillis()}.jpg")

    /** content:// URI для нашого файлу (камера, «Поділитися»). */
    fun contentUriFor(context: Context, file: File): Uri =
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /**
     * Декодує зображення (content:// або file://) зі зменшенням до [maxSide] по довшій стороні.
     * Основний шлях — ImageDecoder (враховує EXIF-поворот); якщо він не впорався —
     * запасний BitmapFactory (напр., нестандартний файл або тестове середовище).
     */
    fun decodeScaled(context: Context, uri: Uri, maxSide: Int): Bitmap? =
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val longest = max(w, h)
                if (longest > maxSide) {
                    val scale = maxSide.toFloat() / longest
                    decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull() ?: decodeWithBitmapFactory(context, uri, maxSide)

    private fun decodeWithBitmapFactory(context: Context, uri: Uri, maxSide: Int): Bitmap? = runCatching {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, opts)
        } ?: return@runCatching null
        val longest = max(decoded.width, decoded.height)
        if (longest <= maxSide) decoded else {
            val scale = maxSide.toFloat() / longest
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        }
    }.getOrNull()
}
