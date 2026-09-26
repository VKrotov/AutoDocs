package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Історія відмічених значень пробігу (F09) — потрібна для прогнозу км/день
 * і точнішого розрахунку наступного ТО між записами журналу.
 */
@Entity(
    tableName = "mileage_entries",
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
data class MileageEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val mileage: Int,
    val date: Long = System.currentTimeMillis()
)
