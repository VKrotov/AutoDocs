package com.autodocs.app.data.repository

import com.autodocs.app.data.dao.CarDao
import com.autodocs.app.data.dao.MileageEntryDao
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.MileageEntry
import kotlinx.coroutines.flow.Flow

/**
 * Тонка обгортка над DAO — на цьому етапі логіки небагато, але тут буде
 * зручно додавати правила (наприклад "лише одне активне авто") без того,
 * щоб UI знав про Room напряму.
 */
class CarRepository(
    private val carDao: CarDao,
    private val mileageEntryDao: MileageEntryDao
) {
    fun observeActiveCar(): Flow<Car?> = carDao.observeActiveCar()

    fun observeArchivedCars(): Flow<List<Car>> = carDao.observeArchivedCars()

    suspend fun getCar(id: Long): Car? = carDao.getById(id)

    suspend fun addCar(car: Car): Long = carDao.insert(car)

    suspend fun updateCar(car: Car) = carDao.update(car)

    suspend fun archiveCar(carId: Long) = carDao.archive(carId)

    suspend fun updateMileage(carId: Long, mileage: Int) {
        val now = System.currentTimeMillis()
        carDao.updateMileage(carId, mileage, now)
        mileageEntryDao.insert(MileageEntry(carId = carId, mileage = mileage, date = now))
    }
}
