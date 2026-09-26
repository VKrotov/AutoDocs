package com.autodocs.app.data.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.autodocs.app.AutoDocsApp
import com.autodocs.app.MainActivity
import com.autodocs.app.R
import com.autodocs.app.data.AppPrefs
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * F10: щоденна перевірка плану ТО й показ сповіщень.
 * Викликається з [MaintenanceCheckWorker] і з кнопки «Перевірити зараз» у налаштуваннях.
 */
object MaintenanceNotifier {
    const val CHANNEL_ID = "maintenance"
    const val EXTRA_OPEN = "open"
    const val OPEN_PLAN = "plan"
    const val OPEN_HOME = "home"
    private const val ID_MAINTENANCE = 1001
    private const val ID_MILEAGE = 1002
    private const val ID_INFO = 1003
    private const val KEY_STATE = "notify_state"

    enum class Result { SENT, NOTHING, DISABLED, NO_PERMISSION, NO_CAR }

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Нагадування про ТО", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Наближення терміну ТО, прострочене обслуговування, нагадування внести пробіг"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * [force] — ручна перевірка: ігнорує антиспам і вимкнений перемикач, не змінює стан антиспаму
     * і, якщо нічого не горить, показує інформаційне сповіщення «усе гаразд».
     */
    suspend fun check(context: Context, force: Boolean = false, today: LocalDate = LocalDate.now()): Result {
        val settings = NotifySettings.load(context)
        if (!settings.enabled && !force) return Result.DISABLED
        if (!canPost(context)) return Result.NO_PERMISSION

        val app = context.applicationContext as AutoDocsApp
        val car = app.carRepository.observeActiveCar().first() ?: return Result.NO_CAR
        val overview = app.planRepository.observePlan(car) { today }.first()

        val prefs = AppPrefs.get(context)
        val state = readState(prefs.getString(KEY_STATE, null))
        val decision = ReminderPlanner.decide(
            items = overview.active.map { ReminderItem(it.rule.id, it.name, it.plan) },
            carMileage = car.mileage,
            mileageUpdated = Instant.ofEpochMilli(car.mileageUpdatedAt).atZone(ZoneId.systemDefault()).toLocalDate(),
            settings = settings,
            state = state,
            today = today,
            force = force
        )
        if (!force) prefs.edit().putString(KEY_STATE, writeState(decision.newState)).apply()

        var sent = false
        if (decision.lines.isNotEmpty()) {
            val title = if (decision.lines.size == 1) "Час на ТО: ${car.name}"
            else "ТО: ${decision.lines.size} пункти потребують уваги"
            val style = NotificationCompat.InboxStyle().also { st -> decision.lines.forEach { st.addLine(it) } }
            post(context, ID_MAINTENANCE, title, decision.lines.first(), style, OPEN_PLAN)
            sent = true
        }
        decision.mileageLine?.let {
            post(context, ID_MILEAGE, "Онови пробіг", it, NotificationCompat.BigTextStyle().bigText(it), OPEN_HOME)
            sent = true
        }
        if (!sent && force) {
            val next = overview.active.firstOrNull { !it.plan.isUnknown }
            val text = if (next != null) "Зараз нічого не горить. Найближче: ${next.name}" else "Зараз нічого не горить"
            post(context, ID_INFO, "Сповіщення працюють", text, NotificationCompat.BigTextStyle().bigText(text), OPEN_PLAN)
            return Result.SENT
        }
        return if (sent) Result.SENT else Result.NOTHING
    }

    @Suppress("MissingPermission") // перевірено в canPost()
    private fun post(context: Context, id: Int, title: String, text: String, style: NotificationCompat.Style, open: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_OPEN, open)
        }
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setColor(0xFF5AAEEB.toInt())
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun readState(json: String?): Map<String, String> = runCatching {
        val o = JSONObject(json ?: return emptyMap())
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())

    private fun writeState(map: Map<String, String>): String = JSONObject(map as Map<*, *>).toString()
}
