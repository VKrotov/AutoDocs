package com.autodocs.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.Migrations
import com.autodocs.app.data.entity.CarDocument
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.PlannedTask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Етап 9: перша міграція бази (v1 → v2). Дані з v1 мають пережити оновлення застосунку,
 * а нові таблиці — з'явитися саме такими, як їх очікує Room.
 *
 * Базу v1 будуємо рівно з експортованої схеми `schemas/.../1.json` (тієї, з якою жила 0.8.0),
 * далі відкриваємо її Room'ом v2: Room виконує міграцію й сам звіряє всі таблиці, індекси
 * й зовнішні ключі з очікуваними — будь-яка розбіжність дає виняток.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test.db"
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    /** Порожня база v1 — як її створила б 0.8.0. */
    private fun createV1(): SQLiteDatabase {
        val schema = JSONObject(java.io.File("schemas/com.autodocs.app.data.AppDatabase/1.json").readText()).getJSONObject("database")
        val file = context.getDatabasePath(dbName).apply { parentFile?.mkdirs(); delete() }
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            val table = e.getString("tableName")
            db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = e.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) {
                db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = 1
        return db
    }

    @Test fun v1_to_v2_keepsData_andAddsV2Tables() {
        createV1().apply {
            execSQL(
                "INSERT INTO cars (id, name, make, model, engine, fuelType, transmissionType, licensePlate, vin, photoUri, mileage, mileageUpdatedAt, isArchived, createdAt) " +
                    "VALUES (1, 'Passat', 'Volkswagen', 'Passat B5+', '2.0 AZM', 'PETROL', 'MANUAL', 'ВТ7164ВМ', 'WVW', NULL, 358248, 10, 0, 11)"
            )
            execSQL("INSERT INTO work_types (id, name, category, isCustom) VALUES (5, 'Заміна масла двигуна', 'ROBOTA', 0)")
            execSQL("INSERT INTO service_records (id, carId, date, mileage, stoName, notes, createdAt) VALUES (3, 1, 1790380800000, 358100, 'СТО', NULL, 30)")
            execSQL("INSERT INTO service_record_items (id, recordId, workTypeId, customName, category, price) VALUES (7, 3, 5, 'Заміна масла двигуна', 'ROBOTA', 400.0)")
            execSQL("INSERT INTO maintenance_rules (id, carId, workTypeId, intervalKm, intervalMonths, lastDoneMileage, lastDoneDate, isActive) VALUES (1, 1, 5, 10000, 12, NULL, NULL, 1)")
            execSQL("INSERT INTO mileage_entries (id, carId, mileage, date) VALUES (4, 1, 358248, 40)")
            execSQL("INSERT INTO photos (id, ownerType, ownerId, kind, uri, createdAt) VALUES (6, 'CAR', 1, 'TECH_PASSPORT_FRONT', 'file:///x.jpg', 50)")
            close()
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                assertEquals(2, db.openHelper.readableDatabase.version)
                val dao = db.backupDao()
                assertEquals("Passat", dao.allCars().single().name)
                assertEquals(358248, dao.allCars().single().mileage)
                assertEquals(1, dao.allRecords().size)
                assertEquals(400.0, dao.allRecordItems().single().price, 0.0)
                assertEquals(10000, dao.allRules().single().intervalKm)
                assertEquals(1, dao.allMileage().size)
                assertEquals(1, dao.allPhotos().size)

                // Нові таблиці працюють, зовнішні ключі — теж.
                val docId = db.carDocumentDao().insert(
                    CarDocument(carId = 1, type = DocumentType.OSAGO, validUntil = 1821830400000)
                )
                val taskId = db.plannedTaskDao().insert(PlannedTask(carId = 1, title = "Шарові", doneAt = 1, doneRecordId = 3))
                assertEquals(DocumentType.OSAGO, db.carDocumentDao().getById(docId)!!.type)
                // Видалили запис журналу — план лишається виконаним, але без посилання (SET NULL).
                db.openHelper.writableDatabase.execSQL("DELETE FROM service_records WHERE id = 3")
                assertNull(db.plannedTaskDao().getById(taskId)!!.doneRecordId)
            }
        } finally {
            db.close()
        }
    }

    /** Свіжа інсталяція одразу створює v2 — і схема така сама, як після міграції. */
    @Test fun freshInstall_opensAtV2() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "fresh.db").allowMainThreadQueries().build()
        try {
            assertEquals(2, db.openHelper.readableDatabase.version)
            assertEquals(AppDatabase.SCHEMA_VERSION, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
        }
    }
}
