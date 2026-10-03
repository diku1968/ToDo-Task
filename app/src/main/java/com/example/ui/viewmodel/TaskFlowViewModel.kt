package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ads.AdManager
import com.example.backup.BackupManager
import com.example.data.local.entity.*
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TagRepository
import com.example.data.repository.TaskRepository
import com.example.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TaskTab(val title: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed")
}

enum class SortOption(val title: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    TITLE("Alphabetical"),
    CREATED("Date Created")
}

enum class StatsPeriod(val title: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    ALL_TIME("All Time")
}

data class ProductivityStats(
    val totalCreated: Int = 0,
    val totalCompleted: Int = 0,
    val completionRate: Float = 0f,
    val overdueCount: Int = 0,
    val streakDays: Int = 0,
    val priorityCounts: Map<Priority, Int> = emptyMap(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val dailyCompletions: List<Pair<String, Int>> = emptyList() // Day label to count (e.g. "Mon" to 3)
)

class TaskFlowViewModel(
    val taskRepository: TaskRepository,
    val categoryRepository: CategoryRepository,
    val tagRepository: TagRepository,
    val userPreferencesRepository: UserPreferencesRepository,
    val backupManager: BackupManager,
    val adManager: AdManager
) : ViewModel() {

    // Preferences
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    // Categories and Tags
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = tagRepository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All active tasks from DB
    val allActiveTasks: StateFlow<List<TaskWithDetails>> = taskRepository.allActiveTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedTasks: StateFlow<List<TaskWithDetails>> = taskRepository.archivedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Filters and Sorting
    val selectedTab = MutableStateFlow(TaskTab.ALL)
    val selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedPriorityFilter = MutableStateFlow<Priority?>(null)
    val selectedSortOption = MutableStateFlow(SortOption.DUE_DATE)
    val searchQuery = MutableStateFlow("")

    // Calendar state
    val selectedCalendarDate = MutableStateFlow(DateTimeUtils.getStartOfDay())

    private data class FilterState(
        val tab: TaskTab,
        val catFilter: Long?,
        val priFilter: Priority?,
        val sort: SortOption,
        val query: String
    )

    private val filterStateFlow: Flow<FilterState> = combine(
        selectedTab,
        selectedCategoryFilter,
        selectedPriorityFilter,
        selectedSortOption,
        searchQuery
    ) { tab, catFilter, priFilter, sort, query ->
        FilterState(tab, catFilter, priFilter, sort, query)
    }

    // Filtered Tasks for Tasks Screen
    val filteredTasks: StateFlow<List<TaskWithDetails>> = combine(
        allActiveTasks,
        filterStateFlow
    ) { tasks, filter ->
        var list = tasks

        // Search query filter
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            list = list.filter {
                it.task.title.lowercase().contains(q) ||
                        (it.task.description?.lowercase()?.contains(q) == true) ||
                        (it.task.notes?.lowercase()?.contains(q) == true) ||
                        (it.category?.name?.lowercase()?.contains(q) == true) ||
                        it.tags.any { tag -> tag.name.lowercase().contains(q) }
            }
        }

        // Tab filter
        list = when (filter.tab) {
            TaskTab.ALL -> list
            TaskTab.TODAY -> list.filter {
                DateTimeUtils.isToday(it.task.dueDate)
            }
            TaskTab.UPCOMING -> list.filter {
                val due = it.task.dueDate
                due != null && due > DateTimeUtils.getEndOfDay() && !it.task.isCompleted
            }
            TaskTab.OVERDUE -> list.filter {
                DateTimeUtils.isOverdue(it.task.dueDate, it.task.isCompleted)
            }
            TaskTab.COMPLETED -> list.filter { it.task.isCompleted }
        }

        // Category filter
        if (filter.catFilter != null) {
            list = list.filter { it.task.categoryId == filter.catFilter }
        }

        // Priority filter
        if (filter.priFilter != null) {
            list = list.filter { it.task.priority == filter.priFilter }
        }

        // Sorting
        when (filter.sort) {
            SortOption.DUE_DATE -> list.sortedWith(
                compareBy<TaskWithDetails> { it.task.isCompleted }
                    .thenBy { it.task.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.task.priority.level }
            )
            SortOption.PRIORITY -> list.sortedWith(
                compareBy<TaskWithDetails> { it.task.isCompleted }
                    .thenByDescending { it.task.priority.level }
                    .thenBy { it.task.dueDate ?: Long.MAX_VALUE }
            )
            SortOption.TITLE -> list.sortedWith(
                compareBy<TaskWithDetails> { it.task.isCompleted }
                    .thenBy { it.task.title.lowercase() }
            )
            SortOption.CREATED -> list.sortedWith(
                compareBy<TaskWithDetails> { it.task.isCompleted }
                    .thenByDescending { it.task.createdAt }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics Period
    val statsPeriod = MutableStateFlow(StatsPeriod.WEEK)

    // Productivity Statistics calculation
    val productivityStats: StateFlow<ProductivityStats> = combine(
        allActiveTasks,
        archivedTasks,
        statsPeriod
    ) { activeTasks, archived, period ->
        val allTasks = activeTasks + archived
        val now = System.currentTimeMillis()

        val periodStartTime = when (period) {
            StatsPeriod.TODAY -> DateTimeUtils.getStartOfDay()
            StatsPeriod.WEEK -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            StatsPeriod.MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            StatsPeriod.ALL_TIME -> 0L
        }

        val filteredPeriodTasks = if (periodStartTime == 0L) {
            allTasks
        } else {
            allTasks.filter { it.task.createdAt >= periodStartTime || (it.task.completedAt ?: 0L) >= periodStartTime }
        }

        val totalCreated = filteredPeriodTasks.size
        val completedList = filteredPeriodTasks.filter { it.task.isCompleted }
        val totalCompleted = completedList.size
        val completionRate = if (totalCreated > 0) totalCompleted.toFloat() / totalCreated.toFloat() else 0f
        val overdueCount = allTasks.count { DateTimeUtils.isOverdue(it.task.dueDate, it.task.isCompleted) }

        // Priority breakdown
        val priorityMap = Priority.entries.associateWith { p ->
            allTasks.count { it.task.priority == p }
        }

        // Category breakdown
        val categoryMap = mutableMapOf<String, Int>()
        for (item in allTasks) {
            val name = item.category?.name ?: "Uncategorized"
            categoryMap[name] = (categoryMap[name] ?: 0) + 1
        }

        // Streak calculation: count consecutive days backwards from today where at least one task was completed
        var streak = 0
        val cal = Calendar.getInstance()
        var checkDayStart = DateTimeUtils.getStartOfDay()

        // Check today first
        val completedDaysTimestamps = allTasks.mapNotNull { it.task.completedAt }
            .map { DateTimeUtils.getStartOfDay(it) }
            .toSet()

        if (completedDaysTimestamps.contains(checkDayStart)) {
            streak++
            checkDayStart -= 24 * 60 * 60 * 1000L
        } else {
            // Check yesterday
            val yesterdayStart = checkDayStart - 24 * 60 * 60 * 1000L
            if (completedDaysTimestamps.contains(yesterdayStart)) {
                streak++
                checkDayStart = yesterdayStart - 24 * 60 * 60 * 1000L
            }
        }

        while (completedDaysTimestamps.contains(checkDayStart) && streak < 365) {
            streak++
            checkDayStart -= 24 * 60 * 60 * 1000L
        }

        // Past 7 days completions for chart
        val last7Days = mutableListOf<Pair<String, Int>>()
        val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dStart = DateTimeUtils.getStartOfDay(dayCal.timeInMillis)
            val dEnd = DateTimeUtils.getEndOfDay(dayCal.timeInMillis)
            val count = allTasks.count {
                val cAt = it.task.completedAt
                cAt != null && cAt in dStart..dEnd
            }
            last7Days.add(dayFormat.format(dayCal.time) to count)
        }

        ProductivityStats(
            totalCreated = totalCreated,
            totalCompleted = totalCompleted,
            completionRate = completionRate,
            overdueCount = overdueCount,
            streakDays = streak,
            priorityCounts = priorityMap,
            categoryCounts = categoryMap,
            dailyCompletions = last7Days
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductivityStats())

    // Last deleted task for Undo Snackbar
    val lastDeletedTask = MutableStateFlow<TaskWithDetails?>(null)

    // User Actions
    fun createTask(
        title: String,
        description: String? = null,
        dueDate: Long? = null,
        dueTime: String? = null,
        priority: Priority = Priority.NONE,
        categoryId: Long? = null,
        notes: String? = null,
        reminderEnabled: Boolean = false,
        reminderTime: Long? = null,
        repeatType: RepeatType = RepeatType.NONE,
        repeatInterval: Int = 1,
        repeatEndDate: Long? = null,
        tagNames: List<String> = emptyList(),
        subtaskTitles: List<String> = emptyList()
    ) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val tagIds = tagNames.map { tagRepository.getOrCreateTag(it) }
            val task = TaskEntity(
                title = title.trim(),
                description = description?.takeIf { it.isNotBlank() },
                dueDate = dueDate,
                dueTime = dueTime?.takeIf { it.isNotBlank() },
                priority = priority,
                categoryId = categoryId,
                notes = notes?.takeIf { it.isNotBlank() },
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                repeatType = repeatType,
                repeatInterval = repeatInterval,
                repeatEndDate = repeatEndDate
            )
            taskRepository.createTask(task, tagIds, subtaskTitles)
        }
    }

    fun quickAddTask(title: String, dueDate: Long? = null, dueTime: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = userPreferences.value
            val defaultPri = Priority.fromString(prefs.defaultPriority)
            val defaultCat = if (prefs.defaultCategoryId > 0) prefs.defaultCategoryId else null

            val task = TaskEntity(
                title = title.trim(),
                dueDate = dueDate ?: DateTimeUtils.getStartOfDay(),
                dueTime = dueTime,
                priority = defaultPri,
                categoryId = defaultCat
            )
            taskRepository.createTask(task)
        }
    }

    fun updateTask(
        task: TaskEntity,
        tagNames: List<String>? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tagIds = tagNames?.map { tagRepository.getOrCreateTag(it) }
            taskRepository.updateTask(task, tagIds)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.setTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun archiveTask(taskId: Long, archived: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.setTaskArchived(taskId, archived)
        }
    }

    fun deleteTask(taskWithDetails: TaskWithDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            lastDeletedTask.value = taskWithDetails
            taskRepository.deleteTask(taskWithDetails.task.id)
        }
    }

    fun undoDelete() {
        val deleted = lastDeletedTask.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val tagIds = deleted.tags.map { it.id }
            val subtasks = deleted.subtasks.map { it.title }
            taskRepository.createTask(
                task = deleted.task.copy(id = 0),
                tagIds = tagIds,
                initialSubtasks = subtasks
            )
            lastDeletedTask.value = null
        }
    }

    fun duplicateTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.duplicateTask(taskId)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.clearCompletedTasks()
        }
    }

    // Subtask actions
    fun addSubtask(taskId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.addSubtask(taskId, title)
        }
    }

    fun toggleSubtaskCompletion(subtaskId: Long, isCompleted: Boolean, parentTaskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.setSubtaskCompleted(subtaskId, isCompleted, parentTaskId)
        }
    }

    fun deleteSubtask(subtaskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteSubtask(subtaskId)
        }
    }

    // Category actions
    fun createCategory(name: String, colorHex: String, iconName: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.createCategory(name, colorHex, iconName)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.deleteCategory(categoryId)
        }
    }

    // Preferences actions
    fun setThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setDefaultPriority(priority: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.setDefaultPriority(priority)
        }
    }

    fun setDefaultCategoryId(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.setDefaultCategoryId(id)
        }
    }

    fun setAutoCompleteParent(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.setAutoCompleteParent(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setShowStreak(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.setShowStreak(show)
        }
    }

    fun setShowAds(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.setShowAds(show)
        }
    }

    // Sample data generator for development/testing
    fun loadSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            val today = DateTimeUtils.getStartOfDay()
            val tomorrow = today + 24 * 60 * 60 * 1000L
            val yesterday = today - 24 * 60 * 60 * 1000L

            val sampleTasks = listOf(
                Pair(
                    TaskEntity(
                        title = "Pay electricity bill",
                        description = "Online via utility portal before 8 PM",
                        dueDate = today,
                        dueTime = "19:00",
                        priority = Priority.URGENT,
                        categoryId = 5 // Finance
                    ),
                    listOf("Review amount", "Pay online", "Download receipt")
                ),
                Pair(
                    TaskEntity(
                        title = "Complete project roadmap",
                        description = "Finalize Q4 milestones and review deliverables with team",
                        dueDate = today,
                        priority = Priority.HIGH,
                        categoryId = 2 // Work
                    ),
                    listOf("Draft key objectives", "Estimate timelines", "Share with engineering")
                ),
                Pair(
                    TaskEntity(
                        title = "Weekly grocery shopping",
                        description = "Organic fruits, oats, almond milk, coffee beans",
                        dueDate = tomorrow,
                        priority = Priority.MEDIUM,
                        categoryId = 3, // Shopping
                        repeatType = RepeatType.WEEKLY
                    ),
                    listOf("Apples & Bananas", "Almond milk", "Cold brew beans", "Spinach")
                ),
                Pair(
                    TaskEntity(
                        title = "Evening 5km run",
                        description = "Pacing 5:30/km in city park",
                        dueDate = today,
                        priority = Priority.MEDIUM,
                        categoryId = 4 // Health
                    ),
                    emptyList()
                ),
                Pair(
                    TaskEntity(
                        title = "Renew car insurance policy",
                        description = "Check policy comparison on broker website",
                        dueDate = yesterday,
                        priority = Priority.HIGH,
                        categoryId = 5 // Finance
                    ),
                    emptyList()
                ),
                Pair(
                    TaskEntity(
                        title = "Clean and organize kitchen",
                        description = "Wipe counters and clean refrigerator shelves",
                        dueDate = today,
                        isCompleted = true,
                        completedAt = today + 3600000L,
                        priority = Priority.LOW,
                        categoryId = 6 // Home
                    ),
                    listOf("Countertops", "Fridge", "Sink")
                )
            )

            for ((t, subtasks) in sampleTasks) {
                taskRepository.createTask(t, emptyList(), subtasks)
            }
        }
    }
}

class TaskFlowViewModelFactory(
    private val app: com.example.TaskFlowApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaskFlowViewModel(
            taskRepository = app.taskRepository,
            categoryRepository = app.categoryRepository,
            tagRepository = app.tagRepository,
            userPreferencesRepository = app.userPreferencesRepository,
            backupManager = app.backupManager,
            adManager = app.adManager
        ) as T
    }
}
