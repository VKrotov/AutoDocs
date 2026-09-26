package com.autodocs.app

import com.autodocs.app.data.plan.DoneMark
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.data.plan.MaintenanceCalculator
import com.autodocs.app.data.plan.MileageForecast
import com.autodocs.app.data.plan.MileagePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Чиста логіка F08/F09 — звичайні JUnit-тести, без Android. */
class MaintenanceCalculatorTest {
    private val today = LocalDate.of(2026, 9, 26)

    // ---------- F09: прогноз ----------

    @Test fun rate_fromTwoPoints() {
        val rate = MileageForecast.kmPerDay(
            listOf(MileagePoint(today.minusDays(100), 350_000), MileagePoint(today, 354_000)), today
        )
        assertEquals(40.0, rate!!, 0.001)
    }

    @Test fun rate_nullWhenTooFewDataOrShortSpan() {
        assertNull(MileageForecast.kmPerDay(listOf(MileagePoint(today, 350_000)), today))
        assertNull(MileageForecast.kmPerDay(listOf(MileagePoint(today.minusDays(5), 350_000), MileagePoint(today, 350_300)), today))
        // пробіг не зростає — прогнозу немає
        assertNull(MileageForecast.kmPerDay(listOf(MileagePoint(today.minusDays(60), 350_000), MileagePoint(today, 350_000)), today))
    }

    @Test fun rate_usesLastYearWindow() {
        val points = listOf(
            MileagePoint(today.minusDays(900), 300_000), // старе — ігнорується
            MileagePoint(today.minusDays(200), 350_000),
            MileagePoint(today, 356_000)
        )
        assertEquals(30.0, MileageForecast.kmPerDay(points, today)!!, 0.001)
    }

    @Test fun estimateCurrent_addsForecast() {
        assertEquals(350_400, MileageForecast.estimateCurrent(350_000, today.minusDays(10), 40.0, today))
        assertEquals(350_000, MileageForecast.estimateCurrent(350_000, today.minusDays(10), null, today))
    }

    // ---------- остання відмітка ----------

    @Test fun latestMark_prefersHigherMileage() {
        val manual = DoneMark(null, 352_000, fromJournal = false)
        val journal = listOf(DoneMark(today.minusDays(300), 348_000, true), DoneMark(today.minusDays(100), 351_000, true))
        assertEquals(manual, MaintenanceCalculator.latestMark(manual, journal))
        val newer = DoneMark(today.minusDays(10), 353_000, true)
        assertEquals(newer, MaintenanceCalculator.latestMark(manual, journal + newer))
    }

    @Test fun latestMark_nullWhenNothingKnown() {
        assertNull(MaintenanceCalculator.latestMark(null, emptyList()))
        assertNull(MaintenanceCalculator.latestMark(DoneMark(null, null, false), emptyList()))
    }

    // ---------- F08: наступне ТО ----------

    @Test fun unknownLastDone_isOverdue() {
        val plan = MaintenanceCalculator.compute(10_000, 12, null, 358_000, 40.0, today)
        assertEquals(DueStatus.OVERDUE, plan.status)
        assertTrue(plan.isUnknown)
    }

    @Test fun ok_whenFarAway() {
        val plan = MaintenanceCalculator.compute(
            10_000, 12, DoneMark(today.minusDays(30), 356_000, true), 357_000, 33.0, today
        )
        assertEquals(DueStatus.OK, plan.status)
        assertEquals(366_000, plan.dueMileage)
        assertEquals(9_000, plan.remainingKm)
        assertEquals(today.minusDays(30).plusMonths(12), plan.dueDate)
    }

    @Test fun soon_byKm() {
        val plan = MaintenanceCalculator.compute(
            10_000, 12, DoneMark(today.minusDays(250), 348_000, true), 357_200, 40.0, today
        )
        assertEquals(800, plan.remainingKm)
        assertEquals(DueStatus.SOON, plan.status)
        assertEquals(20L, plan.remainingDays) // 800 км / 40 км/день
    }

    @Test fun overdue_byDate_evenIfKmOk() {
        // Гальмівна рідина: лише 24 міс
        val plan = MaintenanceCalculator.compute(
            null, 24, DoneMark(today.minusMonths(25), 330_000, false), 358_000, 30.0, today
        )
        assertEquals(DueStatus.OVERDUE, plan.status)
        assertTrue(plan.remainingDays!! < 0)
        assertNull(plan.remainingKm)
    }

    @Test fun overdue_byKm() {
        val plan = MaintenanceCalculator.compute(
            10_000, 12, DoneMark(today.minusDays(100), 346_000, true), 358_000, 40.0, today
        )
        assertEquals(-2_000, plan.remainingKm)
        assertEquals(DueStatus.OVERDUE, plan.status)
    }

    @Test fun onlyMileageKnown_monthsIntervalUsesForecast() {
        // Відомо лише «міняли на 350 000»; за 40 км/день це було 200 днів тому
        val plan = MaintenanceCalculator.compute(null, 24, DoneMark(null, 350_000, false), 358_000, 40.0, today)
        assertEquals(today.minusDays(200).plusMonths(24), plan.dueDate)
        assertEquals(DueStatus.OK, plan.status)
    }

    @Test fun onlyMileageKnown_noForecast_monthsOnly_isOverdue() {
        val plan = MaintenanceCalculator.compute(null, 24, DoneMark(null, 350_000, false), 358_000, null, today)
        assertEquals(DueStatus.OVERDUE, plan.status)
    }

    @Test fun onlyDateKnown_kmIntervalUsesForecast() {
        val plan = MaintenanceCalculator.compute(30_000, null, DoneMark(today.minusDays(100), null, false), 358_000, 50.0, today)
        assertEquals(353_000 + 30_000, plan.dueMileage) // 358 000 − 100×50
        assertEquals(25_000, plan.remainingKm)
    }
}
