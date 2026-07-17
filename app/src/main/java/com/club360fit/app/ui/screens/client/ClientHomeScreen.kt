package com.club360fit.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.data.PushRegistrationRepository
import com.club360fit.app.ui.navigation.NotificationDeepLink
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toDisplayDate

private enum class ClientMainTab {
    TODAY, WORKOUTS, MEALS, PROGRESS, MORE
}

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
    onOpenMealPhotos: (String) -> Unit = {},
    onOpenCommunity: () -> Unit = {},
    pendingDeepLink: String? = null,
    onPendingDeepLinkConsumed: () -> Unit = {},
    viewModel: ClientHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val clientId = state.clientId
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = remember(state.canViewWorkouts, state.canViewNutrition) {
        buildList {
            add(ClientMainTab.TODAY)
            if (state.canViewWorkouts) add(ClientMainTab.WORKOUTS)
            if (state.canViewNutrition) add(ClientMainTab.MEALS)
            add(ClientMainTab.PROGRESS)
            add(ClientMainTab.MORE)
        }
    }

    LaunchedEffect(tabs) {
        if (selectedTab >= tabs.size) selectedTab = 0
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
        PushRegistrationRepository.syncAndroidFcmTokenIfPossible(context)
    }

    LaunchedEffect(pendingDeepLink, clientId) {
        when (pendingDeepLink) {
            NotificationDeepLink.PAYMENTS -> clientId?.let {
                onOpenPayments(it)
                onPendingDeepLinkConsumed()
            }
            NotificationDeepLink.COMMUNITY -> {
                onOpenCommunity()
                onPendingDeepLinkConsumed()
            }
            NotificationDeepLink.MEAL_PHOTOS -> clientId?.let {
                onOpenMealPhotos(it)
                onPendingDeepLinkConsumed()
            }
            NotificationDeepLink.SCHEDULE -> clientId?.let {
                onOpenSchedule(it)
                onPendingDeepLinkConsumed()
            }
            NotificationDeepLink.WORKOUTS -> clientId?.let {
                onOpenWorkouts(it)
                onPendingDeepLinkConsumed()
            }
            NotificationDeepLink.MEALS -> clientId?.let {
                onOpenMeals(it)
                onPendingDeepLinkConsumed()
            }
            NotificationDeepLink.PROGRESS -> clientId?.let {
                onOpenProgress(it)
                onPendingDeepLinkConsumed()
            }
            NotificationDeepLink.HABITS -> clientId?.let {
                onOpenHabits(it)
                onPendingDeepLinkConsumed()
            }
        }
    }

    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = BurgundyPrimary,
        selectedTextColor = BurgundyPrimary,
        indicatorColor = BurgundyPrimary.copy(alpha = 0.12f)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    val (label, icon) = when (tab) {
                        ClientMainTab.TODAY -> "Today" to Icons.Default.WbSunny
                        ClientMainTab.WORKOUTS -> "Workouts" to Icons.Default.FitnessCenter
                        ClientMainTab.MEALS -> "Meals" to Icons.Default.Restaurant
                        ClientMainTab.PROGRESS -> "Progress" to Icons.Default.TrendingUp
                        ClientMainTab.MORE -> "More" to Icons.Default.MoreHoriz
                    }
                    val isShellTab = tab == ClientMainTab.TODAY || tab == ClientMainTab.MORE
                    NavigationBarItem(
                        selected = isShellTab && selectedTab == index,
                        onClick = {
                            when (tab) {
                                ClientMainTab.TODAY, ClientMainTab.MORE -> selectedTab = index
                                ClientMainTab.WORKOUTS -> clientId?.let(onOpenWorkouts)
                                ClientMainTab.MEALS -> clientId?.let(onOpenMeals)
                                ClientMainTab.PROGRESS -> clientId?.let(onOpenProgress)
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = navColors
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (tabs.getOrNull(selectedTab) ?: ClientMainTab.TODAY) {
                ClientMainTab.MORE -> MoreTabContent(
                    state = state,
                    onOpenCommunity = onOpenCommunity,
                    onOpenProfile = onOpenProfile,
                    onOpenGallery = onOpenGallery,
                    onOpenSchedule = { clientId?.let(onOpenSchedule) },
                    onOpenPayments = { clientId?.let(onOpenPayments) },
                    onOpenHabits = { clientId?.let(onOpenHabits) },
                    onOpenMealPhotos = { clientId?.let(onOpenMealPhotos) },
                    onOpenNotifications = { clientId?.let(onOpenNotifications) }
                )
                else -> TodayTabContent(
                    state = state,
                    onOpenNotifications = { clientId?.let(onOpenNotifications) },
                    onOpenSchedule = { clientId?.let(onOpenSchedule) },
                    onOpenWorkouts = { clientId?.let(onOpenWorkouts) },
                    onOpenMealPhotos = { clientId?.let(onOpenMealPhotos) },
                    onOpenHabits = { clientId?.let(onOpenHabits) },
                    onOpenPayments = { clientId?.let(onOpenPayments) }
                )
            }
        }
    }
}

