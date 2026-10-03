package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.BannerAdView
import com.example.data.local.entity.Priority
import com.example.ui.components.getPriorityColor
import com.example.ui.theme.PriorityUrgent
import com.example.ui.viewmodel.ProductivityStats
import com.example.ui.viewmodel.StatsPeriod
import com.example.ui.viewmodel.TaskFlowViewModel

@Composable
fun StatisticsScreen(
    viewModel: TaskFlowViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.productivityStats.collectAsState()
    val selectedPeriod by viewModel.statsPeriod.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Title
        item {
            Text(
                text = "Productivity Insights",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Period Filter Tabs
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                StatsPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.statsPeriod.value = period },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsPeriod.entries.size)
                    ) {
                        Text(period.title, fontSize = 12.sp)
                    }
                }
            }
        }

        // Metric Cards Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Tasks Completed",
                        value = stats.totalCompleted.toString(),
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Completion Rate",
                        value = "${(stats.completionRate * 100).toInt()}%",
                        icon = Icons.Outlined.PieChart,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Active Streak",
                        value = "${stats.streakDays} Days",
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFF97316),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Overdue Tasks",
                        value = stats.overdueCount.toString(),
                        icon = Icons.Outlined.Warning,
                        iconTint = if (stats.overdueCount > 0) PriorityUrgent else Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Past 7 Days Completions Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Completed This Week",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxCount = stats.dailyCompletions.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        stats.dailyCompletions.forEach { (day, count) ->
                            val barHeightRatio = count.toFloat() / maxCount.toFloat()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                if (count > 0) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((barHeightRatio * 80).coerceAtLeast(4f).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (count > 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tasks By Priority Distribution
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tasks by Priority",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val totalTasks = stats.priorityCounts.values.sum().coerceAtLeast(1)
                    Priority.entries.forEach { p ->
                        val count = stats.priorityCounts[p] ?: 0
                        val ratio = count.toFloat() / totalTasks.toFloat()
                        val color = getPriorityColor(p)

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = p.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(text = "$count (${(ratio * 100).toInt()}%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Tasks By Category Distribution
        if (stats.categoryCounts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tasks by Category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val totalCatTasks = stats.categoryCounts.values.sum().coerceAtLeast(1)
                        stats.categoryCounts.forEach { (catName, count) ->
                            val ratio = count.toFloat() / totalCatTasks.toFloat()
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = catName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "$count tasks", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            BannerAdView(adManager = viewModel.adManager, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
