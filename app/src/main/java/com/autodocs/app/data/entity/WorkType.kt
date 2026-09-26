package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WorkItemCategory { ROBOTA, ZAPCHASTYNA }

/**
 * Довідник типів робіт/запчастин (F06) — і початковий сідований набір,
 * і власні пункти, додані користувачем.
 */
@Entity(tableName = "work_types")
data class WorkType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: WorkItemCategory,
    val isCustom: Boolean = false
)
