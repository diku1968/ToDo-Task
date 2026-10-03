package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TaskWithDetails
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.TaskFlowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(
    viewModel: TaskFlowViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val archivedTasks by viewModel.archivedTasks.collectAsState()
    var taskToDeletePermanently by remember { mutableStateOf<TaskWithDetails?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archived Tasks (${archivedTasks.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (archivedTasks.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Archive,
                title = "No Archived Tasks",
                message = "Tasks you archive will appear here safely out of your main view.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(archivedTasks, key = { it.task.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.task.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                if (!item.task.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Restore button
                            IconButton(onClick = { viewModel.archiveTask(item.task.id, archived = false) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Restore,
                                    contentDescription = "Restore task",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Permanent Delete button
                            IconButton(onClick = { taskToDeletePermanently = item }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteForever,
                                    contentDescription = "Permanently delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation for permanent delete
    taskToDeletePermanently?.let { item ->
        AlertDialog(
            onDismissRequest = { taskToDeletePermanently = null },
            title = { Text("Delete Permanently?") },
            text = { Text("Are you sure you want to permanently delete '${item.task.title}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(item)
                        taskToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDeletePermanently = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
