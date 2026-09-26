package com.autodocs.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.autodocs.app.data.dao.CarDao
import com.autodocs.app.data.dao.MaintenanceRuleDao
import com.autodocs.app.data.dao.MileageEntryDao
import com.autodocs.app.data.dao.PhotoDao
import com.autodocs.app.data.dao.ServiceRecordDao
import com.autodocs.app.data.dao.ServiceRecordItemDao
import com.autodocs.app.data.dao.WorkTypeDao
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.MaintenanceRule
import com.autodocs.app.data.entity.MileageEntry
import com.autodocs.app.data.entity.Photo
import com.autodocs.app.data.entity.ServiceRecord
import com.autodocs.app.data.entity.ServiceRecordItem
import com.autodocs.app.data.entity.WorkType

/**
 * Схема версії 1. Це "каркас" етапу 1 — таблиці під усі MVP-фічі закладаємо
 * одразу, щоб на наступних етапах не робити болючих Room-міграцій щоразу,
 * коли з'являється новий екран. Реальна робота з даними (репозиторії,
 * seed-довідник робіт, seed-регламент AZM) додається на відповідних етапах.
 */
@Database(
    entities = [
        Car::class,
        WorkType::class,
        ServiceRecord::class,
        ServiceRecordItem::class,
        MaintenanceRule::class,
        MileageEntry::class,
        Photo::class
    ],
    version = 1,
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

    companion object {
        private const val DB_NAME = "autodocs.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
