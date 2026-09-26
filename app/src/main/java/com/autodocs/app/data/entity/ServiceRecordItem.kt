package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Одна позиція в записі журналу: робота або запчастина з ціною.
 * [workTypeId] — посилання на довідник; якщо null, використовується [customName]
 * (користувач ввів свою назву, не обираючи з довідника).
 */
@Entity(
    tableName = "service_record_items",
    foreignKeys = [
        ForeignKey(
            entity = ServiceRecord::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkType::class,
            parentColumns = ["id"],
            childColumns = ["workTypeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("recordId"), Index("workTypeId")]
)
data class ServiceRecordItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val workTypeId: Long? = null,
    val customName: String? = null,
    val category: WorkItemCategory,
    val price: Double = 0.0
)
