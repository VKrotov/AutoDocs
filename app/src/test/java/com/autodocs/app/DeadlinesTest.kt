package com.autodocs.app

import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.CarDocument
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.MaintenanceRule
import com.autodocs.app.data.entity.PlannedTask
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.notify.NotifySettings
import com.autodocs.app.data.notify.ReminderItem
import com.autodocs.app.data.notify.ReminderPlanner
import com.autodocs.app.data.plan.DocumentDeadlines
import com.autodocs.app.data.plan.DoneMark
import com.autodocs.app.data.plan.DueStatus
import com.autodocs.app.data.plan.MaintenanceCalculator
import com.autodocs.app.data.plan.OneOffPlanner
import com.autodocs.app.data.plan.TaskPlan
import com.autodocs.app.data.repository.PlanOverview
import com.autodocs.app.data.repository.PlanRepository
import com.autodocs.app.data.repository.RulePlan
import com.autodocs.app.data.repository.UpcomingEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Етап 9: документи з терміном дії, разові плани, нагадування про них. Чистий JUnit. */
class DeadlinesTest {
    private val today = LocalDate.of(2026, 9, 27)
    private fun utc(d: LocalDate) = PlanRepository.localDateToUtc(d)

    private fun doc(id: Long, type: DocumentType, until: LocalDate, title: String = "") =
        CarDocument(id = id, carId = 1, type = type, title = title, validUntil = utc(until), createdAt = id)

    // ---- документи ----

    @Test fun documentStatus_byDaysLeft() {
        val list = DocumentDeadlines.evaluate(
            listOf(
                doc(1, DocumentType.OSAGO, today.plusDays(45)),
                doc(2, DocumentType.KASKO, today.plusDays(30)),
                doc(3, DocumentType.INSPECTION, today),
                doc(4, DocumentType.GREEN_CARD, today.minusDays(1))
            ),
            today, warnDays = 30
        ).associateBy { it.doc.id }
        assertEquals(DueStatus.OK, list[1]!!.status)
        assertEquals(45L, list[1]!!.daysLeft)
        assertEquals(DueStatus.SOON, list[2]!!.status)       // рівно за 30 днів — уже «скоро»
        assertEquals(DueStatus.SOON, list[3]!!.status)       // сьогодні останній день — ще діє
        assertEquals(0L, list[3]!!.daysLeft)
        assertEquals(DueStatus.OVERDUE, list[4]!!.status)
        assertTrue(list.values.all { it.isCurrent })
    }

    @Test fun renewedPolicy_replacesOld_perTypeAndOtherByTitle() {
        val evaluated = DocumentDeadlines.evaluate(
            listOf(
                doc(1, DocumentType.OSAGO, today.minusDays(3)),          // старий, прострочений
                doc(2, DocumentType.OSAGO, today.plusDays(362)),         // новий, куплений заздалегідь
                doc(3, DocumentType.OTHER, today.minusDays(10), "Довіреність"),
                doc(4, DocumentType.OTHER, today.plusDays(5), "довіреність "), // та сама назва — замінює
                doc(5, DocumentType.OTHER, today.minusDays(10), "Пропуск")
            ),
            today
        )
        val current = DocumentDeadlines.current(evaluated).map { it.doc.id }
        // Прострочені → скоро → решта.
        assertEquals(listOf(5L, 4L, 2L), current)
        assertEquals(listOf(1L, 3L), DocumentDeadlines.previous(evaluated).map { it.doc.id }.sorted())
    }

    @Test fun documentReminders_soonOverdueStopAfterMonth_superseededSilent() {
        val s = NotifySettings(docDaysBefore = 30)
        val evaluated = DocumentDeadlines.evaluate(
            listOf(
                doc(1, DocumentType.OSAGO, today.plusDays(12)),
                doc(2, DocumentType.INSPECTION, today.minusDays(3)),
                doc(3, DocumentType.GREEN_CARD, today.minusDays(40)),  // давно прострочена — мовчимо
                doc(4, DocumentType.KASKO, today.minusDays(1)),        // замінена новою — мовчимо
                doc(5, DocumentType.KASKO, today.plusDays(200))        // ще далеко — мовчимо
            ),
            today, s.docDaysBefore
        )
        val d = ReminderPlanner.decide(emptyList(), 358_000, today, s.copy(mileageDays = 0), emptyMap(), today, documents = evaluated)
        assertEquals(
            listOf(
                "Техогляд — термін дії закінчився 3 дні тому",
                "Автоцивілка — закінчується через 12 днів"
            ),
            d.documentLines
        )
        assertTrue(d.lines.isEmpty())
        // Антиспам: наступного дня — тиша, через тиждень — знову.
        val next = ReminderPlanner.decide(emptyList(), 358_000, today, s.copy(mileageDays = 0), d.newState, today.plusDays(1),
            documents = DocumentDeadlines.evaluate(evaluated.map { it.doc }, today.plusDays(1), 30))
        assertTrue(next.documentLines.isEmpty())
        val week = ReminderPlanner.decide(emptyList(), 358_000, today, s.copy(mileageDays = 0), d.newState, today.plusDays(7),
            documents = DocumentDeadlines.evaluate(evaluated.map { it.doc }, today.plusDays(7), 30))
        assertEquals(2, week.documentLines.size)
    }

