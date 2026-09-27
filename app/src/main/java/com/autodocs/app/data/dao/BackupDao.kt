package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.CarDocument
import com.autodocs.app.data.entity.MaintenanceRule
import com.autodocs.app.data.entity.MileageEntry
import com.autodocs.app.data.entity.Photo
import com.autodocs.app.data.entity.PlannedTask
import com.autodocs.app.data.entity.ServiceRecord
import com.autodocs.app.data.entity.ServiceRecordItem
import com.autodocs.app.data.entity.Station
import com.autodocs.app.data.entity.TireSet
import com.autodocs.app.data.entity.TireSwap
import com.autodocs.app.data.entity.WorkType

/**
 * Масові операції для бекапу (F12): прочитати все / стерти все / вставити все
 * зі збереженням оригінальних id (щоб зв'язки між таблицями лишилися цілими).
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM cars ORDER BY id") suspend fun allCars(): List<Car>
    @Query("SELECT * FROM work_types ORDER BY id") suspend fun allWorkTypes(): List<WorkType>
    @Query("SELECT * FROM service_records ORDER BY id") suspend fun allRecords(): List<ServiceRecord>
    @Query("SELECT * FROM service_record_items ORDER BY id") suspend fun allRecordItems(): List<ServiceRecordItem>
    @Query("SELECT * FROM maintenance_rules ORDER BY id") suspend fun allRules(): List<MaintenanceRule>
    @Query("SELECT * FROM mileage_entries ORDER BY id") suspend fun allMileage(): List<MileageEntry>
    @Query("SELECT * FROM photos ORDER BY id") suspend fun allPhotos(): List<Photo>
    // Схема v2
    @Query("SELECT * FROM car_documents ORDER BY id") suspend fun allDocuments(): List<CarDocument>
    @Query("SELECT * FROM planned_tasks ORDER BY id") suspend fun allTasks(): List<PlannedTask>
    @Query("SELECT * FROM stations ORDER BY id") suspend fun allStations(): List<Station>
    @Query("SELECT * FROM tire_sets ORDER BY id") suspend fun allTireSets(): List<TireSet>
    @Query("SELECT * FROM tire_swaps ORDER BY id") suspend fun allTireSwaps(): List<TireSwap>

    // Порядок видалення — від дочірніх таблиць до батьківських.
    @Query("DELETE FROM tire_swaps") suspend fun clearTireSwaps()
    @Query("DELETE FROM tire_sets") suspend fun clearTireSets()
    @Query("DELETE FROM planned_tasks") suspend fun clearTasks()
    @Query("DELETE FROM car_documents") suspend fun clearDocuments()
    @Query("DELETE FROM stations") suspend fun clearStations()
    @Query("DELETE FROM service_record_items") suspend fun clearRecordItems()
    @Query("DELETE FROM maintenance_rules") suspend fun clearRules()
    @Query("DELETE FROM mileage_entries") suspend fun clearMileage()
    @Query("DELETE FROM photos") suspend fun clearPhotos()
    @Query("DELETE FROM service_records") suspend fun clearRecords()
    @Query("DELETE FROM work_types") suspend fun clearWorkTypes()
    @Query("DELETE FROM cars") suspend fun clearCars()

    @Insert suspend fun insertCars(items: List<Car>)
    @Insert suspend fun insertWorkTypes(items: List<WorkType>)
    @Insert suspend fun insertRecords(items: List<ServiceRecord>)
    @Insert suspend fun insertRecordItems(items: List<ServiceRecordItem>)
    @Insert suspend fun insertRules(items: List<MaintenanceRule>)
    @Insert suspend fun insertMileage(items: List<MileageEntry>)
    @Insert suspend fun insertPhotos(items: List<Photo>)
    @Insert suspend fun insertDocuments(items: List<CarDocument>)
    @Insert suspend fun insertTasks(items: List<PlannedTask>)
    @Insert suspend fun insertStations(items: List<Station>)
    @Insert suspend fun insertTireSets(items: List<TireSet>)
    @Insert suspend fun insertTireSwaps(items: List<TireSwap>)
}
