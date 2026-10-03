package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.utils.DateTimeUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditSheet(
    initialTask: TaskWithDetails? = null,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String?,
        dueDate: Long?,
        dueTime: String?,
        priority: Priority,
        categoryId: Long?,
        notes: String?,
        reminderEnabled: Boolean,
        reminderTime: Long?,
        repeatType: RepeatType,
        repeatInterval: Int,
        tagNames: List<String>,
        subtasks: List<String>
    ) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(initialTask?.task?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.task?.description ?: "") }
    var dueDate by remember { mutableStateOf(initialTask?.task?.dueDate) }
    var dueTime by remember { mutableStateOf(initialTask?.task?.dueTime ?: "") }
    var priority by remember { mutableStateOf(initialTask?.task?.priority ?: Priority.NONE) }
    var categoryId by remember { mutableStateOf(initialTask?.task?.categoryId) }
    var notes by remember { mutableStateOf(initialTask?.task?.notes ?: "") }
    var reminderEnabled by remember { mutableStateOf(initialTask?.task?.reminderEnabled ?: false) }
    var reminderTime by remember { mutableStateOf(initialTask?.task?.reminderTime) }
    var repeatType by remember { mutableStateOf(initialTask?.task?.repeatType ?: RepeatType.NONE) }
    var tagInput by remember { mutableStateOf(initialTask?.tags?.joinToString(", ") { it.name } ?: "") }

    // Subtasks
    var subtasks by remember {
        mutableStateOf<List<String>>(initialTask?.subtasks?.map { it.title } ?: emptyList())
    }
    var newSubtaskText by remember { mutableStateOf("") }

    val isEditing = initialTask != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEditing) "Edit Task" else "New Task",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                placeholder = { Text("What needs to be done?") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Add details or context...") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date & Time Row
            Text(
                text = "Due Date & Time",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Date Chip
                OutlinedCard(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val cal = Calendar.getInstance()
                            if (dueDate != null) cal.timeInMillis = dueDate!!
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    dueDate = newCal.timeInMillis
                                    // If reminder is enabled and no reminder time set, default to 9:00 AM of that day
                                    if (reminderEnabled && reminderTime == null) {
                                        reminderTime = dueDate!! + (9 * 3600 * 1000)
                                    }
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (dueDate != null) DateTimeUtils.formatDate(dueDate) else "Pick Date",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (dueDate != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear date",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { dueDate = null }
                            )
                        }
                    }
                }

                // Time Chip
                OutlinedCard(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val cal = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    dueTime = DateTimeUtils.formatTime(h, m)
                                    if (dueDate != null) {
                                        reminderTime = dueDate!! + (h * 3600 * 1000L) + (m * 60 * 1000L)
                                    }
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (dueTime.isNotBlank()) dueTime else "Pick Time",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (dueTime.isNotBlank()) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear time",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { dueTime = "" }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selector
            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(Priority.entries) { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        leadingIcon = {
                            Icon(
                                imageVector = getPriorityIcon(p),
                                contentDescription = null,
                                tint = getPriorityColor(p),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(p.title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        label = { Text("None") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(getCategoryColor(cat.colorHex), shape = RoundedCornerShape(2.dp))
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder & Recurrence Switches
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Reminder Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Reminder Notification", style = MaterialTheme.typography.bodyMedium)
                                if (reminderEnabled && reminderTime != null) {
                                    Text(
                                        text = DateTimeUtils.formatDate(reminderTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = {
                                reminderEnabled = it
                                if (it && reminderTime == null) {
                                    reminderTime = dueDate ?: (System.currentTimeMillis() + 3600000L)
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Repeat Recurrence Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Repeat", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.weight(1f))

                        var repeatMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { repeatMenuExpanded = true }) {
                                Text(repeatType.title)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = repeatMenuExpanded,
                                onDismissRequest = { repeatMenuExpanded = false }
                            ) {
                                RepeatType.entries.forEach { rt ->
                                    DropdownMenuItem(
                                        text = { Text(rt.title) },
                                        onClick = {
                                            repeatType = rt
                                            repeatMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtasks / Checklist
            Text(
                text = "Subtasks / Checklist",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            subtasks.forEachIndexed { index, sub ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            subtasks = subtasks.toMutableList().also { it.removeAt(index) }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove subtask",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add subtask item...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            subtasks = subtasks + newSubtaskText.trim()
                            newSubtaskText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add subtask")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tags Field
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = { Text("Tags") },
                placeholder = { Text("e.g. urgent, work, project (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Additional notes, links or thoughts...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val tagsList = tagInput.split(",")
                            .map { it.trim().removePrefix("#") }
                            .filter { it.isNotBlank() }

                        onSave(
                            title,
                            description.takeIf { it.isNotBlank() },
                            dueDate,
                            dueTime.takeIf { it.isNotBlank() },
                            priority,
                            categoryId,
                            notes.takeIf { it.isNotBlank() },
                            reminderEnabled,
                            reminderTime,
                            repeatType,
                            1,
                            tagsList,
                            subtasks
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_task_button")
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Create Task",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
