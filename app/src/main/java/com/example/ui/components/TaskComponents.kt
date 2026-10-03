package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Priority
import com.example.data.local.entity.RepeatType
import com.example.data.local.entity.TaskWithDetails
import com.example.ui.theme.*
import com.example.utils.DateTimeUtils

fun getCategoryColor(colorHex: String?): Color {
    if (colorHex.isNullOrBlank()) return Color(0xFF64748B)
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF64748B)
    }
}

fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.URGENT -> PriorityUrgent
        Priority.HIGH -> PriorityHigh
        Priority.MEDIUM -> PriorityMedium
        Priority.LOW -> PriorityLow
        Priority.NONE -> PriorityNone
    }
}

fun getPriorityIcon(priority: Priority): ImageVector {
    return when (priority) {
        Priority.URGENT -> Icons.Filled.PriorityHigh
        Priority.HIGH -> Icons.Filled.KeyboardDoubleArrowUp
        Priority.MEDIUM -> Icons.Filled.KeyboardArrowUp
        Priority.LOW -> Icons.Filled.KeyboardArrowDown
        Priority.NONE -> Icons.Outlined.Remove
    }
}

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    if (priority == Priority.NONE) return

    val color = getPriorityColor(priority)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = getPriorityIcon(priority),
                contentDescription = "${priority.title} priority",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = priority.title,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CategoryPill(name: String, colorHex: String?, modifier: Modifier = Modifier) {
    val color = getCategoryColor(colorHex)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = name,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItemCard(
    taskWithDetails: TaskWithDetails,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val task = taskWithDetails.task
    val isOverdue = DateTimeUtils.isOverdue(task.dueDate, task.isCompleted)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onToggleComplete()
                false // Reset so item stays or reflects updated state
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF10B981) // Complete (Emerald)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444) // Delete (Red)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                animationSpec = tween(durationMillis = 200),
                label = "swipeColor"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete task",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (task.isCompleted) "Mark Active" else "Complete",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Delete",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete task",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 1.5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onClick)
                .testTag("task_item_${task.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Custom Checkbox button with minimum touch target 48dp
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("checkbox_${task.id}")
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Task completed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = "Mark task completed",
                                tint = if (isOverdue) PriorityUrgent else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!task.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (task.repeatType != RepeatType.NONE) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "Recurring: ${task.repeatType.title}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 4.dp)
                        )
                    }

                    if (task.reminderEnabled) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Reminder enabled",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }

                // Subtasks summary
                if (taskWithDetails.subtasks.isNotEmpty()) {
                    val completedSubtasks = taskWithDetails.subtasks.count { it.isCompleted }
                    val totalSubtasks = taskWithDetails.subtasks.size
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 44.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { completedSubtasks.toFloat() / totalSubtasks.toFloat() },
                            modifier = Modifier
                                .width(80.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$completedSubtasks/$totalSubtasks subtasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Metadata row (Date, Category, Priority, Tags)
                val hasMetadata = task.dueDate != null || taskWithDetails.category != null || task.priority != Priority.NONE || taskWithDetails.tags.isNotEmpty()
                if (hasMetadata) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 44.dp)
                    ) {
                        // Due date indicator
                        if (task.dueDate != null) {
                            val dateText = DateTimeUtils.formatRelativeDate(task.dueDate)
                            val dateColor = if (isOverdue) PriorityUrgent else MaterialTheme.colorScheme.onSurfaceVariant
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOverdue) Icons.Filled.Warning else Icons.Outlined.CalendarToday,
                                    contentDescription = "Due date",
                                    tint = dateColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (!task.dueTime.isNullOrBlank()) "$dateText ${task.dueTime}" else dateText,
                                    color = dateColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        // Category
                        taskWithDetails.category?.let { cat ->
                            CategoryPill(name = cat.name, colorHex = cat.colorHex)
                        }

                        // Priority
                        if (task.priority != Priority.NONE) {
                            PriorityBadge(priority = task.priority)
                        }

                        // First tag if any
                        if (taskWithDetails.tags.isNotEmpty()) {
                            Text(
                                text = "#${taskWithDetails.tags.first().name}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
