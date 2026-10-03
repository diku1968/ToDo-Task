package com.example

import android.app.Application
import com.example.ads.AdManager
import com.example.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TagRepository
import com.example.data.repository.TaskRepository
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TaskFlowApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
    val notificationHelper by lazy { NotificationHelper(this) }

    val taskRepository by lazy {
        TaskRepository(
            taskDao = database.taskDao(),
            tagDao = database.tagDao(),
            subtaskDao = database.subtaskDao(),
            reminderScheduler = reminderScheduler,
            notificationHelper = notificationHelper,
            preferencesRepository = userPreferencesRepository
        )
    }

    val categoryRepository by lazy {
        CategoryRepository(
            categoryDao = database.categoryDao(),
            taskDao = database.taskDao()
        )
    }

    val tagRepository by lazy {
        TagRepository(
            tagDao = database.tagDao()
        )
    }

    val backupManager by lazy {
        BackupManager(database)
    }

    val adManager by lazy {
        AdManager(this, userPreferencesRepository)
    }

    override fun onCreate() {
        super.onCreate()
        // Preload ad & initialize notification channel
        notificationHelper
        adManager
    }
}
