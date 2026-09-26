package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.autodocs.app.data.entity.ServiceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records WHERE carId = :carId ORDER BY date DESC")
    fun observeForCar(carId: Long): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun getById(id: Long): ServiceRecord?

    @Insert
    suspend fun insert(record: ServiceRecord): Long

    @Update
    suspend fun update(record: ServiceRecord)

    @Delete
    suspend fun delete(record: ServiceRecord)
}
