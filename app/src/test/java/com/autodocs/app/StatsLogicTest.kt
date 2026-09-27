package com.autodocs.app

import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.entity.ServiceRecord
import com.autodocs.app.data.entity.ServiceRecordItem
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.plan.MileagePoint
import com.autodocs.app.data.stats.ExpenseStats
import com.autodocs.app.data.stats.JournalFilter
import com.autodocs.app.data.stats.MileageHistory
import com.autodocs.app.data.stats.MileageHistoryPoint
import com.autodocs.app.data.stats.MileageSource
import com.autodocs.app.data.stats.StatsPeriod
import com.autodocs.app.ui.components.compactNumber
import com.autodocs.app.ui.components.niceTicks
import com.autodocs.app.ui.screens.stats.timeTicks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** Етап 8: чиста логіка історії пробігу, статистики витрат, пошуку журналу. */
class StatsLogicTest {
    private val d0 = LocalDate.of(2026, 1, 10)

    private fun p(days: Long, km: Int, src: MileageSource = MileageSource.ENTRY, id: Long = days) =
        MileageHistoryPoint(d0.plusDays(days), km, src, id)

    // ---------- пробіг ----------

    @Test fun suspicious_singleHighTypo_flagsOnlyItself() {
        val marked = MileageHistory.markSuspicious(
            listOf(p(0, 350_000), p(30, 3_510_000), p(60, 352_000), p(90, 353_500))
        )
        assertEquals(listOf(false, true, false, false), marked.map { it.suspicious })
    }

    @Test fun suspicious_lowTypo_flagsOnlyItself() {
        val marked = MileageHistory.markSuspicious(
            listOf(p(0, 350_000), p(30, 351_000), p(60, 35_200), p(90, 353_500))
        )
        assertEquals(listOf(false, false, true, false), marked.map { it.suspicious })
    }

    @Test fun suspicious_latestImpossibleJump_isFlagged() {
        // Остання відмітка з зайвою цифрою: назад не «стрибає», але +3 млн км за день неможливо.
        val marked = MileageHistory.markSuspicious(listOf(p(0, 350_000), p(30, 351_000), p(31, 3_510_000)))
        assertEquals(listOf(false, false, true), marked.map { it.suspicious })
        // Звичайний пробіг за довгий проміжок — не підозрілий.
        assertTrue(MileageHistory.markSuspicious(listOf(p(0, 100_000), p(2000, 350_000))).none { it.suspicious })
    }

    @Test fun suspicious_sameDay_orderedByTime() {
        // Того ж дня спершу ввели помилку, потім виправили: помилкова — перша за часом.
        val day = d0.plusDays(40)
        val typo = MileageHistoryPoint(day, 3_520_000, MileageSource.ENTRY, 1, at = 1_000)
        val fixed = MileageHistoryPoint(day, 352_000, MileageSource.ENTRY, 2, at = 2_000)
        val marked = MileageHistory.markSuspicious(listOf(p(0, 350_000).copy(at = 0), typo, fixed))
        assertEquals(listOf(1L), marked.filter { it.suspicious }.map { it.refId })
    }

    @Test fun suspicious_monotonicHistory_isClean() {
        val marked = MileageHistory.markSuspicious(listOf(p(0, 1), p(1, 1), p(2, 5), p(2, 7)))
        assertTrue(marked.none { it.suspicious })
    }

    @Test fun build_dedupesSameDayAndMileage_preferringEntry() {
        val entry = p(10, 356_000, MileageSource.ENTRY, 7)
        val car = p(10, 356_000, MileageSource.CAR, 1)
        val rec = p(5, 355_000, MileageSource.RECORD, 3)
        val out = MileageHistory.build(listOf(entry), listOf(rec, p(6, 0, MileageSource.RECORD, 4)), car)
        assertEquals(listOf(MileageSource.RECORD, MileageSource.ENTRY), out.map { it.source })
        assertEquals(7L, out.last().refId)
    }

    @Test fun consistent_dropsOutlierForForecast() {
        val pts = listOf(
            MileagePoint(d0, 350_000), MileagePoint(d0.plusDays(10), 3_500_000), MileagePoint(d0.plusDays(100), 354_000)
        )
        assertEquals(listOf(350_000, 354_000), MileageHistory.consistent(pts).map { it.mileage })
    }

    @Test fun drivenBetween_usesRangeAndIgnoresSuspicious() {
        val pts = MileageHistory.markSuspicious(listOf(p(0, 350_000), p(20, 9_999_999), p(40, 351_200), p(400, 362_000)))
        assertEquals(1_200, MileageHistory.drivenBetween(pts, d0, d0.plusDays(60)))
        assertEquals(12_000, MileageHistory.drivenBetween(pts, null, null))
        assertNull(MileageHistory.drivenBetween(pts, d0.plusDays(300), null))
    }

    // ---------- витрати ----------

    private var nextId = 1L
    private fun rec(date: LocalDate, mileage: Int, sto: String?, vararg items: Pair<String, Pair<WorkItemCategory, Double>>, notes: String? = null): RecordWithItems {
        val id = nextId++
        val r = ServiceRecord(id = id, carId = 1, date = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            mileage = mileage, stoName = sto, notes = notes)
        return RecordWithItems(r, items.map { (name, cp) ->
            ServiceRecordItem(id = nextId++, recordId = id, customName = name, category = cp.first, price = cp.second)
        })
    }
    private val R = WorkItemCategory.ROBOTA
    private val Z = WorkItemCategory.ZAPCHASTYNA

