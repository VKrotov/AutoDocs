package com.autodocs.app.data.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Щоденна фонова перевірка плану ТО (WorkManager переживає перезавантаження телефона). */
class MaintenanceCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { MaintenanceNotifier.check(applicationContext) }
        return Result.success()
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "maintenance_daily_check"

    /**
     * Запланувати щоденну перевірку на годину з налаштувань.
     * [reschedule] = true — перепланувати (змінили годину/увімкнули); false — залишити, якщо вже є.
     */
    fun schedule(context: Context, reschedule: Boolean = false) {
        val settings = NotifySettings.load(context)
        val wm = WorkManager.getInstance(context)
        if (!settings.enabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<MaintenanceCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(settings.hour).toMinutes(), TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (reschedule) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Скільки лишилось до найближчої [hour]:00 (сьогодні або завтра). */
    fun delayUntil(hour: Int, now: LocalDateTime = LocalDateTime.now()): Duration {
        var target = now.toLocalDate().atTime(hour, 0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }
}
