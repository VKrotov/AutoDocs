package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autodocs.app.data.entity.Car
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars WHERE isArchived = 0 LIMIT 1")
    fun observeActiveCar(): Flow<Car?>

    @Query("SELECT * FROM cars WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun observeArchivedCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getById(id: Long): Car?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(car: Car): Long

    @Update
    suspend fun update(car: Car)

    @Delete
    suspend fun delete(car: Car)

    @Query("UPDATE cars SET mileage = :mileage, mileageUpdatedAt = :updatedAt WHERE id = :carId")
    suspend fun updateMileage(carId: Long, mileage: Int, updatedAt: Long)

    @Query("UPDATE cars SET isArchived = 1 WHERE id = :carId")
    suspend fun archive(carId: Long)
}
