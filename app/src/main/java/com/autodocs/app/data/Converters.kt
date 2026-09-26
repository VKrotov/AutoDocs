package com.autodocs.app.data

import androidx.room.TypeConverter
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.entity.WorkItemCategory

/** Всі enum-и зберігаються в БД просто як String (name()) — просто й стабільно для міграцій. */
class Converters {
    @TypeConverter
    fun fromFuelType(value: FuelType): String = value.name

    @TypeConverter
    fun toFuelType(value: String): FuelType = FuelType.valueOf(value)

    @TypeConverter
    fun fromTransmissionType(value: TransmissionType): String = value.name

    @TypeConverter
    fun toTransmissionType(value: String): TransmissionType = TransmissionType.valueOf(value)

    @TypeConverter
    fun fromWorkItemCategory(value: WorkItemCategory): String = value.name

    @TypeConverter
    fun toWorkItemCategory(value: String): WorkItemCategory = WorkItemCategory.valueOf(value)

    @TypeConverter
    fun fromPhotoOwnerType(value: PhotoOwnerType): String = value.name

    @TypeConverter
    fun toPhotoOwnerType(value: String): PhotoOwnerType = PhotoOwnerType.valueOf(value)

    @TypeConverter
    fun fromPhotoKind(value: PhotoKind): String = value.name

    @TypeConverter
    fun toPhotoKind(value: String): PhotoKind = PhotoKind.valueOf(value)
}
