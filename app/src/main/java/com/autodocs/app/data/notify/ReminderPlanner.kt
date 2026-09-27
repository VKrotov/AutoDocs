package com.autodocs.app.data.notify

import com.autodocs.app.data.entity.displayName
import com.autodocs.app.data.plan.DocumentDeadlines
import com.autodocs.app.data.plan.DocumentDue
import com.autodocs.app.data.plan.DuePlan
import com.autodocs.app.data.plan.DueStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Пункт для перевірки нагадувань: правило регламенту або (етап 9) разовий план.
 * [key] — ключ у стані антиспаму: «r<id>» для регламенту, «t<id>» для разового плану.
 */
data class ReminderItem(val ruleId: Long, val name: String, val plan: DuePlan, val key: String = "r$ruleId")

/** Що надіслати і який стан антиспаму запам'ятати. */
data class ReminderDecision(
    /** Рядки про ТО («Гальмівна рідина — прострочено на 12 днів»); порожньо — нічого не слати. */
    val lines: List<String>,
    /** Нагадування внести пробіг або null. */
    val mileageLine: String?,
    /** Новий стан: ключ → "ВИД|epochDay" останнього сповіщення. */
    val newState: Map<String, String>,
    /** Етап 9: рядки про документи («Автоцивілка — закінчується через 12 днів»). */
    val documentLines: List<String> = emptyList()
)

/**
 * F10: вирішує, про що нагадати сьогодні. Чиста логіка без Android.
 *
 * Антиспам: про той самий пункт в тому самому стані («скоро» / «прострочено»)
 * нагадуємо не частіше ніж раз на [REPEAT_DAYS] днів; зміна стану (скоро → прострочено)
 * нагадує одразу. Пункти без відмітки («невідомо, коли робили») не нагадуємо —
 * це питання налаштування, воно видно на екрані «План ТО».
 */
object ReminderPlanner {
    const val REPEAT_DAYS = 7L
    private const val MILEAGE_KEY = "mileage"

    fun decide(
        items: List<ReminderItem>,
        carMileage: Int,
        mileageUpdated: LocalDate?,
        settings: NotifySettings,
        state: Map<String, String>,
        today: LocalDate,
        force: Boolean = false,
        documents: List<DocumentDue> = emptyList()
    ): ReminderDecision {
        val newState = state.toMutableMap()
        val lines = mutableListOf<String>()

        for (item in items) {
            val key = item.key
            val kind = dueKind(item.plan, settings)
            if (kind == null) {
                newState.remove(key)
                continue
            }
            if (force || shouldRepeat(state[key], kind, today)) {
                lines += lineFor(item, kind)
                newState[key] = "$kind|${today.toEpochDay()}"
            }
        }
        // Прострочені — першими.
        lines.sortBy { if (it.contains("прострочено")) 0 else 1 }

        var mileageLine: String? = null
        val staleDays = mileageUpdated?.let { ChronoUnit.DAYS.between(it, today) }
        val mileageDue = settings.mileageDays > 0 &&
            (carMileage <= 0 || (staleDays != null && staleDays >= settings.mileageDays))
        if (mileageDue) {
            if (force || shouldRepeat(state[MILEAGE_KEY], "MILEAGE", today)) {
                mileageLine = if (carMileage <= 0) {
                    "Вкажи пробіг авто — без нього план ТО не рахується"
                } else {
                    "Пробіг не оновлювали $staleDays ${plural(staleDays ?: 0, "день", "дні", "днів")} — внеси актуальний, щоб прогноз ТО був точним"
                }
                newState[MILEAGE_KEY] = "MILEAGE|${today.toEpochDay()}"
            }
        } else {
            newState.remove(MILEAGE_KEY)
        }
        // Документи: лише поточні (замінені новим полісом не нагадують), прострочені — ще місяць.
        val documentLines = mutableListOf<String>()
        for (d in documents.sortedBy { it.daysLeft }) {
            val key = "d${d.doc.id}"
            val kind = if (d.isCurrent) documentKind(d, settings) else null
            if (kind == null) {
                newState.remove(key)
                continue
            }
            if (force || shouldRepeat(state[key], kind, today)) {
                documentLines += documentLine(d)
                newState[key] = "$kind|${today.toEpochDay()}"
            }
        }
        return ReminderDecision(lines, mileageLine, newState, documentLines)
    }

    /** "OVERDUE" / "SOON" / null для документа. */
    fun documentKind(d: DocumentDue, s: NotifySettings): String? = when {
        d.daysLeft < -DocumentDeadlines.OVERDUE_REMIND_DAYS -> null
        d.daysLeft < 0 -> "OVERDUE"
        d.daysLeft <= s.docDaysBefore -> "SOON"
        else -> null
    }

    fun documentLine(d: DocumentDue): String {
        val name = d.doc.displayName()
        val days = d.daysLeft
        return when {
            days < 0 -> {
                val ago = -days
                "$name — термін дії закінчився" + if (ago == 1L) " учора" else " $ago ${plural(ago, "день", "дні", "днів")} тому"
            }
            days == 0L -> "$name — сьогодні останній день дії"
            days == 1L -> "$name — діє до завтра включно"
            else -> "$name — закінчується через $days ${plural(days, "день", "дні", "днів")}"
        }
    }

    /** "OVERDUE" / "SOON" / null — чи варто зараз нагадувати про пункт. */
    fun dueKind(plan: DuePlan, s: NotifySettings): String? {
        if (plan.isUnknown) return null
        if (plan.status == DueStatus.OVERDUE) return "OVERDUE"
        val byDays = plan.remainingDays != null && plan.remainingDays <= s.daysBefore
        val byKm = plan.remainingKm != null && plan.remainingKm <= s.kmBefore
        return if (byDays || byKm) "SOON" else null
    }

    private fun shouldRepeat(prev: String?, kind: String, today: LocalDate): Boolean {
        if (prev == null) return true
        val parts = prev.split('|')
        val prevKind = parts.getOrNull(0)
        val prevDay = parts.getOrNull(1)?.toLongOrNull() ?: return true
        if (prevKind != kind) return true
        return today.toEpochDay() - prevDay >= REPEAT_DAYS
    }

    fun lineFor(item: ReminderItem, kind: String): String {
        val p = item.plan
        val km = p.remainingKm
        val days = p.remainingDays
        return if (kind == "OVERDUE") {
            val detail = when {
                km != null && km < 0 -> " на ${fmtKm(-km)} км"
                days != null && days < 0 -> " на ${abs(days)} ${plural(abs(days), "день", "дні", "днів")}"
                else -> ""
            }
            "${item.name} — прострочено$detail"
        } else {
            val parts = listOfNotNull(
                km?.let { "${fmtKm(it)} км" },
                days?.let { "≈ $it ${plural(it, "день", "дні", "днів")}" }
            )
            "${item.name} — через ${parts.joinToString(" / ")}"
        }
    }

    private fun fmtKm(v: Int): String = v.toString().reversed().chunked(3).joinToString(" ").reversed()

    private fun plural(n: Long, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1L && m100 != 11L -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }
}
