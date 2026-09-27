package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.autodocs.app.data.entity.PlannedTask
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedTaskDao {
    @Query("SELECT * FROM planned_tasks WHERE carId = :carId ORDER BY id")
    fun observeForCar(carId: Long): Flow<List<PlannedTask>>

    @Query("SELECT * FROM planned_tasks WHERE id = :id")
    suspend fun getById(id: Long): PlannedTask?

    @Insert
    suspend fun insert(task: PlannedTask): Long

    @Update
    suspend fun update(task: PlannedTask)

    @Query("DELETE FROM planned_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
