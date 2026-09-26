package com.autodocs.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTypeDao {
    @Query("SELECT * FROM work_types ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<WorkType>>

    @Query("SELECT * FROM work_types")
    suspend fun getAll(): List<WorkType>

    @Query("SELECT * FROM work_types WHERE id = :id")
    suspend fun getById(id: Long): WorkType?

    @Query("SELECT * FROM work_types WHERE category = :category AND name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(category: WorkItemCategory, name: String): WorkType?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(workType: WorkType): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(workTypes: List<WorkType>)

    @Update
    suspend fun update(workType: WorkType)

    @Delete
    suspend fun delete(workType: WorkType)

    @Query("SELECT COUNT(*) FROM work_types")
    suspend fun count(): Int
}
