package com.autodocs.app

import android.app.Application
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.repository.CarRepository

class AutoDocsApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val carRepository: CarRepository by lazy {
        CarRepository(database.carDao(), database.mileageEntryDao())
    }

    override fun onCreate() {
        super.onCreate()
    }
}
