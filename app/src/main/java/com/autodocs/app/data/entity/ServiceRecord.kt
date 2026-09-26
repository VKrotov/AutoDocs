package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Запис у журналі (F04) — одне відвідування СТО чи один випадок робіт.
 * Конкретні роботи/запчастини з цінами — у [ServiceRecordItem].
 */
@Entity(
    tableName = "service_records",
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
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val date: Long,
    val mileage: Int,
    val stoName: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
