package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.BackupManager
import com.example.data.local.entity.Priority
import com.example.data.local.entity.RepeatType
import com.example.data.local.entity.TaskWithDetails
import com.example.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    taskWithDetails: TaskWithDetails,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleComplete: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleSubtask: (subtaskId: Long, isCompleted: Boolean) -> Unit,
    onAddSubtask: (title: String) -> Unit,
    onDeleteSubtask: (subtaskId: Long) -> Unit
) {
    val context = LocalContext.current
    val task = taskWithDetails.task
    var newSubtaskText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Action bar: Close, Share, Duplicate, Archive, Edit, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { BackupManager.shareTask(context, taskWithDetails) },
                        modifier = Modifier.testTag("share_task_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share Task")
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Duplicate")
                    }
                    IconButton(onClick = onArchive) {
                        Icon(
                            imageVector = if (task.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                            contentDescription = if (task.archived) "Unarchive" else "Archive"
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_task_button")) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_task_button")) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completion + Title
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Mark Complete",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!task.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (task.priority != Priority.NONE) {
                    PriorityBadge(priority = task.priority)
                }

                taskWithDetails.category?.let { cat ->
                    CategoryPill(name = cat.name, colorHex = cat.colorHex)
                }

                taskWithDetails.tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "#${tag.name}",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Due Date & Reminders Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (task.dueDate != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Due: ${DateTimeUtils.formatDate(task.dueDate)}" +
                                        if (!task.dueTime.isNullOrBlank()) " at ${task.dueTime}" else "",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    if (task.reminderEnabled && task.reminderTime != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Reminder: ${DateTimeUtils.formatDate(task.reminderTime)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (task.repeatType != RepeatType.NONE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Repeat: ${task.repeatType.title}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Subtasks checklist section
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Checklist (${taskWithDetails.subtasks.count { it.isCompleted }}/${taskWithDetails.subtasks.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            taskWithDetails.subtasks.forEach { sub ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = sub.isCompleted,
                        onCheckedChange = { onToggleSubtask(sub.id, it) },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = sub.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onDeleteSubtask(sub.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove subtask",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Add subtask input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add subtask...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            onAddSubtask(newSubtaskText.trim())
                            newSubtaskText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add subtask")
                }
            }

            // Notes section
            if (!task.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Timestamp footer
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Created ${DateTimeUtils.formatDate(task.createdAt)}" +
                        if (task.completedAt != null) " • Completed ${DateTimeUtils.formatDate(task.completedAt)}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
