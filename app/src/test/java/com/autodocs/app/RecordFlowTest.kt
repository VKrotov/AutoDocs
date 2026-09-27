package com.autodocs.app

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Сценарій на весь застосунок (Robolectric + Compose UI test):
 * регресія багів 26.09 — «після запису не повернутись на Головну» і
 * «не зберігається другий запис з частковою назвою (меню підказок перекривало кнопку)».
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class RecordFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @org.junit.After fun resetDb() = com.autodocs.app.data.AppDatabase.resetInstanceForTests()

    private fun app() = rule.activity.application as AutoDocsApp
    private fun scrollTo(m: SemanticsMatcher) { rule.onNode(hasScrollAction()).performScrollToNode(m) }
    private fun exists(m: SemanticsMatcher) = rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty()
    private fun recordsCount() = runBlocking { app().database.backupDao().allRecords().size }

    private fun addRecord(name: String, mileage: String? = null) {
        rule.onNodeWithContentDescription("Додати").performClick()
        rule.waitUntil(5000) { exists(hasText("Новий запис")) }
        if (mileage != null) {
            scrollTo(hasText("358248"))
            rule.onNode(hasSetTextAction() and hasText("358248")).performTextReplacement(mileage)
        }
        scrollTo(hasText("Що робили", substring = true))
        rule.onNode(hasSetTextAction() and hasText("Що робили", substring = true)).performTextInput(name)
        scrollTo(hasText("Зберегти запис"))
        rule.onNodeWithText("Зберегти запис").performClick()
        // Чекаємо саме закриття форми: заголовок «Новий запис» у довгій формі вже прокручений
        // за екран (LazyColumn його не тримає), тож на нього орієнтуватись не можна.
        rule.waitUntil(5000) { !exists(hasText("Зберегти запис")) }
    }

    @Test fun addRecords_thenHomeTabWorks() {
        runBlocking {
            app().carRepository.addCar(
                Car(name = "Passat", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
                    transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 358248)
            )
        }
        rule.waitUntil(5000) { exists(hasText("Passat")) }

        addRecord("Заміна масла двигуна")
        assertEquals(1, recordsCount())

        // Другий запис одразу з вкладки «Журнал»: раніше форма «воскресала» після збереження,
        // лишалась на екрані, і кожне натискання «Зберегти» створювало дубль.
        addRecord("Діагностика ходової")
        assertEquals(2, recordsCount())

        // Після збереження — журнал; тап «Головна» має показати головну, а не повертати у форму/журнал.
        rule.onNodeWithText("Головна").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Редагувати авто").assertExists()

        // Другий запис: менший пробіг, лише робота, назва неповна (підказки відкриті).
        addRecord("Заміна", mileage = "350000")
        assertEquals(3, recordsCount())

        rule.onNodeWithText("Головна").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Редагувати авто").assertExists()
    }
}
