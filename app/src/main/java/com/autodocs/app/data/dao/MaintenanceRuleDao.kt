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

    /** Усі правила авто, включно з вимкненими. */
    @Query("SELECT * FROM maintenance_rules WHERE carId = :carId")
    fun observeForCar(carId: Long): Flow<List<MaintenanceRule>>

    @Query("SELECT * FROM maintenance_rules WHERE carId = :carId")
    suspend fun getForCar(carId: Long): List<MaintenanceRule>

    @Query("SELECT * FROM maintenance_rules WHERE id = :id")
    suspend fun getById(id: Long): MaintenanceRule?

    /** Скільки правил (усіх авто) посилаються на пункт довідника — для попередження при видаленні. */
    @Query("SELECT COUNT(*) FROM maintenance_rules WHERE workTypeId = :workTypeId")
    suspend fun countForWorkType(workTypeId: Long): Int

    @Insert
    suspend fun insert(rule: MaintenanceRule): Long

    @Insert
    suspend fun insertAll(rules: List<MaintenanceRule>)

    @Update
    suspend fun update(rule: MaintenanceRule)

    @Delete
    suspend fun delete(rule: MaintenanceRule)

    @Query("DELETE FROM maintenance_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
