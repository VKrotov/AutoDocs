package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.autodocs.app.data.entity.ServiceRecordItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRecordItemDao {
    @Query("SELECT * FROM service_record_items WHERE recordId = :recordId")
    fun observeForRecord(recordId: Long): Flow<List<ServiceRecordItem>>

    @Insert
    suspend fun insert(item: ServiceRecordItem): Long

    @Insert
    suspend fun insertAll(items: List<ServiceRecordItem>)

    @Update
    suspend fun update(item: ServiceRecordItem)

    @Delete
    suspend fun delete(item: ServiceRecordItem)

    @Query("DELETE FROM service_record_items WHERE recordId = :recordId")
    suspend fun deleteForRecord(recordId: Long)
}
