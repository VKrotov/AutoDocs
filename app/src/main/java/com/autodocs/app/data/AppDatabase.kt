package com.autodocs.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.autodocs.app.data.dao.BackupDao
import com.autodocs.app.data.dao.CarDao
import com.autodocs.app.data.dao.CarDocumentDao
import com.autodocs.app.data.dao.MaintenanceRuleDao
import com.autodocs.app.data.dao.MileageEntryDao
import com.autodocs.app.data.dao.PhotoDao
import com.autodocs.app.data.dao.PlannedTaskDao
import com.autodocs.app.data.dao.ServiceRecordDao
import com.autodocs.app.data.dao.ServiceRecordItemDao
import com.autodocs.app.data.dao.WorkTypeDao
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
 * Схема версії 2.
 * v1 (етап 1) — таблиці під усі MVP-фічі одразу.
 * v2 (етап 9) — усі таблиці V2 однією міграцією: документи з терміном дії, разові плани,
 * а також довідник СТО і шини для етапу 11 (екрани з'являться пізніше).
 * Міграції — у [Migrations]; кожна покрита тестом `MigrationTest`.
 */
@Database(
    entities = [
        Car::class,
        WorkType::class,
        ServiceRecord::class,
        ServiceRecordItem::class,
        MaintenanceRule::class,
        MileageEntry::class,
        Photo::class,
        CarDocument::class,
        PlannedTask::class,
        Station::class,
        TireSet::class,
        TireSwap::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun workTypeDao(): WorkTypeDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun serviceRecordItemDao(): ServiceRecordItemDao
    abstract fun maintenanceRuleDao(): MaintenanceRuleDao
    abstract fun mileageEntryDao(): MileageEntryDao
    abstract fun photoDao(): PhotoDao
    abstract fun carDocumentDao(): CarDocumentDao
    abstract fun plannedTaskDao(): PlannedTaskDao
    abstract fun backupDao(): BackupDao

    companion object {
        /** Версія схеми — пишеться в бекап; тримати в синхроні з @Database(version). */
        const val SCHEMA_VERSION = 2

        private const val DB_NAME = "autodocs.db"

        @Volatile
        private var instance: AppDatabase? = null

        /** Лише для тестів: закрити й забути singleton, щоб наступний тест отримав чисту БД. */
        @androidx.annotation.VisibleForTesting
        fun resetInstanceForTests() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).addMigrations(*Migrations.ALL).build().also { instance = it }
            }
    }
}
