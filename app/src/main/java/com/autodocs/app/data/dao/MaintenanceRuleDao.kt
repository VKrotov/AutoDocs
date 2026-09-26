package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.autodocs.app.data.entity.MaintenanceRule
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceRuleDao {
    @Query("SELECT * FROM maintenance_rules WHERE carId = :carId AND isActive = 1")
    fun observeActiveForCar(carId: Long): Flow<List<MaintenanceRule>>

    @Insert
    suspend fun insert(rule: MaintenanceRule): Long

    @Insert
    suspend fun insertAll(rules: List<MaintenanceRule>)

    @Update
    suspend fun update(rule: MaintenanceRule)

    @Delete
    suspend fun delete(rule: MaintenanceRule)
}
