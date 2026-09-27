package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Вид документа з терміном дії (етап 9). */
enum class DocumentType { OSAGO, KASKO, GREEN_CARD, INSPECTION, OTHER }

/**
 * Документ авто з терміном дії: поліс страхування, техогляд тощо (схема v2).
 * Дати — millis опівночі UTC (як у журналі). [validUntil] — останній день дії включно.
 * Продовжений поліс — це НОВИЙ рядок того ж виду; старий лишається в історії і більше не нагадує.
 * Фото/скани поліса — у таблиці `photos` з ownerType = CAR_DOCUMENT.
 */
@Entity(
    tableName = "car_documents",
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
data class CarDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val type: DocumentType,
    /** Власна назва — для виду OTHER (для решти може бути порожньою). */
    val title: String = "",
    val number: String = "",
    /** Страхова компанія / станція техогляду. */
    val company: String = "",
    val validFrom: Long? = null,
    val validUntil: Long,
    val price: Double? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Коротка назва виду: «Автоцивілка», «КАСКО»… */
fun DocumentType.label(): String = when (this) {
    DocumentType.OSAGO -> "Автоцивілка"
    DocumentType.KASKO -> "КАСКО"
    DocumentType.GREEN_CARD -> "Зелена карта"
    DocumentType.INSPECTION -> "Техогляд"
    DocumentType.OTHER -> "Інше"
}

/** Пояснення під назвою виду у формі. */
fun DocumentType.hint(): String? = when (this) {
    DocumentType.OSAGO -> "ОСЦПВ — обов'язкове страхування цивільної відповідальності"
    DocumentType.KASKO -> "Добровільне страхування самого авто"
    DocumentType.GREEN_CARD -> "Страховка для поїздок за кордон"
    DocumentType.INSPECTION -> "Обов'язковий технічний контроль"
    DocumentType.OTHER -> null
}

/** Назва документа для списків і сповіщень. */
fun CarDocument.displayName(): String =
    if (type == DocumentType.OTHER) title.trim().ifEmpty { "Документ" } else type.label()
