package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.TaskWithDetails
import com.example.ui.components.TaskDetailDialog
import com.example.ui.components.TaskEditSheet
import com.example.ui.screens.*
import com.example.ui.theme.TaskFlowTheme
import com.example.ui.viewmodel.TaskFlowViewModel
import com.example.ui.viewmodel.TaskFlowViewModelFactory
import kotlinx.coroutines.launch

enum class Screen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    TASKS("Tasks", Icons.Filled.Checklist, Icons.Outlined.Checklist),
    CALENDAR("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    STATISTICS("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search),
    ARCHIVED("Archived", Icons.Filled.Archive, Icons.Outlined.Archive)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TaskFlowApplication
        val initialTaskId = intent?.getLongExtra("EXTRA_TASK_ID", -1L) ?: -1L

        setContent {
            val viewModel: TaskFlowViewModel = viewModel(factory = TaskFlowViewModelFactory(app))
            val preferences by viewModel.userPreferences.collectAsState()

            // Notification permission request for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Handled gracefully, settings toggle reflects preference
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            TaskFlowTheme(themeMode = preferences.themeMode) {
                TaskFlowApp(
                    viewModel = viewModel,
                    initialTaskId = initialTaskId
                )
            }
        }
    }
}

@Composable
fun TaskFlowApp(
    viewModel: TaskFlowViewModel,
    initialTaskId: Long = -1L
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Dialog & Sheet States
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskWithDetails?>(null) }
    var taskDetailToShow by remember { mutableStateOf<TaskWithDetails?>(null) }
    var initialDateForNewTask by remember { mutableStateOf<Long?>(null) }

    val categories by viewModel.categories.collectAsState()
    val allActiveTasks by viewModel.allActiveTasks.collectAsState()

    // Handle open from Notification
    LaunchedEffect(initialTaskId, allActiveTasks) {
        if (initialTaskId != -1L && taskDetailToShow == null) {
            val matching = allActiveTasks.find { it.task.id == initialTaskId }
            if (matching != null) {
                taskDetailToShow = matching
            }
        }
    }

    // Observe deleted tasks to show Undo Snackbar
    val lastDeleted by viewModel.lastDeletedTask.collectAsState()
    LaunchedEffect(lastDeleted) {
        if (lastDeleted != null) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Task deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                } else {
                    viewModel.lastDeletedTask.value = null
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (currentScreen != Screen.SEARCH && currentScreen != Screen.ARCHIVED) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    listOf(
                        Screen.HOME,
                        Screen.TASKS,
                        Screen.CALENDAR,
                        Screen.STATISTICS,
                        Screen.SETTINGS
                    ).forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { currentScreen = Screen.TASKS },
                    onNavigateToSearch = { currentScreen = Screen.SEARCH },
                    onOpenTaskDetail = { taskDetailToShow = it },
                    onOpenAddTask = {
                        initialDateForNewTask = null
                        showAddTaskSheet = true
                    }
                )
                Screen.TASKS -> TasksScreen(
                    viewModel = viewModel,
                    onOpenTaskDetail = { taskDetailToShow = it },
                    onOpenAddTask = {
                        initialDateForNewTask = null
                        showAddTaskSheet = true
                    }
                )
                Screen.CALENDAR -> CalendarScreen(
                    viewModel = viewModel,
                    onOpenTaskDetail = { taskDetailToShow = it },
                    onOpenAddTaskForDate = { dateMillis ->
                        initialDateForNewTask = dateMillis
                        showAddTaskSheet = true
                    }
                )
                Screen.STATISTICS -> StatisticsScreen(
                    viewModel = viewModel
                )
                Screen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToArchived = { currentScreen = Screen.ARCHIVED }
                )
                Screen.SEARCH -> SearchScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.HOME },
                    onOpenTaskDetail = { taskDetailToShow = it }
                )
                Screen.ARCHIVED -> ArchivedScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.SETTINGS }
                )
            }
        }
    }

    // Add / Edit Task Modal Sheet
    if (showAddTaskSheet || taskToEdit != null) {
        val editingTask = taskToEdit
        TaskEditSheet(
            initialTask = editingTask,
            categories = categories,
            onDismiss = {
                showAddTaskSheet = false
                taskToEdit = null
            },
            onSave = { title, desc, dueDate, dueTime, priority, catId, notes, reminderEnabled, reminderTime, repeatType, repeatInterval, tags, subtasks ->
                if (editingTask != null) {
                    val updated = editingTask.task.copy(
                        title = title,
                        description = desc,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        priority = priority,
                        categoryId = catId,
                        notes = notes,
                        reminderEnabled = reminderEnabled,
                        reminderTime = reminderTime,
                        repeatType = repeatType,
                        repeatInterval = repeatInterval
                    )
                    viewModel.updateTask(updated, tags)
                } else {
                    viewModel.createTask(
                        title = title,
                        description = desc,
                        dueDate = dueDate ?: initialDateForNewTask,
                        dueTime = dueTime,
                        priority = priority,
                        categoryId = catId,
                        notes = notes,
                        reminderEnabled = reminderEnabled,
                        reminderTime = reminderTime,
                        repeatType = repeatType,
                        repeatInterval = repeatInterval,
                        tagNames = tags,
                        subtaskTitles = subtasks
                    )
                }
                showAddTaskSheet = false
                taskToEdit = null
            }
        )
    }

    // Task Detail Modal Dialog
    taskDetailToShow?.let { detail ->
        // Keep detail in sync if updated in DB
        val currentDetail = allActiveTasks.find { it.task.id == detail.task.id } ?: detail

        TaskDetailDialog(
            taskWithDetails = currentDetail,
            onDismiss = { taskDetailToShow = null },
            onEdit = {
                taskToEdit = currentDetail
                taskDetailToShow = null
            },
            onToggleComplete = {
                viewModel.toggleTaskCompletion(currentDetail.task)
            },
            onArchive = {
                viewModel.archiveTask(currentDetail.task.id, !currentDetail.task.archived)
                taskDetailToShow = null
            },
            onDelete = {
                viewModel.deleteTask(currentDetail)
                taskDetailToShow = null
            },
            onDuplicate = {
                viewModel.duplicateTask(currentDetail.task.id)
                taskDetailToShow = null
            },
            onToggleSubtask = { subtaskId, isCompleted ->
                viewModel.toggleSubtaskCompletion(subtaskId, isCompleted, currentDetail.task.id)
            },
            onAddSubtask = { title ->
                viewModel.addSubtask(currentDetail.task.id, title)
            },
            onDeleteSubtask = { subtaskId ->
                viewModel.deleteSubtask(subtaskId)
            }
        )
    }
}
