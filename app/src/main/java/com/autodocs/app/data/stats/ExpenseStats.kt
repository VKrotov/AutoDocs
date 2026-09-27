package com.autodocs.app.data.stats

import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.displayName
import com.autodocs.app.data.entity.total
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Період статистики: конкретний рік або вся історія (year == null). */
data class StatsPeriod(val year: Int?) {
    val isAll: Boolean get() = year == null

    companion object {
        val ALL = StatsPeriod(null)
    }
}

/** Один стовпчик графіка витрат: місяць року або рік (для «Усі роки»). */
data class ExpenseBar(
    /** Для року — 1..12 (місяць); для «Усі роки» — рік. */
    val key: Int,
    val amount: Double,
    val records: Int
)

/** Рядок топу: позиція (робота/запчастина) або СТО. */
data class TopEntry(val name: String, val amount: Double, val count: Int, val category: WorkItemCategory? = null)

data class ExpenseSummary(
    val period: StatsPeriod,
    val total: Double,
    val works: Double,
    val parts: Double,
    val recordCount: Int,
    val bars: List<ExpenseBar>,
    /** Грн на 1 км за період (null — невідомо, скільки проїхано). */
    val perKm: Double?,
    val drivenKm: Int?,
    val topItems: List<TopEntry>,
    val topSto: List<TopEntry>
) {
    val isEmpty: Boolean get() = recordCount == 0
    val averagePerRecord: Double get() = if (recordCount > 0) total / recordCount else 0.0
}

/** Статистика витрат (етап 8). Чиста логіка — покрита JUnit-тестами. */
object ExpenseStats {
    const val TOP_LIMIT = 5
    /** Менше за цей пробіг «грн/км» вийде випадковим — не показуємо. */
    private const val MIN_KM_FOR_RATE = 300

    fun recordDate(r: RecordWithItems): LocalDate =
        Instant.ofEpochMilli(r.record.date).atZone(ZoneOffset.UTC).toLocalDate()

    /** Роки, за які є записи (від нових до старих). */
    fun years(records: List<RecordWithItems>): List<Int> =
        records.map { recordDate(it).year }.distinct().sortedDescending()

    /** Період за замовчуванням: поточний рік, якщо в ньому є записи, інакше — усі роки. */
    fun defaultPeriod(records: List<RecordWithItems>, today: LocalDate): StatsPeriod =
        if (records.any { recordDate(it).year == today.year }) StatsPeriod(today.year) else StatsPeriod.ALL

    fun summarize(
        records: List<RecordWithItems>,
        period: StatsPeriod,
        mileage: List<MileageHistoryPoint> = emptyList()
    ): ExpenseSummary {
        val inPeriod = records.filter { period.year == null || recordDate(it).year == period.year }
        val items = inPeriod.flatMap { it.items }
        val works = items.filter { it.category == WorkItemCategory.ROBOTA }.sumOf { it.price }
        val parts = items.filter { it.category == WorkItemCategory.ZAPCHASTYNA }.sumOf { it.price }
        val total = inPeriod.sumOf { it.total() }

        val bars = if (period.year != null) {
            val byMonth = inPeriod.groupBy { recordDate(it).monthValue }
            (1..12).map { m -> byMonth[m].orEmpty().let { ExpenseBar(m, it.sumOf { r -> r.total() }, it.size) } }
        } else if (inPeriod.isEmpty()) {
            emptyList()
        } else {
            val byYear = inPeriod.groupBy { recordDate(it).year }
            (byYear.keys.min()..byYear.keys.max()).map { y ->
                byYear[y].orEmpty().let { ExpenseBar(y, it.sumOf { r -> r.total() }, it.size) }
            }
        }

        val driven = if (period.year != null) {
            MileageHistory.drivenBetween(mileage, LocalDate.of(period.year, 1, 1), LocalDate.of(period.year, 12, 31))
        } else {
            MileageHistory.drivenBetween(mileage, null, null)
        }
        val perKm = if (driven != null && driven >= MIN_KM_FOR_RATE && total > 0) total / driven else null

        val topItems = items
            .filter { it.price > 0 }
            .groupBy { it.displayName().trim().lowercase() to it.category }
            .map { (_, list) -> TopEntry(list.first().displayName().trim(), list.sumOf { it.price }, list.size, list.first().category) }
            .sortedWith(compareByDescending<TopEntry> { it.amount }.thenBy { it.name })
            .take(TOP_LIMIT)

        val topSto = inPeriod
            .filter { !it.record.stoName.isNullOrBlank() }
            .groupBy { it.record.stoName!!.trim().lowercase() }
            .map { (_, list) -> TopEntry(list.first().record.stoName!!.trim(), list.sumOf { it.total() }, list.size) }
            .sortedWith(compareByDescending<TopEntry> { it.amount }.thenByDescending { it.count }.thenBy { it.name })
            .take(TOP_LIMIT)

        return ExpenseSummary(
            period = period,
            total = total,
            works = works,
            parts = parts,
            recordCount = inPeriod.size,
            bars = bars,
            perKm = perKm,
            drivenKm = driven,
            topItems = topItems,
            topSto = topSto
        )
    }
}