    private val records by lazy {
        listOf(
            rec(LocalDate.of(2025, 11, 3), 348_000, "Бош-сервіс", "Заміна масла" to (R to 500.0), "Масло 5W-40" to (Z to 1800.0)),
            rec(LocalDate.of(2026, 3, 14), 352_000, "бош-сервіс ", "Заміна масла" to (R to 600.0), "Масло 5W-40" to (Z to 1900.0)),
            rec(LocalDate.of(2026, 3, 28), 352_400, "Шиномонтаж", "Перевзування" to (R to 800.0)),
            rec(LocalDate.of(2026, 7, 2), 356_000, null, "Колодки передні" to (Z to 1400.0), "заміна МАСЛА" to (R to 0.0), notes = "скрип при гальмуванні")
        )
    }

    @Test fun summarize_year_totalsSplitAndMonthBars() {
        val s = ExpenseStats.summarize(records, StatsPeriod(2026))
        assertEquals(3, s.recordCount)
        assertEquals(1400.0, s.works, 0.001)
        assertEquals(3300.0, s.parts, 0.001)
        assertEquals(4700.0, s.total, 0.001)
        assertEquals(12, s.bars.size)
        assertEquals(3300.0, s.bars[2].amount, 0.001) // березень
        assertEquals(2, s.bars[2].records)
        assertEquals(1400.0, s.bars[6].amount, 0.001) // липень
        assertEquals(0.0, s.bars[0].amount, 0.001)
    }

    @Test fun summarize_all_barsPerYearAndTops() {
        val s = ExpenseStats.summarize(records, StatsPeriod.ALL)
        assertEquals(listOf(2025, 2026), s.bars.map { it.key })
        assertEquals(7000.0, s.total, 0.001)
        // «Масло 5W-40» двічі = 3700 — перше; нульова позиція в топ не йде.
        assertEquals("Масло 5W-40", s.topItems.first().name)
        assertEquals(3700.0, s.topItems.first().amount, 0.001)
        assertEquals(2, s.topItems.first().count)
        assertTrue(s.topItems.none { it.amount == 0.0 })
        // СТО групуються без урахування регістру/пробілів; записи без СТО не рахуються.
        assertEquals("Бош-сервіс", s.topSto.first().name)
        assertEquals(2, s.topSto.first().count)
        assertEquals(4800.0, s.topSto.first().amount, 0.001)
        assertEquals(2, s.topSto.size)
    }

    @Test fun summarize_perKm_fromMileage() {
        val m = MileageHistory.markSuspicious(listOf(
            MileageHistoryPoint(LocalDate.of(2026, 1, 5), 350_000, MileageSource.ENTRY, 1),
            MileageHistoryPoint(LocalDate.of(2026, 9, 1), 357_000, MileageSource.ENTRY, 2)
        ))
        val s = ExpenseStats.summarize(records, StatsPeriod(2026), m)
        assertEquals(7_000, s.drivenKm)
        assertEquals(4700.0 / 7000, s.perKm!!, 1e-9)
        assertNull(ExpenseStats.summarize(records, StatsPeriod(2025), m).perKm)
    }

    @Test fun defaultPeriod_andYears() {
        assertEquals(listOf(2026, 2025), ExpenseStats.years(records))
        assertEquals(StatsPeriod(2026), ExpenseStats.defaultPeriod(records, LocalDate.of(2026, 9, 27)))
        assertEquals(StatsPeriod.ALL, ExpenseStats.defaultPeriod(records, LocalDate.of(2027, 1, 2)))
    }

    // ---------- пошук ----------

    @Test fun filter_matchesAllTermsAcrossFields() {
        assertEquals(3, JournalFilter("масло").apply(records).size)
        assertEquals(1, JournalFilter("масло скрип").apply(records).size)
        assertEquals(2, JournalFilter("БОШ").apply(records).size)
        assertEquals(1, JournalFilter("масло", year = 2025).apply(records).size)
        assertEquals(0, JournalFilter("гбц").apply(records).size)
        assertFalse(JournalFilter("  ").isActive)
        assertEquals(listOf("Колодки передні"), JournalFilter("колодки").matchedItemNames(records[3]))
    }

    // ---------- осі графіків ----------

    @Test fun axisHelpers() {
        assertEquals(listOf(0.0, 2000.0, 4000.0, 6000.0), niceTicks(0.0, 5200.0, 3))
        assertEquals("5 тис", compactNumber(5000.0))
        assertEquals("12,5 тис", compactNumber(12_500.0))
        assertEquals("1,2 млн", compactNumber(1_200_000.0))
        assertEquals("800", compactNumber(800.0))
        val ticks = timeTicks(LocalDate.of(2024, 5, 20), LocalDate.of(2026, 9, 1))
        // 28 міс → крок 6 міс; січень підписаний роком.
        assertEquals(listOf("лип", "2025", "лип", "2026", "лип"), ticks.map { it.second })
        val short = timeTicks(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 1))
        assertEquals(listOf("бер", "тра"), short.map { it.second })
        val long = timeTicks(LocalDate.of(2018, 3, 1), LocalDate.of(2026, 9, 1))
        assertTrue(long.all { it.second.length == 4 }) // крок ≥ 12 міс → лише роки
    }
}
