package com.autodocs.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Міграції схеми Room. SQL скопійовано з експортованої схеми (`app/schemas/.../<версія>.json`) —
 * Room при відкритті звіряє таблиці з очікуваними, тож будь-яка розбіжність одразу впаде в тесті.
 */
object Migrations {

    /** v1 → v2 (етап 9): лише НОВІ таблиці; наявні дані не змінюються. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            V2_SQL.forEach(db::execSQL)
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)

    private val V2_SQL = listOf(
        "CREATE TABLE IF NOT EXISTS `car_documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `carId` INTEGER NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `number` TEXT NOT NULL, `company` TEXT NOT NULL, `validFrom` INTEGER, `validUntil` INTEGER NOT NULL, `price` REAL, `notes` TEXT, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_car_documents_carId` ON `car_documents` (`carId`)",
        "CREATE TABLE IF NOT EXISTS `planned_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `carId` INTEGER NOT NULL, `title` TEXT NOT NULL, `dueDate` INTEGER, `dueMileage` INTEGER, `notes` TEXT, `doneAt` INTEGER, `doneRecordId` INTEGER, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`doneRecordId`) REFERENCES `service_records`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        "CREATE INDEX IF NOT EXISTS `index_planned_tasks_carId` ON `planned_tasks` (`carId`)",
        "CREATE INDEX IF NOT EXISTS `index_planned_tasks_doneRecordId` ON `planned_tasks` (`doneRecordId`)",
        "CREATE TABLE IF NOT EXISTS `stations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `notes` TEXT, `createdAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `tire_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `carId` INTEGER NOT NULL, `name` TEXT NOT NULL, `season` TEXT NOT NULL, `brand` TEXT NOT NULL, `size` TEXT NOT NULL, `year` INTEGER, `notes` TEXT, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_tire_sets_carId` ON `tire_sets` (`carId`)",
        "CREATE TABLE IF NOT EXISTS `tire_swaps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `carId` INTEGER NOT NULL, `tireSetId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `mileage` INTEGER NOT NULL, `notes` TEXT, FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tireSetId`) REFERENCES `tire_sets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_tire_swaps_carId` ON `tire_swaps` (`carId`)",
        "CREATE INDEX IF NOT EXISTS `index_tire_swaps_tireSetId` ON `tire_swaps` (`tireSetId`)"
    )
}
