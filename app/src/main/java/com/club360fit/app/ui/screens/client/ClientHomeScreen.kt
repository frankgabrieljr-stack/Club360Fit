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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    onOpenProfile: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenWorkouts: (String) -> Unit,
    onOpenMeals: (String) -> Unit,
    onOpenProgress: (String) -> Unit,
    onOpenSchedule: (String) -> Unit,
    onOpenPayments: (String) -> Unit,
    onOpenHabits: (String) -> Unit,
    onOpenNotifications: (String) -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgedBox(
                    badge = {
                        if (state.unreadNotifications > 0) {
                            Badge { Text("${state.unreadNotifications}") }
                        }
                    }
                ) {
                    IconButton(onClick = { clientId?.let(onOpenNotifications) }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = BurgundyPrimary
                        )
                    }
                }
                IconButton(onClick = onOpenProfile) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "My profile",
                        tint = BurgundyPrimary
                    )
                }
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

                state.adherence?.let { a ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("This week", style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Compliance", style = MaterialTheme.typography.bodyMedium)
                                Text("${a.weeklyComplianceScore}%", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Workouts", style = MaterialTheme.typography.bodyMedium)
                                Text("${a.sessionsLoggedThisWeek}/${a.expectedSessions} · ${a.workoutCompletionPercent}%", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Streak", style = MaterialTheme.typography.bodyMedium)
                                Text("${a.currentStreakDays} day${if (a.currentStreakDays == 1) "" else "s"} (best ${a.longestStreakDays})", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
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

                ClientHomeTwoTileRow(
                    left = { mod ->
                        CategoryTile(
                            title = "Daily habits",
                            line1 = "Water · steps · sleep",
                            line2 = "Log today",
                            icon = Icons.Default.CheckCircle,
                            modifier = mod,
                            enabled = clientId != null,
                            onClick = { clientId?.let(onOpenHabits) }
                        )
                    },
                    right = when {
                        state.canViewWorkouts -> { mod ->
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
                        state.canViewNutrition -> { mod ->
                            CategoryTile(
                                title = "Meals",
                                line1 = "Current: ${state.mealPlan?.title ?: "None"}",
                                line2 = "${state.mealPlans.size} plan${if (state.mealPlans.size == 1) "" else "s"} · meal photos",
                                icon = Icons.Default.Restaurant,
                                modifier = mod,
                                enabled = clientId != null,
                                onClick = { clientId?.let(onOpenMeals) }
                            )
                        }
                        else -> null
                    }
                )
                Spacer(Modifier.height(12.dp))

                // When both workouts and nutrition: Meals shares a row with Progress (same tile size as other rows).
                // Schedule moves to its own row below when events are enabled.
                if (state.canViewWorkouts && state.canViewNutrition) {
                    ClientHomeTwoTileRow(
                        left = { mod ->
                            CategoryTile(
                                title = "Meals",
                                line1 = "Current: ${state.mealPlan?.title ?: "None"}",
                                line2 = "${state.mealPlans.size} plan${if (state.mealPlans.size == 1) "" else "s"} · meal photos",
                                icon = Icons.Default.Restaurant,
                                modifier = mod,
                                enabled = clientId != null,
                                onClick = { clientId?.let(onOpenMeals) }
                            )
                        },
                        right = { mod ->
                            CategoryTile(
                                title = "Progress",
                                line1 = "Last: ${state.progressCheckIns.firstOrNull()?.checkInDate?.toDisplayDate() ?: "None"}",
                                line2 = "${state.progressCheckIns.size} log${if (state.progressCheckIns.size == 1) "" else "s"}",
                                icon = Icons.Default.TrendingUp,
                                modifier = mod,
                                enabled = clientId != null,
                                onClick = { clientId?.let(onOpenProgress) }
                            )
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.canViewEvents) {
                        ClientHomeTwoTileRow(
                            left = { mod ->
                                CategoryTile(
                                    title = "Schedule",
                                    line1 = state.nextSession?.let { n -> "Next: ${n.date.toDisplayDate()} ${n.time}".trim() } ?: "Next: None",
                                    line2 = "${state.upcomingSessions.size} upcoming",
                                    icon = Icons.Default.Event,
                                    modifier = mod,
                                    enabled = clientId != null,
                                    onClick = { clientId?.let(onOpenSchedule) }
                                )
                            },
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
                        Spacer(Modifier.height(12.dp))
                    }
                } else {
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
                }

                // Payments alone on a row unless already shown next to Schedule (workouts + nutrition + events).
                if (state.canViewPayments) {
                    val paymentsBesideSchedule =
                        state.canViewWorkouts && state.canViewNutrition && state.canViewEvents
                    if (!paymentsBesideSchedule) {
                        ClientHomeTwoTileRow(
                            left = { mod ->
                                CategoryTile(
                                    title = "Payments",
                                    line1 = "Venmo or Zelle",
                                    line2 = "View details / QR",
                                    icon = Icons.Default.Payments,
                                    modifier = mod,
                                    enabled = clientId != null,
                                    onClick = { clientId?.let(onOpenPayments) }
                                )
                            },
                            right = null
                        )
                    }
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
