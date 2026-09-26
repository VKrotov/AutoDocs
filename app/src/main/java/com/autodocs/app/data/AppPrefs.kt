package com.autodocs.app.data

import android.content.Context
import android.content.SharedPreferences

/** Дрібний службовий стан застосунку (не дані користувача — вони в Room). */
object AppPrefs {
    const val KEY_WORK_TYPES_SEEDED = "work_types_seeded_v1"
    const val KEY_LAST_BACKUP_AT = "last_backup_at"
    const val KEY_ASKED_NOTIFICATIONS = "asked_notifications_permission"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
}
