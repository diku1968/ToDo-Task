package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.BannerAdView
import com.example.data.local.entity.TaskWithDetails
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TaskItemCard
import com.example.ui.viewmodel.TaskFlowViewModel
import com.example.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: TaskFlowViewModel,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    onOpenAddTaskForDate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allActiveTasks by viewModel.allActiveTasks.collectAsState()

    var currentMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    var selectedDateMillis by remember {
        mutableStateOf(DateTimeUtils.getStartOfDay())
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayOfWeekFormat = remember { SimpleDateFormat("EE", Locale.getDefault()) }

    // Map each day's timestamp to task count
    val tasksByDate = remember(allActiveTasks) {
        val map = mutableMapOf<Long, Int>()
        for (item in allActiveTasks) {
            val due = item.task.dueDate
            if (due != null) {
                val start = DateTimeUtils.getStartOfDay(due)
                map[start] = (map[start] ?: 0) + 1
            }
        }
        map
    }

    // Tasks for selected day
    val selectedDayTasks = remember(allActiveTasks, selectedDateMillis) {
        val start = DateTimeUtils.getStartOfDay(selectedDateMillis)
        val end = DateTimeUtils.getEndOfDay(selectedDateMillis)
        allActiveTasks.filter {
            val due = it.task.dueDate
            due != null && due in start..end
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenAddTaskForDate(selectedDateMillis) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task for Date")
            }
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
            // Month Header & Navigation
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            val newCal = currentMonthCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentMonthCalendar = newCal
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    Text(
                        text = monthYearFormat.format(currentMonthCalendar.time),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = {
                            val newCal = currentMonthCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentMonthCalendar = newCal
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }

            // Calendar Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Weekday headers (Sun, Mon, Tue, etc.)
                        val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            weekdays.forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Day grid calculation
                        val cal = currentMonthCalendar.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                        val todayStart = DateTimeUtils.getStartOfDay()

                        var currentDay = 1
                        val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

                        for (week in 0 until (totalCells / 7)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                for (dayOfWeek in 0..6) {
                                    val cellIndex = week * 7 + dayOfWeek
                                    if (cellIndex < firstDayOfWeek || currentDay > daysInMonth) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        val dayNumber = currentDay
                                        val dayCal = currentMonthCalendar.clone() as Calendar
                                        dayCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                        val dayMillis = DateTimeUtils.getStartOfDay(dayCal.timeInMillis)

                                        val isSelected = DateTimeUtils.getStartOfDay(selectedDateMillis) == dayMillis
                                        val isToday = dayMillis == todayStart
                                        val taskCount = tasksByDate[dayMillis] ?: 0

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .clickable {
                                                    selectedDateMillis = dayMillis
                                                }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )

                                                if (taskCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                else MaterialTheme.colorScheme.secondary
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                        currentDay++
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tasks for selected day header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "Tasks for ${DateTimeUtils.formatRelativeDate(selectedDateMillis)} (${selectedDayTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tasks for selected day list
            if (selectedDayTasks.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.EventNote,
                        title = "No tasks for this day",
                        message = "Tap '+' to schedule a task for this date.",
                        actionLabel = "Schedule Task",
                        onActionClick = { onOpenAddTaskForDate(selectedDateMillis) }
                    )
                }
            } else {
                items(selectedDayTasks, key = { it.task.id }) { item ->
                    TaskItemCard(
                        taskWithDetails = item,
                        onToggleComplete = { viewModel.toggleTaskCompletion(item.task) },
                        onClick = { onOpenTaskDetail(item) },
                        onDelete = { viewModel.deleteTask(item) }
                    )
                }
            }

            item {
                BannerAdView(adManager = viewModel.adManager, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
