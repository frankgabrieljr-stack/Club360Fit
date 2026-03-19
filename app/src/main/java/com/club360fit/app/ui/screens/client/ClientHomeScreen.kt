package com.club360fit.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.data.MealPlanDto
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ClientHomeScreen(
    onOpenProfile: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenWorkouts: (String) -> Unit,
    onOpenMeals: (String) -> Unit,
    onOpenProgress: (String) -> Unit,
    onOpenSchedule: (String) -> Unit,
    onOpenPayments: (String) -> Unit,
    viewModel: ClientHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Home, 1 = Gallery
    val clientId = state.clientId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedTab == 0) "Client Home" else "Transformation Gallery",
                style = MaterialTheme.typography.headlineLarge,
                color = BurgundyPrimary
            )
            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "My profile",
                    tint = BurgundyPrimary
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Home") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { 
                    selectedTab = 1
                    onOpenGallery()
                },
                text = { Text("Gallery") }
            )
        }

        if (state.isLoading && selectedTab == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurgundyPrimary)
            }
        } else if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                }

                // Summary card + tappable tiles
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Next session", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        val next = state.nextSession
                        if (next == null) {
                            Text("No upcoming sessions scheduled.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("${next.date.toDisplayDate()} at ${next.time}", style = MaterialTheme.typography.bodyLarge)
                            if (next.notes.isNotBlank()) Text(next.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryTile(
                        title = "Workouts",
                        line1 = "Current: ${state.workoutPlan?.title ?: "None"}",
                        line2 = "${state.workoutPlans.size} total plan${if (state.workoutPlans.size == 1) "" else "s"}",
                        icon = Icons.Default.FitnessCenter,
                        modifier = Modifier.weight(1f),
                        enabled = clientId != null,
                        onClick = { clientId?.let(onOpenWorkouts) }
                    )
                    CategoryTile(
                        title = "Meals",
                        line1 = "Current: ${state.mealPlan?.title ?: "None"}",
                        line2 = "${state.mealPlans.size} total plan${if (state.mealPlans.size == 1) "" else "s"}",
                        icon = Icons.Default.Restaurant,
                        modifier = Modifier.weight(1f),
                        enabled = clientId != null,
                        onClick = { clientId?.let(onOpenMeals) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryTile(
                        title = "Progress",
                        line1 = "Last: ${state.progressCheckIns.firstOrNull()?.checkInDate?.toDisplayDate() ?: "None"}",
                        line2 = "${state.progressCheckIns.size} log${if (state.progressCheckIns.size == 1) "" else "s"}",
                        icon = Icons.Default.ShowChart,
                        modifier = Modifier.weight(1f),
                        enabled = clientId != null,
                        onClick = { clientId?.let(onOpenProgress) }
                    )
                    CategoryTile(
                        title = "Schedule",
                        line1 = state.nextSession?.let { "Next: ${it.date.toDisplayDate()} ${it.time}".trim() } ?: "Next: None",
                        line2 = "${state.upcomingSessions.size} upcoming",
                        icon = Icons.Default.Event,
                        modifier = Modifier.weight(1f),
                        enabled = clientId != null,
                        onClick = { clientId?.let(onOpenSchedule) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                WideCategoryTile(
                    title = "Payments",
                    line1 = "Pay via Venmo or Zelle",
                    line2 = "Tap to view details / QR",
                    icon = Icons.Default.Payments,
                    enabled = clientId != null,
                    onClick = { clientId?.let(onOpenPayments) }
                )
            }
        }
    }
}

@Composable
private fun WideCategoryTile(
    title: String,
    line1: String,
    line2: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = BurgundyPrimary, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(line1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(line2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryTile(
    title: String,
    line1: String,
    line2: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Card(
        modifier = modifier
            .aspectRatio(1.55f)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = BurgundyPrimary, modifier = Modifier.size(26.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(line1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(line2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
