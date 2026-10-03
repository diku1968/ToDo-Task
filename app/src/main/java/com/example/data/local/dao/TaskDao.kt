package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Transaction
    @Query("SELECT * FROM tasks WHERE deleted = 0 AND archived = 0 ORDER BY isCompleted ASC, sortOrder ASC, createdAt DESC")
    fun getAllActiveTasksWithDetails(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE deleted = 0 AND archived = 1 ORDER BY updatedAt DESC")
    fun getArchivedTasksWithDetails(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskWithDetailsById(taskId: Long): Flow<TaskWithDetails?>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Transaction
    @Query("SELECT * FROM tasks WHERE deleted = 0 AND archived = 0")
    suspend fun getAllActiveTasksSync(): List<TaskWithDetails>

    @Transaction
    @Query("SELECT * FROM tasks WHERE deleted = 0")
    suspend fun getAllTasksSync(): List<TaskWithDetails>

    @Transaction
    @Query("SELECT * FROM tasks WHERE deleted = 0 AND archived = 0 AND dueDate >= :startOfDay AND dueDate <= :endOfDay ORDER BY isCompleted ASC, dueDate ASC")
    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE deleted = 0 AND archived = 0 AND dueDate >= :startOfMonth AND dueDate <= :endOfMonth")
    fun getTasksForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE deleted = 0 AND archived = 0 AND (
            title LIKE '%' || :query || '%' OR 
            description LIKE '%' || :query || '%' OR 
            notes LIKE '%' || :query || '%'
        )
        ORDER BY isCompleted ASC, createdAt DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, isCompleted: Boolean, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET archived = :archived, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setTaskArchived(taskId: Long, archived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET deleted = 1, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM tasks WHERE isCompleted = 1 AND deleted = 0")
    suspend fun clearCompletedTasks()

    @Query("UPDATE tasks SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun reassignCategoryToNull(categoryId: Long)

    @Query("SELECT * FROM tasks WHERE reminderEnabled = 1 AND isCompleted = 0 AND deleted = 0 AND reminderTime > :now")
    suspend fun getActiveReminders(now: Long): List<TaskEntity>
}
