package com.autodocs.app.data.repository

import android.content.Context
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.entity.Photo
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Фото, яке має бути прив'язане до власника (після збереження форми). */
data class PhotoRef(val uri: String, val kind: PhotoKind)

/**
 * F05/F25/F26: фото до записів журналу, скани документів, фото техпаспорта.
 * Файли лежать у files/photos (див. [PhotoStorage]); таблиця `photos` — лише посилання.
 * Зовнішнього ключа на власника немає (власники — різні таблиці), тож видаляємо тут вручну.
 */
class PhotoRepository(private val context: Context, db: AppDatabase) {
    private val dao = db.photoDao()

    companion object {
        const val MAX_PER_RECORD = 5
    }

    fun observeFor(ownerType: PhotoOwnerType, ownerId: Long): Flow<List<Photo>> = dao.observeFor(ownerType, ownerId)

    suspend fun getFor(ownerType: PhotoOwnerType, ownerId: Long): List<Photo> = dao.getFor(ownerType, ownerId)

    /** Кількість фото по кожному запису журналу. */
    fun observeRecordPhotoCounts(): Flow<Map<Long, Int>> =
        dao.observeCounts(PhotoOwnerType.SERVICE_RECORD).map { list -> list.associate { it.ownerId to it.cnt } }

    /**
     * Привести фото власника до списку [desired] (порядок = порядок показу):
     * зайві рядки й файли видаляються, нові — додаються, наявні лишаються як є.
     */
    suspend fun replaceFor(ownerType: PhotoOwnerType, ownerId: Long, desired: List<PhotoRef>) {
        val existing = dao.getFor(ownerType, ownerId)
        val wanted = desired.map { it.uri }.toSet()
        existing.filter { it.uri !in wanted }.forEach { delete(it) }
        val have = existing.map { it.uri }.toSet()
        var t = System.currentTimeMillis()
        desired.filter { it.uri !in have }.forEach { ref ->
            dao.insert(Photo(ownerType = ownerType, ownerId = ownerId, kind = ref.kind, uri = ref.uri, createdAt = t++))
        }
    }

    /** Замінити фото певного виду (напр. лицьова сторона техпаспорта). null — просто видалити. */
    suspend fun setSingle(ownerType: PhotoOwnerType, ownerId: Long, kind: PhotoKind, uri: String?) {
        dao.getFor(ownerType, ownerId).filter { it.kind == kind && it.uri != uri }.forEach { delete(it) }
        if (uri != null && dao.getFor(ownerType, ownerId).none { it.uri == uri }) {
            dao.insert(Photo(ownerType = ownerType, ownerId = ownerId, kind = kind, uri = uri))
        }
    }

    suspend fun deleteAllFor(ownerType: PhotoOwnerType, ownerId: Long) {
        dao.getFor(ownerType, ownerId).forEach { delete(it) }
    }

    private suspend fun delete(photo: Photo) {
        dao.delete(photo)
        PhotoStorage.deleteIfOwned(context, photo.uri)
    }

    /** Видалити файл, який так і не став рядком у БД (скасована форма). */
    fun discardFile(uri: String) = PhotoStorage.deleteIfOwned(context, uri)
}
