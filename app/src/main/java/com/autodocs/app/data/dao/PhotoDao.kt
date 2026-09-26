package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.autodocs.app.data.entity.Photo
import com.autodocs.app.data.entity.PhotoOwnerType
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE ownerType = :ownerType AND ownerId = :ownerId")
    fun observeFor(ownerType: PhotoOwnerType, ownerId: Long): Flow<List<Photo>>

    @Insert
    suspend fun insert(photo: Photo): Long

    @Delete
    suspend fun delete(photo: Photo)
}
