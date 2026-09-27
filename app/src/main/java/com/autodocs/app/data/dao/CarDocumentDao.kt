package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.autodocs.app.data.entity.CarDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDocumentDao {
    @Query("SELECT * FROM car_documents WHERE carId = :carId ORDER BY validUntil DESC, id DESC")
    fun observeForCar(carId: Long): Flow<List<CarDocument>>

    @Query("SELECT * FROM car_documents WHERE carId = :carId ORDER BY validUntil DESC, id DESC")
    suspend fun getForCar(carId: Long): List<CarDocument>

    @Query("SELECT * FROM car_documents WHERE id = :id")
    suspend fun getById(id: Long): CarDocument?

    /** Назви компаній, що вже траплялись — для підказок-чипів у формі. */
    @Query("SELECT DISTINCT company FROM car_documents WHERE company != '' ORDER BY createdAt DESC")
    fun observeCompanies(): Flow<List<String>>

    @Insert
    suspend fun insert(doc: CarDocument): Long

    @Update
    suspend fun update(doc: CarDocument)

    @Query("DELETE FROM car_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}
