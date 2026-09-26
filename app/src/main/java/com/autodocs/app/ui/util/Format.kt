package com.autodocs.app.ui.util

import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.entity.WorkItemCategory
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val UkLocale: Locale = Locale.forLanguageTag("uk-UA")

fun FuelType.label(): String = when (this) {
    FuelType.UNKNOWN -> "Не вказано"
    FuelType.PETROL -> "Бензин"
    FuelType.DIESEL -> "Дизель"
    FuelType.GAS -> "Газ"
    FuelType.HYBRID -> "Гібрид"
    FuelType.ELECTRIC -> "Електро"
}

fun TransmissionType.label(): String = when (this) {
    TransmissionType.UNKNOWN -> "Не вказано"
    TransmissionType.MANUAL -> "Механіка"
    TransmissionType.AUTOMATIC -> "Автомат"
    TransmissionType.ROBOT -> "Робот"
    TransmissionType.VARIATOR -> "Варіатор"
}

fun WorkItemCategory.label(): String = when (this) {
    WorkItemCategory.ROBOTA -> "Робота"
    WorkItemCategory.ZAPCHASTYNA -> "Запчастина"
}

fun WorkItemCategory.pluralLabel(): String = when (this) {
    WorkItemCategory.ROBOTA -> "Роботи"
    WorkItemCategory.ZAPCHASTYNA -> "Запчастини"
}

private val groupingSymbols = DecimalFormatSymbols(UkLocale).apply {
    groupingSeparator = ' '
    decimalSeparator = ','
}

/** 358248 → «358 248» */
fun formatKm(value: Int): String = DecimalFormat("#,##0", groupingSymbols).format(value)

/** 1250 → «1 250»; 1250.5 → «1 250,50» (копійки лише якщо вони є) */
fun formatMoney(value: Double): String {
    val hasKopecks = Math.round(value * 100) % 100 != 0L
    return DecimalFormat(if (hasKopecks) "#,##0.00" else "#,##0", groupingSymbols).format(value)
}

/** Рядок ціни → Double: приймає і кому, і крапку; пробіли ігнорує. */
fun parseMoney(text: String): Double? =
    text.replace(" ", "").replace(" ", "").replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

/** Українські множини: 1 день, 2 дні, 5 днів. */
fun pluralUk(n: Long, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1L && mod100 != 11L -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

fun String.capitalizeFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(UkLocale) else it.toString() }

// ---- Дати записів журналу ----
// Дата запису зберігається як epoch-millis опівночі UTC відповідного календарного дня
// (так само повертає DatePicker з Material3), щоб день не "з'їжджав" через часові пояси.

fun todayUtcMillis(): Long = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun utcMillisToLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", UkLocale)
private val monthYearFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", UkLocale)
private val weekdayDayMonthFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", UkLocale)

/** «12 вересня 2026» */
fun formatRecordDate(utcMillis: Long): String = utcMillisToLocalDate(utcMillis).format(fullDateFormatter)

/** «Вересень 2026» — заголовок групи в журналі */
fun formatMonthYear(date: LocalDate): String = date.format(monthYearFormatter).capitalizeFirst()

/** «Субота, 26 вересня» — шапка головного екрана */
fun formatTodayHeader(): String = LocalDate.now().format(weekdayDayMonthFormatter).capitalizeFirst()

/** «оновлено сьогодні / вчора / 3 дні тому» для звичайного timestamp (локальний час). */
fun formatUpdatedAgo(timestampMillis: Long): String {
    val then = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(then, LocalDate.now())
    return when {
        days <= 0L -> "оновлено сьогодні"
        days == 1L -> "оновлено вчора"
        else -> "оновлено $days ${pluralUk(days, "день", "дні", "днів")} тому"
    }
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", UkLocale)

/** «26 вересня 2026, 14:20» для звичайного timestamp (локальний час). */
fun formatDateTime(timestampMillis: Long): String =
    Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

/** «2026-09-26» — для імен файлів. */
fun isoToday(): String = LocalDate.now().toString()

// ---- План ТО ----

/** «Кожні 10 000 км або 12 міс» / «Раз на 24 міс» / «Кожні 30 000 км». */
fun formatInterval(km: Int?, months: Int?): String = when {
    km != null && months != null -> "Кожні ${formatKm(km)} км або $months міс"
    km != null -> "Кожні ${formatKm(km)} км"
    months != null -> "Раз на $months міс"
    else -> "Інтервал не задано"
}

/** 25 → «25 днів», −12 → «−12 днів». */
fun formatDaysSigned(days: Long): String {
    val abs = kotlin.math.abs(days)
    val sign = if (days < 0) "−" else ""
    return "$sign$abs ${pluralUk(abs, "день", "дні", "днів")}"
}

/** 1180 → «1 180 км», −2000 → «−2 000 км». */
fun formatKmSigned(km: Int): String = (if (km < 0) "−" else "") + formatKm(kotlin.math.abs(km)) + " км"

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", UkLocale)

/** «12 жовт. 2027» */
fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)

/** «12 вересня 2026» для LocalDate. */
fun formatLongDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", UkLocale))
