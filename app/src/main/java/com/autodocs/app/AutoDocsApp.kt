package com.autodocs.app

import android.app.Application
import com.autodocs.app.data.AppDatabase

class AutoDocsApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
