package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.BackupManager
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.launch

val categoryPalette = listOf(
    "#4F46E5", // Indigo
    "#0284C7", // Sky
    "#059669", // Emerald
    "#D97706", // Amber
    "#DC2626", // Red
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#14B8A6", // Teal
    "#64748B"  // Slate
)

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onCreateCategory: (name: String, colorHex: String, iconName: String) -> Unit,
    onDeleteCategory: (categoryId: Long) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(categoryPalette.first()) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // New Category Input
                Text("Add New Category", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Category name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreateCategory(newName.trim(), selectedColor, "Folder")
                                newName = ""
                            }
                        },
                        enabled = newName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Color selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categoryPalette.forEach { hex ->
                        val color = getCategoryColor(hex)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Existing Categories List
                Text("Existing Categories", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(categories) { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(getCategoryColor(cat.colorHex))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { categoryToDelete = cat },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete category",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    // Confirm deletion dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category?") },
            text = {
                Text("Are you sure you want to delete '${cat.name}'? Existing tasks in this category will not be deleted; they will be moved to 'Uncategorized'.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BackupRestoreDialog(
    backupManager: BackupManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var importJsonText by remember { mutableStateOf("") }
    var showImportInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup & Restore Data") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Export your tasks, categories, and subtasks to a JSON file or share directly via email or drive.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Export JSON
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch {
                            val json = backupManager.exportToJson()
                            BackupManager.shareTextContent(
                                context,
                                subject = "TaskFlow Backup",
                                content = json,
                                mimeType = "application/json"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Backup (JSON)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Export CSV
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch {
                            val csv = backupManager.exportToCsv()
                            BackupManager.shareTextContent(
                                context,
                                subject = "TaskFlow Tasks Export",
                                content = csv,
                                mimeType = "text/csv"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Spreadsheet (CSV)")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (!showImportInput) {
                    OutlinedButton(
                        onClick = { showImportInput = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore from JSON Backup")
                    }
                } else {
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Paste JSON Backup here") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank()) {
                                coroutineScope.launch {
                                    val result = backupManager.importFromJson(importJsonText)
                                    result.onSuccess { count ->
                                        Toast.makeText(context, "Successfully restored $count tasks!", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Failed to restore: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = importJsonText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Restore")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
