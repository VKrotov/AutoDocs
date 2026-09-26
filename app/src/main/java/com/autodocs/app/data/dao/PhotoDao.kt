package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.autodocs.app.data.entity.Photo
import com.autodocs.app.data.entity.PhotoOwnerType
import kotlinx.coroutines.flow.Flow

/** Кількість фото на власника — для позначки «📎 N» у журналі. */
data class PhotoCount(val ownerId: Long, val cnt: Int)

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt, id")
    fun observeFor(ownerType: PhotoOwnerType, ownerId: Long): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt, id")
    suspend fun getFor(ownerType: PhotoOwnerType, ownerId: Long): List<Photo>

    @Query("SELECT ownerId, COUNT(*) AS cnt FROM photos WHERE ownerType = :ownerType GROUP BY ownerId")
    fun observeCounts(ownerType: PhotoOwnerType): Flow<List<PhotoCount>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: Long): Photo?

    @Insert
    suspend fun insert(photo: Photo): Long

    @Delete
    suspend fun delete(photo: Photo)
}
