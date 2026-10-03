package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TaskWithDetails
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TaskItemCard
import com.example.ui.viewmodel.TaskFlowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: TaskFlowViewModel,
    onNavigateBack: () -> Unit,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val allActiveTasks by viewModel.allActiveTasks.collectAsState()
    val focusRequester = remember { FocusRequester() }

    val searchResults = remember(allActiveTasks, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase()
            allActiveTasks.filter {
                it.task.title.lowercase().contains(q) ||
                        (it.task.description?.lowercase()?.contains(q) == true) ||
                        (it.task.notes?.lowercase()?.contains(q) == true) ||
                        (it.category?.name?.lowercase()?.contains(q) == true) ||
                        it.tags.any { tag -> tag.name.lowercase().contains(q) }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search title, notes, tags...") },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (searchQuery.isBlank()) {
            EmptyStateView(
                icon = Icons.Outlined.Search,
                title = "Search Tasks",
                message = "Type above to search across titles, descriptions, categories, and tags.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (searchResults.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Search,
                title = "No results found",
                message = "No tasks matched '$searchQuery'. Try different keywords.",
                actionLabel = "Clear",
                onActionClick = { searchQuery = "" },
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults, key = { it.task.id }) { item ->
                    TaskItemCard(
                        taskWithDetails = item,
                        onToggleComplete = { viewModel.toggleTaskCompletion(item.task) },
                        onClick = { onOpenTaskDetail(item) },
                        onDelete = { viewModel.deleteTask(item) }
                    )
                }
            }
        }
    }
}
