package com.autodocs.app

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.plan.MaintenanceTemplate
import com.autodocs.app.data.repository.ItemInput
import com.autodocs.app.ui.util.todayUtcMillis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Етап 5: регламент → первинне налаштування → відмітка з журналу → редагування інтервалу. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class PlanFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @org.junit.After fun resetDb() = com.autodocs.app.data.AppDatabase.resetInstanceForTests()

    private fun app() = rule.activity.application as AutoDocsApp
    private fun exists(m: SemanticsMatcher) = rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty()
    private fun scrollTo(m: SemanticsMatcher) { rule.onNode(hasScrollAction()).performScrollToNode(m) }

    @Test fun seedTemplate_journalMark_editInterval() {
        val carId = runBlocking {
            app().carRepository.addCar(
                Car(name = "Passat", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
                    transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 358_248)
            )
        }
        rule.waitUntil(5000) { exists(hasText("Passat")) }

        rule.onNodeWithText("План ТО").performClick()
        rule.waitUntil(5000) { exists(hasText("Заповнити стандартний регламент")) }
        rule.onNodeWithText("Заповнити стандартний регламент").performClick()

        // Після сідування — екран «Коли робили востаннє».
        rule.waitUntil(5000) { exists(hasText("Коли робили востаннє")) }
        val rules = runBlocking { app().database.backupDao().allRules() }
        assertEquals(MaintenanceTemplate.azm.size, rules.size)
        assertEquals(MaintenanceTemplate.azm.count { it.isActive }, rules.count { it.isActive })

        // Повторне сідування нічого не дублює.
        assertEquals(0, runBlocking { app().planRepository.seedTemplate(carId) })

        scrollTo(hasText("Зберегти"))
        rule.onNodeWithText("Зберегти").performClick()
        rule.waitUntil(5000) { exists(hasText("без відмітки", substring = true)) }
        val activeCount = MaintenanceTemplate.azm.count { it.isActive }
        rule.onNodeWithText("$activeCount пунктів без відмітки").assertExists()

        // Запис у журналі із заміною масла → правило отримує відмітку автоматично.
        runBlocking {
            app().serviceRepository.saveRecord(
                recordId = null, carId = carId, date = todayUtcMillis(), mileage = 358_248, stoName = null, notes = null,
                items = listOf(ItemInput(WorkItemCategory.ROBOTA, "Заміна масла двигуна", 1500.0))
            )
        }
        rule.waitUntil(5000) { exists(hasText("10\u00A0000 км")) }
        rule.onNodeWithText("${activeCount - 1} пунктів без відмітки").assertExists() // 12 → «пунктів»

        // Редагування інтервалу.
        rule.onNodeWithText("Заміна масла двигуна").performScrollTo().performClick()
        rule.waitUntil(5000) { exists(hasText("Регламент ТО")) }
        rule.onNode(hasSetTextAction() and hasText("10000")).performTextReplacement("15000")
        rule.onNodeWithText("Зберегти").performScrollTo().performClick()
        rule.waitUntil(5000) { exists(hasText("15\u00A0000 км")) }
    }
}
