package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.entity.ServiceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records WHERE carId = :carId ORDER BY date DESC")
    fun observeForCar(carId: Long): Flow<List<ServiceRecord>>

    @Transaction
    @Query("SELECT * FROM service_records WHERE carId = :carId ORDER BY date DESC, mileage DESC, id DESC")
    fun observeWithItemsForCar(carId: Long): Flow<List<RecordWithItems>>

    @Transaction
    @Query("SELECT * FROM service_records WHERE id = :id")
    fun observeWithItems(id: Long): Flow<RecordWithItems?>

    @Transaction
    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun getWithItems(id: Long): RecordWithItems?

    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun getById(id: Long): ServiceRecord?

    /** Назви СТО, які вже траплялися, — для підказок у формі (найчастіші першими). */
    @Query(
        "SELECT stoName FROM service_records WHERE stoName IS NOT NULL AND stoName != '' " +
            "GROUP BY stoName ORDER BY COUNT(*) DESC, MAX(date) DESC"
    )
    fun observeStoNames(): Flow<List<String>>

    @Insert
    suspend fun insert(record: ServiceRecord): Long

    @Update
    suspend fun update(record: ServiceRecord)

    @Delete
    suspend fun delete(record: ServiceRecord)

    @Query("DELETE FROM service_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
