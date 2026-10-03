package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.BannerAdView
import com.example.data.local.entity.TaskWithDetails
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProductivityProgressCard
import com.example.ui.components.QuickAddBar
import com.example.ui.components.TaskItemCard
import com.example.ui.theme.PriorityUrgent
import com.example.ui.viewmodel.TaskFlowViewModel
import com.example.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: TaskFlowViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    onOpenAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allActiveTasks by viewModel.allActiveTasks.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState()
    val productivityStats by viewModel.productivityStats.collectAsState()

    // Categorize tasks for Home Screen
    val overdueTasks = remember(allActiveTasks) {
        allActiveTasks.filter { DateTimeUtils.isOverdue(it.task.dueDate, it.task.isCompleted) }
    }
    val todayTasks = remember(allActiveTasks) {
        allActiveTasks.filter { DateTimeUtils.isToday(it.task.dueDate) && !it.task.isCompleted }
    }
    val todayCompletedTasks = remember(allActiveTasks) {
        allActiveTasks.filter { it.task.isCompleted && DateTimeUtils.isToday(it.task.completedAt) }
    }
    val upcomingTasks = remember(allActiveTasks) {
        allActiveTasks.filter {
            val due = it.task.dueDate
            due != null && due > DateTimeUtils.getEndOfDay() && !it.task.isCompleted
        }.take(4)
    }

    val totalTodayCount = todayTasks.size + todayCompletedTasks.size
    val completedTodayCount = todayCompletedTasks.size

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val dateFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddTask,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Task") },
                text = { Text("Add Task") },
                modifier = Modifier.testTag("home_fab_add_task")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Greeting, Date, Search action
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Productivity Summary Progress Ring & Streak
            item {
                ProductivityProgressCard(
                    totalToday = totalTodayCount,
                    completedToday = completedTodayCount,
                    streakDays = productivityStats.streakDays,
                    showStreak = preferences.showStreak
                )
            }

            // Quick Add Input Bar
            item {
                QuickAddBar(
                    onAddTask = { title, dueDate ->
                        viewModel.quickAddTask(title = title, dueDate = dueDate)
                    }
                )
            }

            // OVERDUE TASKS SECTION (if any)
            if (overdueTasks.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = PriorityUrgent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Overdue (${overdueTasks.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PriorityUrgent
                        )
                    }
                }

                items(overdueTasks, key = { "overdue_${it.task.id}" }) { item ->
                    TaskItemCard(
                        taskWithDetails = item,
                        onToggleComplete = { viewModel.toggleTaskCompletion(item.task) },
                        onClick = { onOpenTaskDetail(item) },
                        onDelete = { viewModel.deleteTask(item) }
                    )
                }
            }

            // TODAY TASKS SECTION
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "Today's Tasks (${todayTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (todayTasks.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.CheckCircle,
                        title = "No tasks for today",
                        message = "You're all caught up or haven't scheduled any tasks for today.",
                        actionLabel = "Add a Task",
                        onActionClick = onOpenAddTask
                    )
                }
            } else {
                items(todayTasks, key = { "today_${it.task.id}" }) { item ->
                    TaskItemCard(
                        taskWithDetails = item,
                        onToggleComplete = { viewModel.toggleTaskCompletion(item.task) },
                        onClick = { onOpenTaskDetail(item) },
                        onDelete = { viewModel.deleteTask(item) }
                    )
                }
            }

            // UPCOMING TASKS PREVIEW
            if (upcomingTasks.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Upcoming",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToTasks() }
                        )
                    }
                }

                items(upcomingTasks, key = { "upcoming_${it.task.id}" }) { item ->
                    TaskItemCard(
                        taskWithDetails = item,
                        onToggleComplete = { viewModel.toggleTaskCompletion(item.task) },
                        onClick = { onOpenTaskDetail(item) },
                        onDelete = { viewModel.deleteTask(item) }
                    )
                }
            }

            // TODAY'S COMPLETED SECTION (if any)
            if (todayCompletedTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed Today (${todayCompletedTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(todayCompletedTasks, key = { "completed_${it.task.id}" }) { item ->
                    TaskItemCard(
                        taskWithDetails = item,
                        onToggleComplete = { viewModel.toggleTaskCompletion(item.task) },
                        onClick = { onOpenTaskDetail(item) },
                        onDelete = { viewModel.deleteTask(item) }
                    )
                }
            }

            // Non-intrusive Banner Ad at bottom
            item {
                BannerAdView(adManager = viewModel.adManager, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
