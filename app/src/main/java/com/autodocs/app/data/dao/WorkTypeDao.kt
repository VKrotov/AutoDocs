package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.autodocs.app.data.entity.WorkType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTypeDao {
    @Query("SELECT * FROM work_types ORDER BY name ASC")
    fun observeAll(): Flow<List<WorkType>>

    @Query("SELECT * FROM work_types WHERE id = :id")
    suspend fun getById(id: Long): WorkType?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(workType: WorkType): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(workTypes: List<WorkType>)

    @Query("SELECT COUNT(*) FROM work_types")
    suspend fun count(): Int
}
