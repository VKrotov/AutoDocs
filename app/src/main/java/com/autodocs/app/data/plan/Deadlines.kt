package com.autodocs.app.data.plan

import com.autodocs.app.data.entity.CarDocument
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.PlannedTask
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

// Етап 9: дедлайни — документи з терміном дії та разові плани. Чиста логіка без Android.

private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** Документ разом зі станом на сьогодні. */
data class DocumentDue(
    val doc: CarDocument,
    val validUntil: LocalDate,
    /** Скільки днів лишилось до останнього дня дії: 0 — сьогодні останній день, −1 — учора закінчився. */
    val daysLeft: Long,
    val status: DueStatus,
    /**
     * Поточний документ свого виду (найпізніший термін). Старі поліси, яким уже є заміна,
     * — не поточні: вони в історії й не нагадують.
     */
    val isCurrent: Boolean
)

object DocumentDeadlines {
    /** За скільки днів до кінця дії документ стає «скоро» (жовтим) і починає нагадувати. */
    const val DEFAULT_WARN_DAYS = 30

    /** Прострочений документ нагадує ще стільки днів — далі лише червоним в застосунку (напр. непотрібна вже зелена карта). */
    const val OVERDUE_REMIND_DAYS = 30

    /** Документи одного «виду» замінюють один одного. Для «Іншого» вид — це назва. */
    fun groupKey(doc: CarDocument): String =
        if (doc.type == DocumentType.OTHER) "OTHER:" + doc.title.trim().lowercase() else doc.type.name

    fun evaluate(docs: List<CarDocument>, today: LocalDate, warnDays: Int = DEFAULT_WARN_DAYS): List<DocumentDue> {
        val currentIds = docs.groupBy(::groupKey).values
            .map { group -> group.maxWith(compareBy<CarDocument> { it.validUntil }.thenBy { it.id }).id }
            .toSet()
        return docs.map { doc ->
            val until = utcDate(doc.validUntil)
            val days = ChronoUnit.DAYS.between(today, until)
            val status = when {
                days < 0 -> DueStatus.OVERDUE
                days <= warnDays -> DueStatus.SOON
                else -> DueStatus.OK
            }
            DocumentDue(doc, until, days, status, doc.id in currentIds)
        }
    }

    /** Поточні документи: прострочені → скоро → решта; усередині — хто раніше закінчується. */
    fun current(evaluated: List<DocumentDue>): List<DocumentDue> = evaluated
        .filter { it.isCurrent }
        .sortedWith(compareBy<DocumentDue> { it.status.ordinal }.thenBy { it.daysLeft }.thenBy { it.doc.id })

    /** Історія (замінені документи) — новіші першими. */
    fun previous(evaluated: List<DocumentDue>): List<DocumentDue> = evaluated
        .filterNot { it.isCurrent }
        .sortedWith(compareByDescending<DocumentDue> { it.validUntil }.thenByDescending { it.doc.id })
}

/** Разовий план разом із розрахунком терміну. [plan] у тому ж форматі, що й для регламенту. */
data class TaskPlan(val task: PlannedTask, val plan: DuePlan) {
    /** Хоч один термін відомий (можна сказати, скільки лишилось). */
    val hasKnownDue: Boolean get() = plan.remainingKm != null || plan.remainingDays != null
    val hasDeadline: Boolean get() = task.dueDate != null || task.dueMileage != null
}

object OneOffPlanner {
    /** Пороги «скоро» для разових планів (у регламенті вони залежать від інтервалу, тут інтервалу немає). */
    const val SOON_KM = 1_000
    const val SOON_DAYS = 14L

    /** Відмітка-заглушка: у разового плану немає «коли робили», але термін відомий — це не «невідомо». */
    private val NOT_APPLICABLE = DoneMark(date = null, mileage = null, fromJournal = false)

    fun compute(task: PlannedTask, currentMileage: Int, kmPerDay: Double?, today: LocalDate): DuePlan {
        val dueDate = task.dueDate?.let(::utcDate)
        val dueMileage = task.dueMileage
        val rate = kmPerDay?.takeIf { it > 0 }
        val remainingKm = if (dueMileage != null && currentMileage > 0) dueMileage - currentMileage else null
        val daysByDate = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
        val daysByKm = if (remainingKm != null && rate != null) ceil(remainingKm / rate).toLong() else null
        val remainingDays = listOfNotNull(daysByDate, daysByKm).minOrNull()
        val overdue = (remainingKm != null && remainingKm < 0) || (daysByDate != null && daysByDate < 0)
        val soon = (remainingKm != null && remainingKm <= SOON_KM) || (remainingDays != null && remainingDays <= SOON_DAYS)
        val status = when {
            overdue -> DueStatus.OVERDUE
            soon -> DueStatus.SOON
            else -> DueStatus.OK
        }
        return DuePlan(NOT_APPLICABLE, dueMileage, dueDate, remainingKm, remainingDays, status)
    }

    /** Відкриті плани: прострочені → скоро → решта (хто раніше) → без терміну (у порядку додавання). */
    fun sort(plans: List<TaskPlan>): List<TaskPlan> = plans.sortedWith(
        compareBy<TaskPlan> { if (it.hasKnownDue || it.plan.status != DueStatus.OK) it.plan.status.ordinal else 3 }
            .thenBy { it.plan.remainingDays ?: Long.MAX_VALUE }
            .thenBy { it.plan.remainingKm ?: Int.MAX_VALUE }
            .thenBy { it.task.createdAt }
            .thenBy { it.task.id }
    )
}
