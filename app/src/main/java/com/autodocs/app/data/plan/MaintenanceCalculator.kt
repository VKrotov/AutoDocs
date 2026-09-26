package com.autodocs.app.data.plan

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Відмітка пробігу на дату (з історії пробігу, записів журналу, картки авто). */
data class MileagePoint(val date: LocalDate, val mileage: Int)

/**
 * F09: прогноз пробігу. Чиста логіка без Android — покрита JUnit-тестами.
 */
object MileageForecast {
    /** Вікно, за яким рахуємо середній пробіг: свіжі дані важливіші за старі. */
    private const val WINDOW_DAYS = 365L
    /** Менше двох тижнів між першою й останньою точкою — прогноз ненадійний. */
    private const val MIN_SPAN_DAYS = 14L

    /**
     * Середній пробіг, км/день, за останній рік (або null, якщо даних замало).
     * Береться найраніша і найпізніша точка вікна: (пробіг₂ − пробіг₁) / дні.
     */
    fun kmPerDay(points: List<MileagePoint>, today: LocalDate): Double? {
        val valid = points.filter { it.mileage > 0 && !it.date.isAfter(today) }
        if (valid.size < 2) return null
        val from = today.minusDays(WINDOW_DAYS)
        val window = valid.filter { !it.date.isBefore(from) }.takeIf { it.size >= 2 } ?: valid
        val first = window.minWith(compareBy<MileagePoint> { it.date }.thenBy { it.mileage })
        val last = window.maxWith(compareBy<MileagePoint> { it.date }.thenBy { it.mileage })
        val days = ChronoUnit.DAYS.between(first.date, last.date)
        if (days < MIN_SPAN_DAYS) return null
        val km = last.mileage - first.mileage
        if (km <= 0) return null
        return km.toDouble() / days
    }

    /** Орієнтовний пробіг на сьогодні: останнє введене значення + середній пробіг × дні. */
    fun estimateCurrent(knownMileage: Int, knownDate: LocalDate, kmPerDay: Double?, today: LocalDate): Int {
        if (kmPerDay == null || knownMileage <= 0) return knownMileage
        val days = ChronoUnit.DAYS.between(knownDate, today)
        if (days <= 0) return knownMileage
        return knownMileage + (kmPerDay * days).roundToInt()
    }
}

enum class DueStatus { OVERDUE, SOON, OK }

/** Коли робота востаннє виконувалась. Хоча б одне з полів задане. */
data class DoneMark(val date: LocalDate?, val mileage: Int?, val fromJournal: Boolean)

/** Результат розрахунку одного правила (F08). */
data class DuePlan(
    /** Остання відмітка (з журналу або введена вручну); null — невідомо, коли робили. */
    val lastDone: DoneMark?,
    val dueMileage: Int?,
    val dueDate: LocalDate?,
    /** Скільки км лишилось (від'ємне — перепробіг). */
    val remainingKm: Int?,
    /** Скільки днів лишилось до найближчого з термінів (за датою або за прогнозом км). */
    val remainingDays: Long?,
    val status: DueStatus
) {
    val isUnknown: Boolean get() = lastDone == null
}

/**
 * F08: розрахунок наступного ТО за правилом «км або місяці — що настане раніше».
 */
object MaintenanceCalculator {

    /**
     * Остання відмітка: пізніша з ручної ([manual]) та записів журналу ([journal]).
     * Порівняння: за пробігом, якщо він є в обох; інакше за датою; інакше перевага журналу.
     */
    fun latestMark(manual: DoneMark?, journal: List<DoneMark>): DoneMark? {
        val fromJournal = journal.maxWithOrNull(
            compareBy<DoneMark> { it.date ?: LocalDate.MIN }.thenBy { it.mileage ?: Int.MIN_VALUE }
        )
        if (manual == null || (manual.date == null && manual.mileage == null)) return fromJournal
        if (fromJournal == null) return manual
        return when {
            manual.mileage != null && fromJournal.mileage != null ->
                if (manual.mileage > fromJournal.mileage) manual else fromJournal
            manual.date != null && fromJournal.date != null ->
                if (manual.date.isAfter(fromJournal.date)) manual else fromJournal
            else -> fromJournal
        }
    }

    fun compute(
        intervalKm: Int?,
        intervalMonths: Int?,
        lastDone: DoneMark?,
        currentMileage: Int,
        kmPerDay: Double?,
        today: LocalDate
    ): DuePlan {
        if (lastDone == null) {
            // Не знаємо, коли робили, — вважаємо простроченим (так домовились у вимогах).
            return DuePlan(null, null, null, null, null, DueStatus.OVERDUE)
        }
        val rate = kmPerDay?.takeIf { it > 0 }

        // Дописуємо відсутню половину відмітки прогнозом, якщо це можливо.
        val lastMileage: Int? = lastDone.mileage ?: run {
            val date = lastDone.date
            if (date != null && rate != null && currentMileage > 0) {
                max(0, currentMileage - (rate * ChronoUnit.DAYS.between(date, today)).roundToInt())
            } else null
        }
        val lastDate: LocalDate? = lastDone.date ?: run {
            val m = lastDone.mileage
            if (m != null && rate != null && currentMileage > 0) {
                today.minusDays(max(0L, ((currentMileage - m) / rate).roundToLong()))
            } else null
        }

        val dueMileage = if (intervalKm != null && lastMileage != null) lastMileage + intervalKm else null
        val dueDate = if (intervalMonths != null && lastDate != null) lastDate.plusMonths(intervalMonths.toLong()) else null

        val remainingKm = if (dueMileage != null && currentMileage > 0) dueMileage - currentMileage else null
        val daysByDate = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
        val daysByKm = if (remainingKm != null && rate != null) ceil(remainingKm / rate).toLong() else null
        val remainingDays = listOfNotNull(daysByDate, daysByKm).minOrNull()

        // Якщо жоден термін порахувати неможливо — як «невідомо».
        if (remainingKm == null && daysByDate == null) {
            return DuePlan(lastDone, dueMileage, dueDate, null, null, DueStatus.OVERDUE)
        }

        val soonKm = intervalKm?.let { max(500, it / 10) }
        val soonDays = intervalMonths?.let { max(14L, (it * 30L) / 10) } ?: 14L
        val overdue = (remainingKm != null && remainingKm < 0) || (daysByDate != null && daysByDate < 0)
        val soon = (remainingKm != null && soonKm != null && remainingKm <= soonKm) ||
            (remainingDays != null && remainingDays <= soonDays)
        val status = when {
            overdue -> DueStatus.OVERDUE
            soon -> DueStatus.SOON
            else -> DueStatus.OK
        }
        return DuePlan(lastDone, dueMileage, dueDate, remainingKm, remainingDays, status)
    }
}
