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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.LaunchedEffect
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
    onOpenMealPhotos: (String) -> Unit,
    viewModel: ClientHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Home, 1 = Gallery
    val clientId = state.clientId

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

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
                text = when {
                    selectedTab == 1 -> "Transformation Gallery"
                    selectedTab == 0 && (state.isLoading || state.clientId == null) -> "Welcome"
                    else -> "Welcome, ${state.welcomeName}"
                },
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
                if (state.canViewEvents) {
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
                }

                if (state.canViewWorkouts || state.canViewNutrition) {
                    ClientHomeTwoTileRow(
                        left = if (state.canViewWorkouts) {
                            { mod ->
                                CategoryTile(
                                    title = "Workouts",
                                    line1 = "Current: ${state.workoutPlan?.title ?: "None"}",
                                    line2 = "${state.workoutPlans.size} total plan${if (state.workoutPlans.size == 1) "" else "s"}",
                                    icon = Icons.Default.FitnessCenter,
                                    modifier = mod,
                                    enabled = clientId != null,
                                    onClick = { clientId?.let(onOpenWorkouts) }
                                )
                            }
                        } else null,
                        right = if (state.canViewNutrition) {
                            { mod ->
                                CategoryTile(
                                    title = "Meals",
                                    line1 = "Current: ${state.mealPlan?.title ?: "None"}",
                                    line2 = "${state.mealPlans.size} total plan${if (state.mealPlans.size == 1) "" else "s"}",
                                    icon = Icons.Default.Restaurant,
                                    modifier = mod,
                                    enabled = clientId != null,
                                    onClick = { clientId?.let(onOpenMeals) }
                                )
                            }
                        } else null
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Progress always; Schedule only if club events are enabled for this client
                ClientHomeTwoTileRow(
                    left = { mod ->
                        CategoryTile(
                            title = "Progress",
                            line1 = "Last: ${state.progressCheckIns.firstOrNull()?.checkInDate?.toDisplayDate() ?: "None"}",
                            line2 = "${state.progressCheckIns.size} log${if (state.progressCheckIns.size == 1) "" else "s"}",
                            icon = Icons.Default.TrendingUp,
                            modifier = mod,
                            enabled = clientId != null,
                            onClick = { clientId?.let(onOpenProgress) }
                        )
                    },
                        right = if (state.canViewEvents) {
                            { mod ->
                                CategoryTile(
                                    title = "Schedule",
                                    line1 = state.nextSession?.let { n -> "Next: ${n.date.toDisplayDate()} ${n.time}".trim() } ?: "Next: None",
                                    line2 = "${state.upcomingSessions.size} upcoming",
                                    icon = Icons.Default.Event,
                                    modifier = mod,
                                    enabled = clientId != null,
                                    onClick = { clientId?.let(onOpenSchedule) }
                                )
                            }
                        } else null
                )
                Spacer(Modifier.height(12.dp))

                if (state.canViewNutrition || state.canViewPayments) {
                    ClientHomeTwoTileRow(
                        left = if (state.canViewNutrition) {
                            { mod ->
                                CategoryTile(
                                    title = "Meal photos",
                                    line1 = "Log meals for coach",
                                    line2 = "Camera or gallery",
                                    icon = Icons.Default.CameraAlt,
                                    modifier = mod,
                                    enabled = clientId != null,
                                    onClick = { clientId?.let(onOpenMealPhotos) }
                                )
                            }
                        } else null,
                        right = if (state.canViewPayments) {
                            { mod ->
                                CategoryTile(
                                    title = "Payments",
                                    line1 = "Venmo or Zelle",
                                    line2 = "View details / QR",
                                    icon = Icons.Default.Payments,
                                    modifier = mod,
                                    enabled = clientId != null,
                                    onClick = { clientId?.let(onOpenPayments) }
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientHomeTwoTileRow(
    left: (@Composable (Modifier) -> Unit)?,
    right: (@Composable (Modifier) -> Unit)?
) {
    if (left == null && right == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (left != null) {
            left(Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (right != null) {
            right(Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
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
