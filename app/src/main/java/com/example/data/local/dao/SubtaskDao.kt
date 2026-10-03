package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    fun getSubtasksForTask(taskId: Long): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    suspend fun getSubtasksForTaskSync(taskId: Long): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setSubtaskCompleted(id: Long, isCompleted: Boolean)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubtaskById(id: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Long)
}