@Composable
private fun TodayTabContent(
    state: ClientHomeUiState,
    onOpenNotifications: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenWorkouts: () -> Unit,
    onOpenMealPhotos: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenPayments: () -> Unit
) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BurgundyPrimary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Today", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = BurgundyPrimary)
                Text(state.welcomeName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BadgedBox(
                badge = {
                    if (state.unreadNotifications > 0) {
                        Badge { Text("${state.unreadNotifications}") }
                    }
                }
            ) {
                IconButton(onClick = onOpenNotifications) {
                    Icon(Icons.Default.Notifications, contentDescription = "Updates", tint = BurgundyPrimary)
                }
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (state.clientId != null && !state.hasAssignedCoach) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BurgundyPrimary.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Waiting for a coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BurgundyPrimary)
                    Text(
                        "You’re all set — a coach will claim you soon. Community and browsing still work.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (state.canViewEvents) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSchedule),
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
                        if (next.notes.isNotBlank()) {
                            Text(next.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Tap to open schedule", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text(
            "Quick actions",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (state.canViewWorkouts) {
            QuickActionRow(
                title = "Log today’s workout",
                subtitle = state.workoutPlan?.title?.let { "Current: $it" } ?: "Open workouts",
                icon = Icons.Default.FitnessCenter,
                onClick = onOpenWorkouts
            )
        }
        if (state.canViewNutrition) {
            QuickActionRow(
                title = "Log a meal photo",
                subtitle = "Breakfast · lunch · dinner · snacks",
                icon = Icons.Default.CameraAlt,
                onClick = onOpenMealPhotos
            )
        }
        QuickActionRow(
            title = "Daily habits",
            subtitle = "Water · steps · sleep",
            icon = Icons.Default.CheckCircle,
            onClick = onOpenHabits
        )
        if (state.canViewPayments) {
            QuickActionRow(
                title = "Payments",
                subtitle = "Venmo or Zelle",
                icon = Icons.Default.Payments,
                onClick = onOpenPayments
            )
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
                    Text("Compliance ${a.weeklyComplianceScore}%", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Workouts ${a.sessionsLoggedThisWeek}/${a.expectedSessions} · streak ${a.currentStreakDays}d",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreTabContent(
    state: ClientHomeUiState,
    onOpenCommunity: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenMealPhotos: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("More", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = BurgundyPrimary)
        Text(
            "Community, profile, and extras",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        MoreRow("Community", "Peer tips, wins, and coach replies", Icons.Default.Groups, onOpenCommunity)
        MoreRow("Profile", "Account and member settings", Icons.Default.Person, onOpenProfile)
        MoreRow("Updates", "Notifications from your coach", Icons.Default.Notifications, onOpenNotifications)
        if (state.canViewEvents) {
            MoreRow("Schedule", "Upcoming sessions", Icons.Default.Event, onOpenSchedule)
        }
        if (state.canViewPayments) {
            MoreRow("Payments", "Venmo / Zelle details", Icons.Default.Payments, onOpenPayments)
        }
        if (state.canViewNutrition) {
            MoreRow("Meal photos", "Day-by-day food log", Icons.Default.CameraAlt, onOpenMealPhotos)
        }
        MoreRow("Daily habits", "Water, steps, sleep", Icons.Default.CheckCircle, onOpenHabits)
        MoreRow("Gallery", "Transformation photos", Icons.Default.PhotoLibrary, onOpenGallery)
    }
}

@Composable
private fun QuickActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BurgundyPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BurgundyPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MoreRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = BurgundyPrimary, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
