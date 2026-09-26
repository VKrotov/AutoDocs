package com.autodocs.app.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/** Запис журналу разом з його позиціями (роботи/запчастини) — для списку й перегляду. */
data class RecordWithItems(
    @Embedded val record: ServiceRecord,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val items: List<ServiceRecordItem>
)

/** Загальна сума запису, ₴. */
fun RecordWithItems.total(): Double = items.sumOf { it.price }

/** Назва позиції для показу: власна/збережена назва; порожньо — «Без назви». */
fun ServiceRecordItem.displayName(): String = customName?.takeIf { it.isNotBlank() } ?: "Без назви"
