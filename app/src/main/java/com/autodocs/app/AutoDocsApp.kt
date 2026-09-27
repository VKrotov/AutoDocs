package com.autodocs.app

import android.app.Application
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.AppPrefs
import com.autodocs.app.data.backup.BackupManager
import com.autodocs.app.data.notify.MaintenanceNotifier
import com.autodocs.app.data.notify.ReminderScheduler
import com.autodocs.app.data.repository.CarRepository
import com.autodocs.app.data.repository.DocumentRepository
import com.autodocs.app.data.repository.MileageRepository
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.data.repository.PlanRepository
import com.autodocs.app.data.repository.ServiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoDocsApp : Application() {

    /** Скоуп для фонових разових задач рівня застосунку (сідування довідника тощо). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val carRepository: CarRepository by lazy {
        CarRepository(database.carDao(), database.mileageEntryDao())
    }

    val photoRepository: PhotoRepository by lazy { PhotoRepository(this, database) }

    val serviceRepository: ServiceRepository by lazy { ServiceRepository(database, photoRepository) }

    val backupManager: BackupManager by lazy { BackupManager(this, database) }

    val planRepository: PlanRepository by lazy { PlanRepository(database) }

    val mileageRepository: MileageRepository by lazy { MileageRepository(database) }

    val documentRepository: DocumentRepository by lazy { DocumentRepository(database, photoRepository) }

    override fun onCreate() {
        super.onCreate()
        seedWorkTypes()
        MaintenanceNotifier.createChannel(this)
        // Не даємо збою WorkManager (напр. у тестовому середовищі) покласти застосунок.
        runCatching { ReminderScheduler.schedule(this) }
    }

    /** Стартовий довідник робіт — один раз за життя інсталяції. */
    private fun seedWorkTypes() {
        val prefs = AppPrefs.get(this)
        val seeded = prefs.getBoolean(AppPrefs.KEY_WORK_TYPES_SEEDED, false)
        if (seeded) return
        appScope.launch {
            runCatching { serviceRepository.seedWorkTypesIfNeeded(alreadySeeded = false) }
                .onSuccess { prefs.edit().putBoolean(AppPrefs.KEY_WORK_TYPES_SEEDED, true).apply() }
        }
    }
}
