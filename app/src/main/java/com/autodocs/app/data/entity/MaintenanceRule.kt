package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Регламент ТО для конкретного авто й типу робіт (F07): інтервал за пробігом
 * і/або часом ("що раніше настане"). [lastDoneMileage]/[lastDoneDate] —
 * коли востаннє виконано (звідси рахується наступне ТО, F08).
 */
@Entity(
    tableName = "maintenance_rules",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkType::class,
            parentColumns = ["id"],
            childColumns = ["workTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId"), Index("workTypeId")]
)
data class MaintenanceRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val workTypeId: Long,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val lastDoneMileage: Int? = null,
    val lastDoneDate: Long? = null,
    val isActive: Boolean = true
)
