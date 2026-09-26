package com.autodocs.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.notify.MaintenanceNotifier
import com.autodocs.app.data.notify.ReminderScheduler
import com.autodocs.app.data.repository.Baseline
import com.autodocs.app.data.repository.ItemInput
import com.autodocs.app.data.repository.PlanRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/** Етап 6: сповіщення про ТО + відкриття «Плану ТО» з натиснутого сповіщення. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class NotificationTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<AutoDocsApp>()
    private val nm get() = shadowOf(app.getSystemService(NotificationManager::class.java))

    @After fun resetDb() = AppDatabase.resetInstanceForTests()

    private fun seedCarWithOilDueSoon(): Long = runBlocking {
        val carId = app.carRepository.addCar(
            Car(name = "Passat", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
                transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 358_000)
        )
        app.planRepository.seedTemplate(carId)
        // Масло міняли 9 400 км тому → лишилось 600 км (≤ 1 000 за замовчуванням) → «скоро».
        app.serviceRepository.saveRecord(
            null, carId, PlanRepository.localDateToUtc(LocalDate.now().minusDays(30)), 348_600, null, null,
            listOf(ItemInput(WorkItemCategory.ROBOTA, "Заміна масла двигуна", 1500.0))
        )
        carId
    }

    @Test fun dailyCheck_postsOnce_thenAntiSpam_forceAlwaysPosts() = runBlocking {
        seedCarWithOilDueSoon()
        // Без дозволу — нічого.
        assertEquals(MaintenanceNotifier.Result.NO_PERMISSION, MaintenanceNotifier.check(app))

        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(MaintenanceNotifier.Result.SENT, MaintenanceNotifier.check(app))
        val n = nm.allNotifications.single()
        val text = n.extras.getCharSequence("android.text").toString()
        assertTrue(text, text.startsWith("Заміна масла двигуна — через 600"))

        nm.allNotifications.forEach { } // той самий день — антиспам
        assertEquals(MaintenanceNotifier.Result.NOTHING, MaintenanceNotifier.check(app))
        assertEquals(MaintenanceNotifier.Result.SENT, MaintenanceNotifier.check(app, force = true))
    }

    @Test fun unknownRulesOnly_nothingToSend() = runBlocking {
        runBlocking {
            val carId = app.carRepository.addCar(
                Car(name = "P", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
                    transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 358_000)
            )
            app.planRepository.seedTemplate(carId)
        }
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(MaintenanceNotifier.Result.NOTHING, MaintenanceNotifier.check(app))
        assertTrue(nm.allNotifications.isEmpty())
    }

    @Test fun schedulerDelay_nextHour() {
        val now = LocalDateTime.of(2026, 9, 26, 9, 30)
        assertEquals(30L, ReminderScheduler.delayUntil(10, now).toMinutes())
        assertEquals(23L * 60 + 30, ReminderScheduler.delayUntil(9, now).toMinutes())
    }

    @Test fun tapOnNotification_opensPlanTab() {
        seedCarWithOilDueSoon()
        val intent = Intent(app, MainActivity::class.java).putExtra(MaintenanceNotifier.EXTRA_OPEN, MaintenanceNotifier.OPEN_PLAN)
        ActivityScenario.launch<MainActivity>(intent).use {
            compose.waitUntil(5000) {
                compose.onAllNodes(hasText("Коли робили востаннє", substring = true) or hasText("без відмітки", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