    @Test fun documentLine_texts() {
        fun line(days: Long) = ReminderPlanner.documentLine(
            DocumentDeadlines.evaluate(listOf(doc(1, DocumentType.OSAGO, today.plusDays(days))), today).single()
        )
        assertEquals("Автоцивілка — сьогодні останній день дії", line(0))
        assertEquals("Автоцивілка — діє до завтра включно", line(1))
        assertEquals("Автоцивілка — закінчується через 21 день", line(21))
        assertEquals("Автоцивілка — термін дії закінчився учора", line(-1))
    }

    // ---- разові плани ----

    private fun task(id: Long, title: String, km: Int? = null, date: LocalDate? = null) =
        PlannedTask(id = id, carId = 1, title = title, dueMileage = km, dueDate = date?.let(::utc), createdAt = id)

    @Test fun oneOff_compute_kmOrDateWhicheverFirst() {
        // 40 км/день; до 360 000 лишилось 2 000 км ≈ 50 днів, але дата — через 20 днів.
        val p = OneOffPlanner.compute(task(1, "Шарові", km = 360_000, date = today.plusDays(20)), 358_000, 40.0, today)
        assertEquals(2_000, p.remainingKm)
        assertEquals(20L, p.remainingDays)
        assertEquals(DueStatus.OK, p.status)
        assertFalse(p.isUnknown)

        val soonKm = OneOffPlanner.compute(task(2, "Ремінь", km = 358_900), 358_000, 40.0, today)
        assertEquals(DueStatus.SOON, soonKm.status)
        val overdueDate = OneOffPlanner.compute(task(3, "Страховка", date = today.minusDays(1)), 358_000, null, today)
        assertEquals(DueStatus.OVERDUE, overdueDate.status)
        val overdueKm = OneOffPlanner.compute(task(4, "Колодки", km = 357_000), 358_000, null, today)
        assertEquals(-1_000, overdueKm.remainingKm)
        assertEquals(DueStatus.OVERDUE, overdueKm.status)

        val none = OneOffPlanner.compute(task(5, "Лампочка"), 358_000, 40.0, today)
        assertEquals(DueStatus.OK, none.status)
        assertNull(none.remainingKm)
        assertNull(none.remainingDays)
    }

    @Test fun oneOff_sort_overdueSoonThenOkThenNoDeadline() {
        val plans = listOf(
            task(1, "Без терміну"),
            task(2, "Ок", km = 370_000),
            task(3, "Скоро", date = today.plusDays(5)),
            task(4, "Прострочено", km = 357_000),
            task(5, "Ще без терміну")
        ).map { TaskPlan(it, OneOffPlanner.compute(it, 358_000, 40.0, today)) }
        assertEquals(
            listOf("Прострочено", "Скоро", "Ок", "Без терміну", "Ще без терміну"),
            OneOffPlanner.sort(plans).map { it.task.title }
        )
    }

    @Test fun oneOff_reminders_useOwnKeys() {
        val t = task(7, "Шарові", km = 358_500)
        val plan = OneOffPlanner.compute(t, 358_000, 40.0, today)
        val item = ReminderItem(t.id, t.title, plan, key = "t${t.id}")
        val d = ReminderPlanner.decide(listOf(item), 358_000, today, NotifySettings(mileageDays = 0), emptyMap(), today)
        assertEquals(listOf("Шарові — через 500 км / ≈ 13 днів"), d.lines)
        assertTrue(d.newState.containsKey("t7"))
        assertFalse(d.newState.containsKey("r7"))
    }

    @Test fun homeUpcoming_mixesRulesAndTasks_skipsTasksWithoutTerm() {
        val car = Car(id = 1, name = "P", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
            transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 358_000)
        val oil = RulePlan(
            MaintenanceRule(id = 1, carId = 1, workTypeId = 1, intervalKm = 10_000), "Масло", null, null,
            MaintenanceCalculator.compute(10_000, null, DoneMark(null, 350_000, false), 358_000, 40.0, today)
        )
        val tasks = listOf(
            task(1, "Шарові", km = 358_300),
            task(2, "Колодки", date = today.plusDays(100)),
            task(3, "Лампочка")
        ).map { TaskPlan(it, OneOffPlanner.compute(it, 358_000, 40.0, today)) }
        val overview = PlanOverview(car, 358_000, 40.0, listOf(oil), emptyList(), OneOffPlanner.sort(tasks))
        val upcoming = overview.upcoming()
        assertEquals(listOf("Шарові", "Масло", "Колодки"), upcoming.map { it.name })
        assertTrue(upcoming[0] is UpcomingEntry.Task)
        assertTrue(upcoming[1] is UpcomingEntry.Rule)
        // Старий nearest() — лише регламент, як і раніше.
        assertEquals(listOf("Масло"), overview.nearest().map { it.name })
    }
}
