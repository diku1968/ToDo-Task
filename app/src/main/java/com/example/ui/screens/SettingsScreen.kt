package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Priority
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.CategoryManagerDialog
import com.example.ui.viewmodel.TaskFlowViewModel

@Composable
fun SettingsScreen(
    viewModel: TaskFlowViewModel,
    onNavigateToArchived: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences by viewModel.userPreferences.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Screen Title
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Section: Appearance
        item {
            SettingsSection(title = "Appearance") {
                SettingsRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    subtitle = when (preferences.themeMode) {
                        "DARK" -> "Dark"
                        "LIGHT" -> "Light"
                        else -> "System Default"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
        }

        // Section: Tasks & Categories
        item {
            SettingsSection(title = "Tasks & Categories") {
                SettingsRow(
                    icon = Icons.Outlined.Category,
                    title = "Manage Categories",
                    subtitle = "${categories.size} categories configured",
                    onClick = { showCategoryDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    icon = Icons.Outlined.Archive,
                    title = "Archived Tasks",
                    subtitle = "View and restore archived tasks",
                    onClick = onNavigateToArchived
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsToggleRow(
                    icon = Icons.Outlined.Checklist,
                    title = "Auto-Complete Parent Task",
                    subtitle = "Mark task done when all subtasks are finished",
                    checked = preferences.autoCompleteParent,
                    onCheckedChange = { viewModel.setAutoCompleteParent(it) }
                )
            }
        }

        // Section: Notifications
        item {
            SettingsSection(title = "Notifications & Reminders") {
                SettingsToggleRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Enable Reminders",
                    subtitle = "Allow alarms and reminders to notify you",
                    checked = preferences.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsToggleRow(
                    icon = Icons.Outlined.LocalFireDepartment,
                    title = "Show Productivity Streak",
                    subtitle = "Display streak badge on home and stats",
                    checked = preferences.showStreak,
                    onCheckedChange = { viewModel.setShowStreak(it) }
                )
            }
        }

        // Section: Data & Backup
        item {
            SettingsSection(title = "Data & Storage") {
                SettingsRow(
                    icon = Icons.Outlined.CloudSync,
                    title = "Backup & Restore",
                    subtitle = "Export JSON, restore backup, or export CSV",
                    onClick = { showBackupDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    icon = Icons.Outlined.DeleteSweep,
                    title = "Clear Completed Tasks",
                    subtitle = "Permanently remove all completed tasks",
                    onClick = { showClearConfirm = true }
                )
            }
        }

        // Section: Monetization & Privacy
        item {
            SettingsSection(title = "Privacy & Monetization") {
                SettingsToggleRow(
                    icon = Icons.Outlined.AdUnits,
                    title = "Support with Ads",
                    subtitle = "Display non-intrusive banner ads",
                    checked = preferences.showAds,
                    onCheckedChange = { viewModel.setShowAds(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy & Data Security",
                    subtitle = "Offline-first. Your tasks never leave your device.",
                    onClick = { showPrivacyDialog = true }
                )
            }
        }

        // Section: Testing & Development Tools
        item {
            SettingsSection(title = "Developer Tools") {
                SettingsRow(
                    icon = Icons.Outlined.Dataset,
                    title = "Load Sample Tasks",
                    subtitle = "Populate database with example productivity tasks",
                    onClick = {
                        viewModel.loadSampleData()
                        Toast.makeText(context, "Sample tasks loaded!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // About Footer
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "TaskFlow – Smart To-Do List",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Version 1.0 • Offline-First Native Android",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Dialogs
    if (showCategoryDialog) {
        CategoryManagerDialog(
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onCreateCategory = { name, color, icon -> viewModel.createCategory(name, color, icon) },
            onDeleteCategory = { id -> viewModel.deleteCategory(id) }
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            backupManager = viewModel.backupManager,
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Completed Tasks?") },
            text = { Text("Are you sure you want to permanently delete all completed tasks? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCompletedTasks()
                        showClearConfirm = false
                        Toast.makeText(context, "Completed tasks cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    listOf("SYSTEM" to "System Default", "LIGHT" to "Light Theme", "DARK" to "Dark Theme").forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy & Security") },
            text = {
                Column {
                    Text(
                        text = "TaskFlow is built strictly offline-first.\n\n" +
                                "• Your task titles, notes, and checklists are stored purely on your local device in a secure SQLite database.\n" +
                                "• No accounts or logins are required to manage your tasks.\n" +
                                "• No task data is ever uploaded or synced to external cloud servers.\n" +
                                "• Network connectivity is only used for displaying Google AdMob monetization.\n" +
                                "• You maintain 100% ownership and control of your data via the built-in JSON and CSV export tools.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
