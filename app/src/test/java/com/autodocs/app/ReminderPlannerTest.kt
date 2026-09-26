package com.autodocs.app

import com.autodocs.app.data.notify.NotifySettings
import com.autodocs.app.data.notify.ReminderItem
import com.autodocs.app.data.notify.ReminderPlanner
import com.autodocs.app.data.plan.DoneMark
import com.autodocs.app.data.plan.MaintenanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderPlannerTest {
    private val today = LocalDate.of(2026, 9, 26)
    private val s = NotifySettings()

    private fun item(id: Long, name: String, km: Int?, mo: Int?, last: DoneMark?, current: Int = 358_000) =
        ReminderItem(id, name, MaintenanceCalculator.compute(km, mo, last, current, 40.0, today))

    private val oilSoon = item(1, "Заміна масла двигуна", 10_000, 12, DoneMark(today.minusDays(250), 348_600, true)) // 600 км
    private val brakeOverdue = item(2, "Заміна гальмівної рідини", null, 24, DoneMark(today.minusMonths(25), 330_000, false))
    private val plugsOk = item(3, "Заміна свічок", 30_000, 24, DoneMark(today.minusDays(100), 354_000, false))
    private val unknown = item(4, "Контроль рівня масла", 1_000, 1, null)

    @Test fun firstRun_notifiesDueOnly_overdueFirst() {
        val d = ReminderPlanner.decide(listOf(oilSoon, brakeOverdue, plugsOk, unknown), 358_000, today, s, emptyMap(), today)
        assertEquals(2, d.lines.size)
        assertTrue(d.lines[0].startsWith("Заміна гальмівної рідини — прострочено на "))
        assertTrue(d.lines[1], d.lines[1].startsWith("Заміна масла двигуна — через 600 км"))
        assertEquals(setOf("r1", "r2"), d.newState.keys)
        assertNull(d.mileageLine)
    }

    @Test fun antiSpam_sameStateWithinWeek_silent_thenRepeats() {
        val first = ReminderPlanner.decide(listOf(oilSoon, brakeOverdue), 358_000, today, s, emptyMap(), today)
        val next = ReminderPlanner.decide(listOf(oilSoon, brakeOverdue), 358_000, today, s, first.newState, today.plusDays(3))
        assertTrue(next.lines.isEmpty())
        val week = ReminderPlanner.decide(listOf(oilSoon, brakeOverdue), 358_000, today, s, first.newState, today.plusDays(7))
        assertEquals(2, week.lines.size)
    }

    @Test fun stateChange_soonToOverdue_notifiesImmediately() {
        val first = ReminderPlanner.decide(listOf(oilSoon), 358_000, today, s, emptyMap(), today)
        val overdueOil = item(1, "Заміна масла двигуна", 10_000, 12, DoneMark(today.minusDays(250), 348_600, true), current = 359_000)
        val d = ReminderPlanner.decide(listOf(overdueOil), 359_000, today, s, first.newState, today.plusDays(1))
        assertEquals(1, d.lines.size)
        assertTrue(d.lines[0].contains("прострочено на 400 км"))
    }

    @Test fun doneAgain_clearsState() {
        val first = ReminderPlanner.decide(listOf(oilSoon), 358_000, today, s, emptyMap(), today)
        val fresh = item(1, "Заміна масла двигуна", 10_000, 12, DoneMark(today, 358_000, true))
        val d = ReminderPlanner.decide(listOf(fresh), 358_000, today, s, first.newState, today.plusDays(1))
        assertTrue(d.lines.isEmpty())
        assertTrue("r1" !in d.newState)
    }

    @Test fun mileageReminder_whenStale_andRepeatsWeekly() {
        val d = ReminderPlanner.decide(emptyList(), 358_000, today.minusDays(20), s, emptyMap(), today)
        assertTrue(d.mileageLine!!.contains("20 днів"))
        val again = ReminderPlanner.decide(emptyList(), 358_000, today.minusDays(20), s, d.newState, today.plusDays(2))
        assertNull(again.mileageLine)
        val off = ReminderPlanner.decide(emptyList(), 358_000, today.minusDays(20), s.copy(mileageDays = 0), emptyMap(), today)
        assertNull(off.mileageLine)
    }

    @Test fun force_ignoresAntiSpam() {
        val first = ReminderPlanner.decide(listOf(oilSoon), 358_000, today, s, emptyMap(), today)
        val forced = ReminderPlanner.decide(listOf(oilSoon), 358_000, today, s, first.newState, today, force = true)
        assertEquals(1, forced.lines.size)
    }
}
