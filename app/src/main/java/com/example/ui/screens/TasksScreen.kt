package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.BannerAdView
import com.example.data.local.entity.Priority
import com.example.data.local.entity.TaskWithDetails
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TaskItemCard
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getPriorityColor
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.TaskFlowViewModel
import com.example.ui.viewmodel.TaskTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskFlowViewModel,
    onOpenTaskDetail: (TaskWithDetails) -> Unit,
    onOpenAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedPriority by viewModel.selectedPriorityFilter.collectAsState()
    val selectedSort by viewModel.selectedSortOption.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddTask,
                modifier = Modifier.testTag("tasks_fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Header & Search
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search tasks, notes, tags...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("task_search_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sort,
                            contentDescription = "Sort Options",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.title) },
                                leadingIcon = {
                                    if (selectedSort == sort) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.selectedSortOption.value = sort
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Tabs Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                TaskTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectedTab.value = tab },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Filters row (Categories & Priority)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Filter chips
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectedCategoryFilter.value = null },
                        label = { Text("All Categories", fontSize = 12.sp) }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.id,
                        onClick = {
                            viewModel.selectedCategoryFilter.value =
                                if (selectedCategory == cat.id) null else cat.id
                        },
                        label = { Text(cat.name, fontSize = 12.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(getCategoryColor(cat.colorHex), shape = RoundedCornerShape(2.dp))
                            )
                        }
                    )
                }

                // Priority Filter chips
                item {
                    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                }
                items(listOf(Priority.URGENT, Priority.HIGH, Priority.MEDIUM, Priority.LOW)) { pri ->
                    FilterChip(
                        selected = selectedPriority == pri,
                        onClick = {
                            viewModel.selectedPriorityFilter.value =
                                if (selectedPriority == pri) null else pri
                        },
                        label = { Text(pri.title, fontSize = 12.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(getPriorityColor(pri), shape = RoundedCornerShape(2.dp))
                            )
                        }
                    )
                }
            }

            // Tasks List
            if (tasks.isEmpty()) {
                val (emptyTitle, emptyMsg) = when {
                    searchQuery.isNotBlank() -> Pair("No tasks found", "No tasks match your search query '$searchQuery'")
                    selectedTab == TaskTab.COMPLETED -> Pair("No completed tasks", "Completed tasks will appear here.")
                    selectedTab == TaskTab.OVERDUE -> Pair("No overdue tasks", "Great job! You don't have any overdue tasks.")
                    selectedTab == TaskTab.TODAY -> Pair("No tasks due today", "Enjoy your day or add a task for today.")
                    else -> Pair("No tasks found", "Tap the '+' button to add your first task.")
                }

                EmptyStateView(
                    icon = if (selectedTab == TaskTab.COMPLETED) Icons.Default.CheckCircle else Icons.Outlined.Checklist,
                    title = emptyTitle,
                    message = emptyMsg,
                    actionLabel = if (searchQuery.isNotBlank()) "Clear Search" else "Add Task",
                    onActionClick = {
                        if (searchQuery.isNotBlank()) viewModel.searchQuery.value = "" else onOpenAddTask()
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tasks, key = { it.task.id }) { item ->
                        TaskItemCard(
                            taskWithDetails = item,
                            onToggleComplete = { viewModel.toggleTaskCompletion(item.task) },
                            onClick = { onOpenTaskDetail(item) },
                            onDelete = { viewModel.deleteTask(item) }
                        )
                    }

                    item {
                        BannerAdView(adManager = viewModel.adManager, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}
