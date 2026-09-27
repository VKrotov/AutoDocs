package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Таблиці для етапу 11 «Гараж». Закладені в схему v2 наперед (одна міграція на весь V2),
// екранів поки немає; бекап їх уже зберігає.

/**
 * Довідник СТО. Із записами журналу пов'язується за назвою ([ServiceRecord.stoName],
 * без урахування регістру) — так само, як уже працюють підказки й статистика.
 */
@Entity(tableName = "stations")
data class Station(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TireSeason { SUMMER, WINTER, ALL_SEASON }

/** Комплект шин (або коліс) авто. */
@Entity(
    tableName = "tire_sets",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class TireSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val name: String,
    val season: TireSeason,
    /** Виробник і модель, напр. «Michelin Alpin 6». */
    val brand: String = "",
    /** Розмір, напр. «205/55 R16». */
    val size: String = "",
    /** Рік виготовлення (DOT), якщо відомий. */
    val year: Int? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Встановлення комплекту на авто (сезонна заміна). Пробіг комплекту = сума відрізків між встановленнями. */
@Entity(
    tableName = "tire_swaps",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TireSet::class,
            parentColumns = ["id"],
            childColumns = ["tireSetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index("tireSetId")]
)
data class TireSwap(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val tireSetId: Long,
    /** Дата встановлення — millis опівночі UTC. */
    val date: Long,
    val mileage: Int,
    val notes: String? = null
)
