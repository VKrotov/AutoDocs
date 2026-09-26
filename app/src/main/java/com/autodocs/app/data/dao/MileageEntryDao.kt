package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autodocs.app.data.entity.MileageEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageEntryDao {
    @Query("SELECT * FROM mileage_entries WHERE carId = :carId ORDER BY date DESC")
    fun observeForCar(carId: Long): Flow<List<MileageEntry>>

    @Query("SELECT * FROM mileage_entries WHERE carId = :carId ORDER BY date DESC LIMIT :limit")
    suspend fun recentForCar(carId: Long, limit: Int = 10): List<MileageEntry>

    @Insert
    suspend fun insert(entry: MileageEntry): Long
}
