package com.autodocs.app

import android.Manifest
import android.app.NotificationManager
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.CarDocument
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.notify.MaintenanceNotifier
import com.autodocs.app.data.repository.PlanRepository
import com.autodocs.app.ui.util.formatKm
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Етап 9: страховка з головної → продовження поліса; разовий план → виконано через журнал; сповіщення. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class DeadlinesFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @After fun resetDb() = com.autodocs.app.data.AppDatabase.resetInstanceForTests()

    private fun app() = rule.activity.application as AutoDocsApp
    private fun exists(m: SemanticsMatcher) = rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty()
    private fun utc(d: LocalDate) = PlanRepository.localDateToUtc(d)
    private val today = LocalDate.now()

    private fun seedCar(): Long = runBlocking {
        app().carRepository.addCar(
            Car(name = "Passat", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
                transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 358_000)
        )
    }

    @Test fun insurance_homeLine_documents_renew() {
        val carId = seedCar()
        val oldUntil = today.plusDays(10)
        runBlocking {
            app().documentRepository.save(
                CarDocument(carId = carId, type = DocumentType.OSAGO, company = "ТАС", number = "EP-1",
                    validFrom = utc(oldUntil.minusYears(1).plusDays(1)), validUntil = utc(oldUntil)),
                emptyList()
            )
        }
        rule.waitUntil(5000) { exists(hasText("Автоцивілка")) }
        // На головній — найтерміновіший документ жовтим «10 днів».
        rule.onNodeWithText("10 днів").assertExists()

        rule.onNodeWithText("Автоцивілка").performClick()
        rule.waitUntil(5000) { exists(hasText("Страховка й техогляд")) }
        rule.waitForIdle()
        rule.onNodeWithText("Техпаспорт").assertExists()
        rule.onNodeWithText("Автоцивілка").performClick()

        rule.waitUntil(5000) { exists(hasText("Продовжити — внести новий поліс")) }
        rule.onNodeWithText("Продовжити — внести новий поліс").performScrollTo().performClick()
        rule.waitUntil(5000) { exists(hasText("Новий поліс")) }
        rule.onNodeWithText("Додати документ").performScrollTo().performClick()

        // Назад у списку: новий поліс поточний, старий — у «Попередні».
        rule.waitUntil(5000) { exists(hasText("Попередні · 1")) }
        val docs = runBlocking { app().documentRepository.getForCar(carId) }
        assertEquals(2, docs.size)
        val renewed = docs.maxBy { it.validUntil }
        assertEquals(DocumentType.OSAGO, renewed.type)
        assertEquals("ТАС", renewed.company)
        assertEquals(utc(oldUntil.plusDays(1)), renewed.validFrom)
        assertEquals(utc(oldUntil.plusYears(1)), renewed.validUntil)

        // Старий поліс більше не нагадує: сповіщень про документи немає.
        shadowOf(app()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        runBlocking { MaintenanceNotifier.check(app()) }
        val nm = shadowOf(app().getSystemService(NotificationManager::class.java))
        assertTrue(nm.allNotifications.none { it.extras.getCharSequence("android.title").toString().startsWith("Документи") })
    }

    @Test fun expiringPolicy_notifies() {
        val carId = seedCar()
        runBlocking {
            app().documentRepository.save(
                CarDocument(carId = carId, type = DocumentType.OSAGO, validUntil = utc(today.plusDays(5))), emptyList()
            )
        }
        shadowOf(app()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(MaintenanceNotifier.Result.SENT, runBlocking { MaintenanceNotifier.check(app()) })
        val nm = shadowOf(app().getSystemService(NotificationManager::class.java))
        val n = nm.allNotifications.single { it.extras.getCharSequence("android.title").toString().startsWith("Документи") }
        assertEquals("Автоцивілка — закінчується через 5 днів", n.extras.getCharSequence("android.text").toString())
    }

    @Test fun oneOffTask_addFromPlan_showOnHome_doneViaJournal() {
        val carId = seedCar()
        rule.waitUntil(5000) { exists(hasText("Passat")) }

        rule.onNodeWithText("План ТО").performClick()
        rule.waitUntil(5000) { exists(hasText("Разовий план")) }
        rule.onNodeWithText("Разовий план").performScrollTo().performClick()

        rule.waitUntil(5000) { exists(hasText("Новий разовий план")) }
        rule.onNode(hasSetTextAction() and hasText("Що зробити", substring = true)).performTextInput("Шарові опори")
        rule.onNodeWithText("+${formatKm(1_000)} км").performClick()
        rule.onNodeWithText("Додати в план").performScrollTo().performClick()

        rule.waitUntil(5000) { exists(hasText("Разові плани")) }
        val task = runBlocking { app().database.backupDao().allTasks().single() }
        assertEquals("Шарові опори", task.title)
        assertEquals(359_000, task.dueMileage)
        rule.onNodeWithText("до ${formatKm(359_000)} км").assertExists()

        // Разовий план з терміном — і в «Найближче ТО» на головній.
        rule.onNodeWithText("Головна").performClick()
        rule.waitUntil(5000) { exists(hasText("Шарові опори")) }
        rule.waitForIdle()
        rule.onNodeWithText("Шарові опори").performScrollTo().performClick()

        rule.waitUntil(5000) { exists(hasText("Виконано")) }
        rule.onNodeWithText("Виконано").performScrollTo().performClick()
        rule.waitUntil(5000) { exists(hasText("Записати в журнал")) }
        rule.onNodeWithText("Записати в журнал").performClick()

        // Форма запису вже з позицією «Шарові опори».
        rule.waitUntil(5000) { exists(hasText("Новий запис")) }
        rule.onNode(hasSetTextAction() and hasText("Шарові опори")).assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Зберегти запис"))
        rule.onNodeWithText("Зберегти запис").performClick()
        rule.waitUntil(5000) { !exists(hasText("Зберегти запис")) }

        val done = runBlocking { app().planRepository.getTask(task.id)!! }
        val record = runBlocking { app().database.backupDao().allRecords().single() }
        assertNotNull(done.doneAt)
        assertEquals(record.id, done.doneRecordId)
        assertEquals(carId, record.carId)

        rule.onNodeWithText("План ТО").performClick()
        rule.waitUntil(5000) { exists(hasText("Виконані · 1")) }
        rule.onNodeWithContentDescription("Додати в план").assertExists()
    }
}
