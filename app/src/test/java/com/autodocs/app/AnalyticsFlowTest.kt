package com.autodocs.app

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.repository.ItemInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/** Етап 8: виправлення помилкового пробігу, пошук у журналі, статистика, графік пробігу. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AnalyticsFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @After fun resetDb() = com.autodocs.app.data.AppDatabase.resetInstanceForTests()

    private fun app() = rule.activity.application as AutoDocsApp
    private fun exists(m: SemanticsMatcher) = rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty()
    private fun utc(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun seed(): Long = runBlocking {
        val carId = app().carRepository.addCar(
            Car(name = "Passat", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
                transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 350_000)
        )
        val year = LocalDate.now().year
        app().serviceRepository.saveRecord(
            null, carId, utc(year, 1, 15), 350_500, "Бош-сервіс", null,
            listOf(ItemInput(WorkItemCategory.ROBOTA, "Заміна масла двигуна", 600.0), ItemInput(WorkItemCategory.ZAPCHASTYNA, "Масло 5W-40", 1900.0))
        )
        app().serviceRepository.saveRecord(
            null, carId, utc(year, 1, 20), 350_700, null, "скрип",
            listOf(ItemInput(WorkItemCategory.ZAPCHASTYNA, "Колодки передні", 1400.0))
        )
        carId
    }

    @Test fun deleteMistakenEntry_restoresCarMileage() {
        val carId = seed()
        runBlocking {
            app().carRepository.updateMileage(carId, 351_000)
            app().carRepository.updateMileage(carId, 3_510_000) // зайвий нуль
            assertEquals(3_510_000, app().carRepository.getCar(carId)!!.mileage)

            val car = app().carRepository.getCar(carId)!!
            val history = app().mileageRepository.observeHistory(car).first()
            val typo = history.single { it.mileage == 3_510_000 }
            assert(typo.suspicious)

            app().mileageRepository.deleteEntry(typo.refId)
            assertEquals(351_000, app().carRepository.getCar(carId)!!.mileage)
        }
    }

    @Test fun journalSearch_statsAndMileageScreens() {
        seed()
        rule.waitUntil(5000) { exists(hasText("Passat")) }

        // Графік пробігу з головної.
        rule.onNodeWithContentDescription("Історія пробігу").performClick()
        rule.waitUntil(5000) { exists(hasText("Історія", substring = true)) }
        rule.onNodeWithText("Торкнись графіка", substring = true).assertExists()
        rule.onNodeWithContentDescription("Назад").performClick()

        // Пошук у журналі.
        rule.onNodeWithText("Журнал").performClick()
        rule.waitUntil(5000) { exists(hasText("2 записи", substring = true)) }
        rule.onNodeWithContentDescription("Пошук").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("колодки")
        rule.waitUntil(5000) { exists(hasText("Знайдено: 1 запис", substring = true)) }
        rule.onNodeWithText("Колодки передні", substring = true).assertExists()

        // Статистика витрат.
        rule.onNodeWithContentDescription("Статистика витрат").performClick()
        rule.waitUntil(5000) { exists(hasText("Витрачено за ${LocalDate.now().year}")) }
        rule.onNodeWithText("3 900", substring = true).assertExists()
        rule.onNodeWithText("Найбільші витрати").assertExists()
    }
}
