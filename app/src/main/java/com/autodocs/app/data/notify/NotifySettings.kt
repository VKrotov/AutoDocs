package com.autodocs.app.data.notify

import android.content.Context
import com.autodocs.app.data.AppPrefs

/** Налаштування нагадувань (F10). Зберігаються в SharedPreferences `app_state`. */
data class NotifySettings(
    val enabled: Boolean = true,
    /** Попереджати за стільки днів до терміну. */
    val daysBefore: Int = 14,
    /** …або за стільки км до терміну. */
    val kmBefore: Int = 1_000,
    /** Нагадати внести пробіг, якщо він не оновлювався стільки днів (0 — не нагадувати). */
    val mileageDays: Int = 14,
    /** Година дня для щоденної перевірки. */
    val hour: Int = 10,
    /** Документи (страховка, техогляд): попереджати за стільки днів до кінця дії. */
    val docDaysBefore: Int = 30
) {
    companion object {
        private const val K_ENABLED = "notify_enabled"
        private const val K_DAYS = "notify_days_before"
        private const val K_KM = "notify_km_before"
        private const val K_MILEAGE = "notify_mileage_days"
        private const val K_HOUR = "notify_hour"
        private const val K_DOC_DAYS = "notify_doc_days_before"

        fun load(context: Context): NotifySettings {
            val p = AppPrefs.get(context)
            val d = NotifySettings()
            return NotifySettings(
                enabled = p.getBoolean(K_ENABLED, d.enabled),
                daysBefore = p.getInt(K_DAYS, d.daysBefore),
                kmBefore = p.getInt(K_KM, d.kmBefore),
                mileageDays = p.getInt(K_MILEAGE, d.mileageDays),
                hour = p.getInt(K_HOUR, d.hour),
                docDaysBefore = p.getInt(K_DOC_DAYS, d.docDaysBefore)
            )
        }

        fun save(context: Context, s: NotifySettings) {
            AppPrefs.get(context).edit()
                .putBoolean(K_ENABLED, s.enabled)
                .putInt(K_DAYS, s.daysBefore)
                .putInt(K_KM, s.kmBefore)
                .putInt(K_MILEAGE, s.mileageDays)
                .putInt(K_HOUR, s.hour)
                .putInt(K_DOC_DAYS, s.docDaysBefore)
                .apply()
        }
    }
}
