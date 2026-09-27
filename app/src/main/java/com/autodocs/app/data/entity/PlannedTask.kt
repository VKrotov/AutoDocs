package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Разовий план (схема v2): «зробити до дати і/або до пробігу» без повтору —
 * на відміну від регламенту ([MaintenanceRule]). Обидва терміни необов'язкові:
 * без них це просто нагадування-нотатка в «Плані ТО».
 * [doneAt] != null — виконано; [doneRecordId] — запис журналу, яким закрили план (якщо є).
 */
@Entity(
    tableName = "planned_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ServiceRecord::class,
            parentColumns = ["id"],
            childColumns = ["doneRecordId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("carId"), Index("doneRecordId")]
)
data class PlannedTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val title: String,
    /** Дата «до» — millis опівночі UTC. */
    val dueDate: Long? = null,
    val dueMileage: Int? = null,
    val notes: String? = null,
    val doneAt: Long? = null,
    val doneRecordId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
