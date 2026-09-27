package com.autodocs.app

import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.MaintenanceRule
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.plan.DoneMark
import com.autodocs.app.data.plan.DuePlan
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.data.repository.PlanOverview
import com.autodocs.app.data.repository.RulePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** «Найближче ТО» на головній: фільтр, порядок, ліміт 5. */
class HomeNearestTest {
    private val car = Car(
        id = 1, name = "Passat", make = "VW", model = "Passat B5+", engine = "",
        fuelType = FuelType.UNKNOWN, transmissionType = TransmissionType.UNKNOWN, licensePlate = "", vin = ""
    )
    private val mark = DoneMark(LocalDate.of(2026, 1, 1), 340_000, fromJournal = true)
    private var nextId = 1L

    private fun item(name: String, status: DueStatus, km: Int?, days: Long?, last: DoneMark? = mark, active: Boolean = true) =
        RulePlan(
            rule = MaintenanceRule(id = nextId++, carId = 1, workTypeId = nextId, isActive = active),
            name = name,
            hint = null,
            journalMark = null,
            plan = DuePlan(last, null, null, km, days, status)
        )

    private fun overview(items: List<RulePlan>) =
        PlanOverview(car, 350_000, 30.0, items.filter { it.rule.isActive }, items.filterNot { it.rule.isActive })

    @Test fun unknownAndUncomputable_areHidden() {
        val plan = overview(
            listOf(
                item("Невідомо", DueStatus.OVERDUE, null, null, last = null), // «Вказати»
                item("Без терміну", DueStatus.OVERDUE, null, null),            // «?»
                item("Масло", DueStatus.OK, 5_000, 160)
            )
        )
        assertEquals(listOf("Масло"), plan.nearest().map { it.name })
        assertEquals(1, plan.unknownCount)
    }

    @Test fun order_overdueThenSoonThenRest() {
        val plan = overview(
            listOf(
                item("OK далеко", DueStatus.OK, 20_000, 600),
                item("Скоро", DueStatus.SOON, 400, 12),
                item("OK ближче", DueStatus.OK, 3_000, 100),
                item("Прострочено трохи", DueStatus.OVERDUE, -100, -3),
                item("Прострочено сильно", DueStatus.OVERDUE, -5_000, -150)
            )
        )
        assertEquals(
            listOf("Прострочено сильно", "Прострочено трохи", "Скоро", "OK ближче", "OK далеко"),
            plan.nearest().map { it.name }
        )
    }

    @Test fun limitIsFive_andInactiveSkipped() {
        val items = (1..8).map { item("OK $it", DueStatus.OK, 1_000 * it, 30L * it) } +
            item("Вимкнене", DueStatus.OVERDUE, -1_000, -10, active = false)
        val nearest = overview(items).nearest()
        assertEquals(5, nearest.size)
        assertTrue(nearest.none { it.name == "Вимкнене" })
        assertEquals("OK 1", nearest.first().name)
    }

    @Test fun dateOnlyRule_isShown() {
        // Гальмівна рідина: лише інтервал у місяцях — залишок у днях, км немає.
        val plan = overview(listOf(item("Гальмівна рідина", DueStatus.SOON, null, 10)))
        assertEquals(1, plan.nearest().size)
    }
}
