package com.autodocs.app

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.repository.PhotoRef
import com.autodocs.app.data.repository.PhotoRepository
import com.autodocs.app.ui.screens.record.RecordFormViewModel
import com.autodocs.app.ui.screens.record.RecordFormViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.os.Looper
import java.io.File

/** Етап 7: фото до записів і техпаспорт — сховище, ліміт 5, прибирання файлів. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoTest {
    private val app get() = ApplicationProvider.getApplicationContext<AutoDocsApp>()

    @After fun reset() = AppDatabase.resetInstanceForTests()

    /** Згенерувати JPEG «з галереї» (поза папкою застосунку). */
    private fun sourceImage(name: String, w: Int = 3000, h: Int = 2000): Uri {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 180, 90)) }
        val f = File(app.cacheDir, name)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(f)
    }

    private fun fileOf(uri: String) = File(Uri.parse(uri).path!!)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test fun importImage_downscalesAndCopiesIntoApp() = runBlocking {
        val stored = PhotoStorage.importImage(app, sourceImage("a.jpg"), "rec")!!
        val f = fileOf(stored)
        assertTrue(f.isFile)
        assertEquals(PhotoStorage.photosDir(app).canonicalPath, f.parentFile!!.canonicalPath)
        val bmp = PhotoStorage.decodeScaled(app, Uri.parse(stored), 10_000)!!
        assertEquals(1600, maxOf(bmp.width, bmp.height))
        // Дві сторінки підряд — різні файли.
        val s2 = PhotoStorage.importImage(app, sourceImage("b.jpg"), "rec")!!
        assertTrue(stored != s2)
    }

    @Test fun replaceFor_andDeleteRecord_cleanUpFiles() = runBlocking {
        val repo = app.photoRepository
        val a = PhotoStorage.importImage(app, sourceImage("a.jpg"), "rec")!!
        val b = PhotoStorage.importImage(app, sourceImage("b.jpg"), "rec")!!
        repo.replaceFor(PhotoOwnerType.SERVICE_RECORD, 7, listOf(PhotoRef(a, PhotoKind.RECORD_PHOTO), PhotoRef(b, PhotoKind.SCAN)))
        assertEquals(2, repo.getFor(PhotoOwnerType.SERVICE_RECORD, 7).size)

        repo.replaceFor(PhotoOwnerType.SERVICE_RECORD, 7, listOf(PhotoRef(b, PhotoKind.SCAN)))
        assertEquals(listOf(b), repo.getFor(PhotoOwnerType.SERVICE_RECORD, 7).map { it.uri })
        assertFalse("прибране фото видалено з диска", fileOf(a).exists())

        // Видалення запису журналу забирає і його фото.
        val carId = app.carRepository.addCar(Car(name = "P", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
            transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 1))
        val recId = app.serviceRepository.saveRecord(null, carId, 0L, 1, null, null,
            listOf(com.autodocs.app.data.repository.ItemInput(com.autodocs.app.data.entity.WorkItemCategory.ROBOTA, "X", 0.0)))
        repo.replaceFor(PhotoOwnerType.SERVICE_RECORD, recId, listOf(PhotoRef(b, PhotoKind.SCAN)))
        app.serviceRepository.deleteRecord(recId)
        assertTrue(repo.getFor(PhotoOwnerType.SERVICE_RECORD, recId).isEmpty())
        assertFalse(fileOf(b).exists())
    }

    @Test fun techPassport_setSingle_replacesOldFile() = runBlocking {
        val repo = app.photoRepository
        val f1 = PhotoStorage.importImage(app, sourceImage("p1.jpg"), "passport")!!
        val f2 = PhotoStorage.importImage(app, sourceImage("p2.jpg"), "passport")!!
        repo.setSingle(PhotoOwnerType.CAR, 1, PhotoKind.TECH_PASSPORT_FRONT, f1)
        repo.setSingle(PhotoOwnerType.CAR, 1, PhotoKind.TECH_PASSPORT_FRONT, f2)
        val list = repo.getFor(PhotoOwnerType.CAR, 1)
        assertEquals(listOf(f2), list.map { it.uri })
        assertFalse(fileOf(f1).exists())
        repo.setSingle(PhotoOwnerType.CAR, 1, PhotoKind.TECH_PASSPORT_FRONT, null)
        assertTrue(repo.getFor(PhotoOwnerType.CAR, 1).isEmpty())
        assertFalse(fileOf(f2).exists())
    }

    @Test fun recordForm_limit5_saveLinksPhotos_cancelDiscardsFiles() {
        runBlocking {
            app.carRepository.addCar(Car(name = "P", make = "VW", model = "B5", engine = "", fuelType = FuelType.PETROL,
                transmissionType = TransmissionType.MANUAL, licensePlate = "", vin = "", mileage = 358_000))
        }
        val factory = RecordFormViewModelFactory(app, app.carRepository, app.serviceRepository, app.photoRepository)

        // 1) Скасована форма: скопійовані файли прибираються.
        val store1 = ViewModelStore()
        val vm1 = ViewModelProvider(store1, factory)[RecordFormViewModel::class.java]
        vm1.init(null)
        waitFor { !vm1.state.value.isLoading }
        vm1.onPhotosPicked(listOf(sourceImage("c1.jpg")), isScan = false)
        waitFor { vm1.state.value.photos.size == 1 }
        val orphan = vm1.state.value.photos.first().uri
        assertTrue(fileOf(orphan).exists())
        store1.clear()
        assertFalse("файл скасованої форми видалено", fileOf(orphan).exists())

        // 2) Ліміт 5 і збереження.
        val store2 = ViewModelStore()
        val vm = ViewModelProvider(store2, factory)[RecordFormViewModel::class.java]
        vm.init(null)
        waitFor { !vm.state.value.isLoading }
        vm.onPhotosPicked((1..7).map { sourceImage("m$it.jpg", 800, 600) }, isScan = true)
        waitFor { vm.state.value.photos.size == 5 && !vm.state.value.isImportingPhotos }
        assertNotNull(vm.state.value.photoMessage)
        assertEquals(0, vm.state.value.photoSlotsLeft)
        vm.onItemNameChange(vm.state.value.items.first().key, "Заміна масла двигуна")
        vm.save()
        waitFor { vm.state.value.isSaved }
        val recId = runBlocking { app.database.backupDao().allRecords().single().id }
        val saved = runBlocking { app.photoRepository.getFor(PhotoOwnerType.SERVICE_RECORD, recId) }
        assertEquals(5, saved.size)
        assertTrue(saved.all { it.kind == PhotoKind.SCAN })
        store2.clear() // після збереження файли лишаються
        assertTrue(saved.all { fileOf(it.uri).exists() })
    }

    private fun waitFor(timeoutMs: Long = 10_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!cond()) {
            idle()
            if (System.currentTimeMillis() > end) throw AssertionError("Умова не виконалась за $timeoutMs мс")
            Thread.sleep(20)
        }
    }
}
