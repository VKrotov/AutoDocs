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

    /** Копіює зображення з [source] у сховище застосунку. Повертає `file://` URI або null. */
    suspend fun importImage(context: Context, source: Uri, prefix: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(context, source, MAX_SIDE) ?: return@runCatching null
            val file = File(photosDir(context), "${prefix}_${System.currentTimeMillis()}.jpg")
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

    /** Декодує зображення (content:// або file://) зі зменшенням до [maxSide] по довшій стороні. */
    fun decodeScaled(context: Context, uri: Uri, maxSide: Int): Bitmap? = runCatching {
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
    }.getOrNull()
}
