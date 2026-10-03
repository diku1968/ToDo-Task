package com.example.data.repository

import com.example.data.local.dao.SubtaskDao
import com.example.data.local.dao.TagDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.*
import com.example.data.preferences.UserPreferencesRepository
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler
import com.example.utils.RecurrenceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao,
    private val subtaskDao: SubtaskDao,
    private val reminderScheduler: ReminderScheduler,
    private val notificationHelper: NotificationHelper,
    private val preferencesRepository: UserPreferencesRepository
) {

    val allActiveTasks: Flow<List<TaskWithDetails>> = taskDao.getAllActiveTasksWithDetails()

    val archivedTasks: Flow<List<TaskWithDetails>> = taskDao.getArchivedTasksWithDetails()

    fun getTaskById(taskId: Long): Flow<TaskWithDetails?> = taskDao.getTaskWithDetailsById(taskId)

    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithDetails>> =
        taskDao.getTasksForDateRange(startOfDay, endOfDay)

    fun getTasksForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<TaskWithDetails>> =
        taskDao.getTasksForMonth(startOfMonth, endOfMonth)

    fun searchTasks(query: String): Flow<List<TaskWithDetails>> = taskDao.searchTasks(query)

    suspend fun createTask(
        task: TaskEntity,
        tagIds: List<Long> = emptyList(),
        initialSubtasks: List<String> = emptyList()
    ): Long {
        val taskId = taskDao.insertTask(task)

        if (tagIds.isNotEmpty()) {
            val crossRefs = tagIds.map { TaskTagCrossRef(taskId = taskId, tagId = it) }
            tagDao.insertTaskTagCrossRefs(crossRefs)
        }

        if (initialSubtasks.isNotEmpty()) {
            val subtasks = initialSubtasks
                .filter { it.isNotBlank() }
                .mapIndexed { index, title ->
                    SubtaskEntity(
                        taskId = taskId,
                        title = title.trim(),
                        isCompleted = false,
                        sortOrder = index
                    )
                }
            subtaskDao.insertSubtasks(subtasks)
        }

        if (task.reminderEnabled && task.reminderTime != null) {
            reminderScheduler.scheduleReminder(
                taskId = taskId,
                title = task.title,
                description = task.description,
                triggerTime = task.reminderTime
            )
        }

        return taskId
    }

    suspend fun updateTask(
        task: TaskEntity,
        tagIds: List<Long>? = null
    ) {
        val updatedTask = task.copy(updatedAt = System.currentTimeMillis())
        taskDao.updateTask(updatedTask)

        if (tagIds != null) {
            tagDao.deleteTagsForTask(task.id)
            val crossRefs = tagIds.map { TaskTagCrossRef(taskId = task.id, tagId = it) }
            tagDao.insertTaskTagCrossRefs(crossRefs)
        }

        if (updatedTask.reminderEnabled && updatedTask.reminderTime != null && !updatedTask.isCompleted) {
            reminderScheduler.scheduleReminder(
                taskId = updatedTask.id,
                title = updatedTask.title,
                description = updatedTask.description,
                triggerTime = updatedTask.reminderTime
            )
        } else {
            reminderScheduler.cancelReminder(task.id)
        }
    }

    suspend fun setTaskCompleted(taskId: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        taskDao.setTaskCompleted(taskId, isCompleted, completedAt)

        val task = taskDao.getTaskById(taskId) ?: return

        if (isCompleted) {
            reminderScheduler.cancelReminder(taskId)
            notificationHelper.cancelNotification(taskId)

            // Recurring task handling: schedule next occurrence
            if (task.repeatType != RepeatType.NONE && task.dueDate != null) {
                val nextDueDate = RecurrenceCalculator.calculateNextDueDate(
                    currentDueDate = task.dueDate,
                    repeatType = task.repeatType,
                    repeatInterval = task.repeatInterval ?: 1
                )

                if (nextDueDate != null) {
                    val withinEndDate = task.repeatEndDate == null || nextDueDate <= task.repeatEndDate
                    if (withinEndDate) {
                        // Calculate next reminder time if enabled
                        val nextReminderTime = if (task.reminderEnabled && task.reminderTime != null) {
                            val diff = task.reminderTime - task.dueDate
                            nextDueDate + diff
                        } else null

                        val nextTask = task.copy(
                            id = 0,
                            isCompleted = false,
                            completedAt = null,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            dueDate = nextDueDate,
                            reminderTime = nextReminderTime
                        )

                        // Copy subtasks in uncompleted state
                        val currentSubtasks = subtaskDao.getSubtasksForTaskSync(taskId)
                        val newSubtaskTitles = currentSubtasks.map { it.title }

                        createTask(nextTask, emptyList(), newSubtaskTitles)
                    }
                }
            }
        } else {
            // Un-completing task, restore reminder if still in future
            if (task.reminderEnabled && task.reminderTime != null && task.reminderTime > System.currentTimeMillis()) {
                reminderScheduler.scheduleReminder(
                    taskId = taskId,
                    title = task.title,
                    description = task.description,
                    triggerTime = task.reminderTime
                )
            }
        }
    }

    suspend fun setTaskArchived(taskId: Long, archived: Boolean) {
        taskDao.setTaskArchived(taskId, archived)
        if (archived) {
            reminderScheduler.cancelReminder(taskId)
            notificationHelper.cancelNotification(taskId)
        }
    }

    suspend fun deleteTask(taskId: Long) {
        reminderScheduler.cancelReminder(taskId)
        notificationHelper.cancelNotification(taskId)
        taskDao.deleteTaskById(taskId)
    }

    suspend fun clearCompletedTasks() {
        taskDao.clearCompletedTasks()
    }

    suspend fun duplicateTask(taskId: Long): Long? {
        val original = taskDao.getTaskById(taskId) ?: return null
        val subtasks = subtaskDao.getSubtasksForTaskSync(taskId)

        val copyTask = original.copy(
            id = 0,
            title = "${original.title} (Copy)",
            isCompleted = false,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val newId = taskDao.insertTask(copyTask)
        if (subtasks.isNotEmpty()) {
            val copiedSubtasks = subtasks.map {
                it.copy(id = 0, taskId = newId, isCompleted = false)
            }
            subtaskDao.insertSubtasks(copiedSubtasks)
        }
        return newId
    }

    // Subtasks operations
    suspend fun addSubtask(taskId: Long, title: String): Long {
        val count = subtaskDao.getSubtasksForTaskSync(taskId).size
        return subtaskDao.insertSubtask(
            SubtaskEntity(
                taskId = taskId,
                title = title.trim(),
                isCompleted = false,
                sortOrder = count
            )
        )
    }

    suspend fun setSubtaskCompleted(subtaskId: Long, isCompleted: Boolean, parentTaskId: Long) {
        subtaskDao.setSubtaskCompleted(subtaskId, isCompleted)

        // Check if parent should be auto-completed
        val prefs = preferencesRepository.userPreferencesFlow.first()
        if (prefs.autoCompleteParent) {
            val allSubtasks = subtaskDao.getSubtasksForTaskSync(parentTaskId)
            if (allSubtasks.isNotEmpty() && allSubtasks.all { it.isCompleted }) {
                setTaskCompleted(parentTaskId, true)
            }
        }
    }

    suspend fun deleteSubtask(subtaskId: Long) {
        subtaskDao.deleteSubtaskById(subtaskId)
    }
}
