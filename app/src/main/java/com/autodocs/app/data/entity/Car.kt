package com.autodocs.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FuelType { PETROL, DIESEL, GAS, HYBRID, ELECTRIC }

enum class TransmissionType { MANUAL, AUTOMATIC, ROBOT, VARIATOR }

/**
 * Автомобіль користувача. Активний лише один [isArchived] == false одночасно
 * (правило контролюється на рівні репозиторію/UI, не в БД).
 */
@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val make: String,
    val model: String,
    val engine: String,
    val fuelType: FuelType,
    val transmissionType: TransmissionType,
    val licensePlate: String,
    val vin: String,
    val photoUri: String? = null,
    val mileage: Int = 0,
    val mileageUpdatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
