package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** CAR_DOCUMENT — поліс/техогляд (схема v2). */
enum class PhotoOwnerType { CAR, SERVICE_RECORD, CAR_DOCUMENT }

enum class PhotoKind { CAR_PHOTO, TECH_PASSPORT_FRONT, TECH_PASSPORT_BACK, RECORD_PHOTO, SCAN }

/**
 * Фото авто, техпаспорта (F26) чи додані до запису журналу (F05/F25).
 * Без окремого зовнішнього ключа на дві різні таблиці — [ownerType]+[ownerId]
 * визначають, до чого належить фото; каскадне видалення робимо в репозиторії.
 */
@Entity(tableName = "photos", indices = [Index("ownerType", "ownerId")])
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerType: PhotoOwnerType,
    val ownerId: Long,
    val kind: PhotoKind,
    val uri: String,
    val createdAt: Long = System.currentTimeMillis()
)
