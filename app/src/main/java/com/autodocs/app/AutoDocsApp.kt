package com.autodocs.app

import android.app.Application
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.repository.CarRepository
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

    val serviceRepository: ServiceRepository by lazy { ServiceRepository(database) }

    override fun onCreate() {
        super.onCreate()
        seedWorkTypes()
    }

    /** Стартовий довідник робіт — один раз за життя інсталяції. */
    private fun seedWorkTypes() {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        val seeded = prefs.getBoolean(KEY_WORK_TYPES_SEEDED, false)
        if (seeded) return
        appScope.launch {
            runCatching { serviceRepository.seedWorkTypesIfNeeded(alreadySeeded = false) }
                .onSuccess { prefs.edit().putBoolean(KEY_WORK_TYPES_SEEDED, true).apply() }
        }
    }

    private companion object {
        const val KEY_WORK_TYPES_SEEDED = "work_types_seeded_v1"
    }
}
