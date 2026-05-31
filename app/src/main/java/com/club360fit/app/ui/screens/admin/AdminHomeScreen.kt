package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.R
import com.club360fit.app.data.ClientDto
import com.club360fit.app.data.MealPlanRepository
import com.club360fit.app.data.PushRegistrationRepository
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.data.WorkoutPlanRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.theme.Club360FitTheme
import com.club360fit.app.ui.theme.Club360Glass
import com.club360fit.app.ui.theme.club360Glass
import com.club360fit.app.ui.theme.club360ScreenBackground
import com.club360fit.app.ui.screens.gallery.TransformationGalleryScreen
import com.club360fit.app.ui.screens.profile.UserProfileScreen
import com.club360fit.app.ui.utils.buildClientMemberSummaryLine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    onOpenCoachNotifications: () -> Unit,
    onOpenClientProfile: (String?) -> Unit,
    onOpenClientHub: (String) -> Unit,
    onOpenClientWorkouts: (String) -> Unit,
    onOpenClientMeals: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: AdminHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val coachUnread by viewModel.coachUnreadCount.collectAsState()
    val scheduleViewModel: ScheduleViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var hubShowSchedule by remember { mutableStateOf(false) }
    var showCoachDirectory by remember { mutableStateOf(false) }
    val assignedClients = state.clients.filter { it.coachId != null }
    val newClients = state.clients.filter { it.coachId == null }
    var moreDestination by remember { mutableStateOf<AdminMoreDestination?>(null) }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 0) hubShowSchedule = false
        if (selectedTab != 5) moreDestination = null
    }

    LaunchedEffect(Unit) {
        PushRegistrationRepository.syncAndroidFcmTokenIfPossible(context)
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) viewModel.refreshCoachUnread()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = BurgundyPrimary,
        selectedTextColor = BurgundyPrimary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        floatingActionButton = {
            if (selectedTab == 3) {
                FloatingActionButton(
                    onClick = { onOpenClientProfile(null) },
                    containerColor = BurgundyPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Client")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BurgundyPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Hub") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Schedule") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    label = { Text("New clients") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    label = { Text("My clients") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Meal inbox") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    label = { Text("More") },
                    colors = navItemColors
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .club360ScreenBackground()
        ) {
            // Tab headers now scroll with each tab's content (iOS-style), except the
            // in-Hub "back to Hub" row when the Hub is showing the schedule.
            if (selectedTab == 0 && hubShowSchedule) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { hubShowSchedule = false }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to hub",
                            tint = Club360Glass.burgundy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Hub", color = Club360Glass.burgundy)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        color = Club360Glass.burgundy,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        if (hubShowSchedule) {
                            ScheduleTab(clients = assignedClients, viewModel = scheduleViewModel)
                        } else {
                            OverviewTab(
                                clients = assignedClients,
                                scheduleViewModel = scheduleViewModel,
                                coachUnread = coachUnread,
                                onOpenCoachNotifications = onOpenCoachNotifications,
                                onOpenClientProfile = onOpenClientProfile,
                                onOpenClientWorkouts = onOpenClientWorkouts,
                                onOpenClientMeals = onOpenClientMeals,
                                onScheduleSession = { selectedTab = 1 }
                            )
                        }
                    }
                    1 -> AdminScheduleOptionsTab(
                        clients = assignedClients,
                        viewModel = scheduleViewModel
                    )
                    2 -> NewClientsTab(
                        clients = newClients,
                        profileRolesByUserId = state.profileRolesByUserId,
                        onClaim = viewModel::claimClient
                    )
                    3 -> ClientsTab(
                        viewModel = viewModel,
                        onOpenProfile = onOpenClientProfile
                    )
                    4 -> CoachMealPhotoInboxScreen(
                        clients = assignedClients,
                        onOpenClientHub = { clientId, _ -> onOpenClientHub(clientId) }
                    )
                    5 -> when (moreDestination) {
                        AdminMoreDestination.Gallery -> TransformationGalleryScreen(
                            onBack = { moreDestination = null },
                            showTopBarBack = true
                        )
                        AdminMoreDestination.Profile -> UserProfileScreen(
                            onBack = { moreDestination = null },
                            onEditProfile = {},
                            onSignOut = onSignOut,
                            showTopBarBack = true,
                            onOpenCoachDirectory = { showCoachDirectory = true }
                        )
                        null -> AdminMoreTab(
                            onOpenGallery = { moreDestination = AdminMoreDestination.Gallery },
                            onOpenProfile = { moreDestination = AdminMoreDestination.Profile }
                        )
                    }
                }
            }
        }
    }
    if (showCoachDirectory) {
        CoachDirectoryScreen(
            onBack = { showCoachDirectory = false },
            modifier = Modifier.fillMaxSize()
        )
    }
    }
}

private enum class AdminMoreDestination {
    Gallery,
    Profile
}

private enum class AdminScheduleQuickView {
    Menu,
    Day,
    Week,
    Calendar
}

private fun weekStartSunday(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value % 7).toLong())

@Composable
private fun AdminScheduleOptionsTab(
    clients: List<ClientDto>,
    viewModel: ScheduleViewModel
) {
    val state by viewModel.uiState.collectAsState()
    var selectedView by remember { mutableStateOf(AdminScheduleQuickView.Menu) }
    val today = LocalDate.now()
    val weekStart = weekStartSunday(today)
    val weekEnd = weekStart.plusDays(6)
    val assignedIds = remember(clients) { clients.mapNotNull { it.id }.toSet() }
    val clientNameById = remember(clients) {
        clients.mapNotNull { client ->
            val id = client.id ?: return@mapNotNull null
            id to (client.fullName?.takeIf { it.isNotBlank() } ?: "(no name)")
        }.toMap()
    }
    val scopedEvents = remember(state.events, assignedIds) {
        state.events.filter { event -> event.clientId?.let { it in assignedIds } == true }
            .sortedWith(compareBy<ScheduleEvent> { it.date }.thenBy { it.time })
    }
    val dayEvents = remember(scopedEvents, today) {
        scopedEvents.filter { it.date == today }
    }
    val weekEvents = remember(scopedEvents, weekStart, weekEnd) {
        scopedEvents.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
    }

    when (selectedView) {
        AdminScheduleQuickView.Menu -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                TabHeader(
                    title = "Schedule",
                    subtitle = "Day and week views",
                    icon = Icons.Default.CalendarMonth
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminScheduleOptionCard(
                        title = "Schedule for the day",
                        subtitle = if (dayEvents.isEmpty()) {
                            "No sessions today"
                        } else {
                            "${dayEvents.size} session${if (dayEvents.size == 1) "" else "s"} today"
                        },
                        icon = Icons.Default.WbSunny,
                        onClick = { selectedView = AdminScheduleQuickView.Day }
                    )
                    AdminScheduleOptionCard(
                        title = "Schedule for the Week",
                        subtitle = "${weekEvents.size} session${if (weekEvents.size == 1) "" else "s"} this week",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { selectedView = AdminScheduleQuickView.Week }
                    )
                    AdminScheduleOptionCard(
                        title = "Calendar view",
                        subtitle = "Month view, date picker, and add events",
                        icon = Icons.Default.DateRange,
                        onClick = { selectedView = AdminScheduleQuickView.Calendar }
                    )
                }
            }
        }
        AdminScheduleQuickView.Day -> AdminScheduleEventList(
            title = "Schedule for the day",
            subtitle = today.toDisplayDate(),
            emptyText = "No sessions today.",
            events = dayEvents,
            clientNameById = clientNameById,
            onBack = { selectedView = AdminScheduleQuickView.Menu }
        )
        AdminScheduleQuickView.Week -> AdminScheduleEventList(
            title = "Schedule for the Week",
            subtitle = "${weekStart.toDisplayDate()} - ${weekEnd.toDisplayDate()}",
            emptyText = "No sessions this week.",
            events = weekEvents,
            clientNameById = clientNameById,
            onBack = { selectedView = AdminScheduleQuickView.Menu }
        )
        AdminScheduleQuickView.Calendar -> Column(modifier = Modifier.fillMaxSize()) {
            TextButton(
                onClick = { selectedView = AdminScheduleQuickView.Menu },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to schedule",
                    tint = Club360Glass.burgundy,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Schedule", color = Club360Glass.burgundy)
            }
            ScheduleTab(clients = clients, viewModel = viewModel)
        }
    }
}

@Composable
private fun AdminScheduleOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .club360Glass(cornerRadius = 26)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Club360Glass.burgundy,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Club360Glass.cardTitle
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Club360Glass.captionOnGlass
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Club360Glass.captionOnGlass
        )
    }
}

@Composable
private fun AdminScheduleEventList(
    title: String,
    subtitle: String,
    emptyText: String,
    events: List<ScheduleEvent>,
    clientNameById: Map<String, String>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        TextButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to schedule",
                tint = Club360Glass.burgundy,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Schedule", color = Club360Glass.burgundy)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Club360Glass.burgundy
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Club360Glass.captionOnGlass
        )
        Spacer(Modifier.height(12.dp))

        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = emptyText,
                    color = Club360Glass.captionOnGlass,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id ?: "${it.date}-${it.time}-${it.title}" }) { event ->
                    AdminScheduleEventSummaryCard(
                        event = event,
                        clientName = event.clientId?.let { clientNameById[it] }.orEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminScheduleEventSummaryCard(
    event: ScheduleEvent,
    clientName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .club360Glass(cornerRadius = 24)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.time.ifBlank { event.date.toDisplayDate() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Club360Glass.tealDark
            )
            if (event.time.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = event.date.toDisplayDate(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Club360Glass.captionOnGlass
                )
            }
            Spacer(Modifier.weight(1f))
            if (event.isCompleted) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Club360Glass.taupe
                )
            }
        }
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (event.isPastDue) Club360Glass.peachDeep else Club360Glass.cardTitle
        )
        if (clientName.isNotBlank()) {
            Text(
                text = clientName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Club360Glass.burgundy
            )
        }
        if (event.notes.isNotBlank()) {
            Text(
                text = event.notes,
                style = MaterialTheme.typography.bodySmall,
                color = Club360Glass.captionOnGlass,
                maxLines = 2
            )
        }
        if (event.isPastDue) {
            Text(
                text = "Past due",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Club360Glass.peachDeep
            )
        }
    }
}

@Composable
private fun AdminMoreTab(
    onOpenGallery: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        TabHeader(
            title = "More",
            subtitle = "Gallery and profile",
            icon = Icons.Default.MoreVert
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminMoreOptionCard(
                title = "Gallery",
                subtitle = "Transformation gallery",
                icon = Icons.Default.PhotoLibrary,
                onClick = onOpenGallery
            )
            AdminMoreOptionCard(
                title = "Profile",
                subtitle = "Account settings and sign out",
                icon = Icons.Default.Person,
                onClick = onOpenProfile
            )
        }
    }
}

@Composable
private fun AdminMoreOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .club360Glass(cornerRadius = 26)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Club360Glass.burgundy,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Club360Glass.cardTitle
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Club360Glass.captionOnGlass
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Club360Glass.captionOnGlass
        )
    }
}

/**
 * iOS-style scrolling tab header: an icon tile (or the brand logo) plus a large
 * burgundy title and subtitle, sitting on the cream Hub background.
 */
@Composable
private fun TabHeader(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    useLogo: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (useLogo) {
            Image(
                painter = painterResource(R.drawable.logo_burgundy),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(14.dp))
        } else if (icon != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Club360Glass.tealDark,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Club360Glass.burgundy
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Club360Glass.burgundy
            )
        }
    }
}

private enum class HubAssignMode { Workout, Meal }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTab(
    clients: List<ClientDto>,
    scheduleViewModel: ScheduleViewModel,
    coachUnread: Int,
    onOpenCoachNotifications: () -> Unit,
    onOpenClientProfile: (String?) -> Unit,
    onOpenClientWorkouts: (String) -> Unit,
    onOpenClientMeals: (String) -> Unit,
    onScheduleSession: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val events = scheduleState.events
    val today = LocalDate.now()
    val weekStart = weekStartSunday(today)
    val horizonEnd = today.plusDays(7)

    var workoutPlansThisWeek by remember { mutableIntStateOf(0) }
    var mealPlansThisWeek by remember { mutableIntStateOf(0) }

    LaunchedEffect(clients.map { it.id }.toString()) {
        var workoutCount = 0
        var mealCount = 0
        for (c in clients) {
            val id = c.id ?: continue
            runCatching { WorkoutPlanRepository.getAllPlans(id) }.getOrNull()
                ?.let { plans -> workoutCount += plans.count { it.weekStart == weekStart } }
            runCatching { MealPlanRepository.getAllPlans(id) }.getOrNull()
                ?.let { plans -> mealCount += plans.count { it.weekStart == weekStart } }
        }
        workoutPlansThisWeek = workoutCount
        mealPlansThisWeek = mealCount
    }

    val overdueEvents = events
        .filter { !it.isCompleted && it.date.isBefore(today) }
        .sortedBy { it.date }
    val upcomingEvents = events
        .filter { !it.isCompleted && !it.date.isBefore(today) && !it.date.isAfter(horizonEnd) }
        .sortedBy { it.date }

    val clientNameById = remember(clients) {
        clients.mapNotNull { c ->
            c.id?.let { it to (c.fullName?.takeIf { n -> n.isNotBlank() } ?: "(no name)") }
        }.toMap()
    }

    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf(today) }
    var assignMode by remember { mutableStateOf<HubAssignMode?>(null) }
    var calendarOffsetPx by remember { mutableFloatStateOf(0f) }

    fun jumpTo(date: LocalDate) {
        selectedDay = date
        visibleMonth = YearMonth.from(date)
        scope.launch { scrollState.animateScrollTo(calendarOffsetPx.toInt()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .club360ScreenBackground()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.logo_burgundy),
                contentDescription = "Club 360 Fit logo",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Coach hub",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Club360Glass.burgundy
                )
                Text(
                    text = "Assignments & schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Club360Glass.burgundy
                )
            }
            BadgedBox(
                badge = {
                    if (coachUnread > 0) {
                        Badge { Text("${coachUnread.coerceAtMost(99)}") }
                    }
                }
            ) {
                IconButton(onClick = onOpenCoachNotifications) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Coach updates",
                        tint = Club360Glass.burgundy
                    )
                }
            }
        }

        // At a glance
        HubSectionTitle("At a glance")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CoachStatTile(
                title = "Overdue",
                value = overdueEvents.size.toString(),
                subtitle = "Sessions not done",
                tint = Club360Glass.peachDeep,
                modifier = Modifier.weight(1f),
                onClick = { jumpTo(overdueEvents.firstOrNull()?.date ?: today) }
            )
            CoachStatTile(
                title = "Next 7 days",
                value = upcomingEvents.size.toString(),
                subtitle = "Upcoming sessions",
                tint = Club360Glass.tealDark,
                modifier = Modifier.weight(1f),
                onClick = { jumpTo(upcomingEvents.firstOrNull()?.date ?: today) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CoachStatTile(
                title = "This week",
                value = workoutPlansThisWeek.toString(),
                subtitle = "Workout plans",
                tint = Club360Glass.mintDeep,
                modifier = Modifier.weight(1f),
                enabled = clients.isNotEmpty(),
                onClick = { assignMode = HubAssignMode.Workout }
            )
            CoachStatTile(
                title = "This week",
                value = mealPlansThisWeek.toString(),
                subtitle = "Meal plans",
                tint = Club360Glass.teal,
                modifier = Modifier.weight(1f),
                enabled = clients.isNotEmpty(),
                onClick = { assignMode = HubAssignMode.Meal }
            )
        }

        // Quick assign
        HubSectionTitle("Quick assign")
        QuickAssignButton(
            title = "Schedule session",
            icon = Icons.Default.DateRange,
            enabled = clients.isNotEmpty(),
            onClick = onScheduleSession
        )
        QuickAssignButton(
            title = "Workout plan",
            icon = Icons.Default.FitnessCenter,
            enabled = clients.isNotEmpty(),
            onClick = { assignMode = HubAssignMode.Workout }
        )
        QuickAssignButton(
            title = "Meal plan",
            icon = Icons.Default.Restaurant,
            enabled = clients.isNotEmpty(),
            onClick = { assignMode = HubAssignMode.Meal }
        )

        // Calendar
        Column(
            modifier = Modifier.onGloballyPositioned { calendarOffsetPx = it.positionInParent().y },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HubSectionTitle("Calendar")
            CoachHubCalendarStrip(
                visibleMonth = visibleMonth,
                selectedDay = selectedDay,
                eventDates = events.map { it.date }.toSet(),
                onPrevMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onSelectDay = { selectedDay = it }
            )
            HubSelectedDaySessions(
                selectedDay = selectedDay,
                events = events.filter { it.date == selectedDay }.sortedBy { it.time },
                clientNameById = clientNameById
            )
        }

        // Clients
        HubSectionTitle("Clients")
        Text(
            text = "Open a member to edit their assigned plans and sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = Club360Glass.captionOnGlass
        )
        if (clients.isEmpty()) {
            Text(
                text = "No assigned clients yet. Check New clients to claim new signups.",
                style = MaterialTheme.typography.bodySmall,
                color = Club360Glass.captionOnGlass
            )
        } else {
            clients.forEach { client ->
                val cid = client.id
                if (cid != null && cid.isNotEmpty()) {
                    HubClientTile(
                        name = client.fullName?.takeIf { it.isNotBlank() } ?: "(no name)",
                        onClick = { onOpenClientProfile(cid) }
                    )
                }
            }
        }
    }

    assignMode?.let { mode ->
        ClientPickerDialog(
            title = if (mode == HubAssignMode.Workout) "Choose client - workout plan" else "Choose client - meal plan",
            clients = clients,
            onSelect = { id ->
                assignMode = null
                when (mode) {
                    HubAssignMode.Workout -> onOpenClientWorkouts(id)
                    HubAssignMode.Meal -> onOpenClientMeals(id)
                }
            },
            onDismiss = { assignMode = null }
        )
    }
}

@Composable
private fun HubSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Club360Glass.cardTitle
    )
}

@Composable
private fun CoachStatTile(
    title: String,
    value: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .club360Glass(cornerRadius = 20)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Club360Glass.captionOnGlass
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = tint
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = Club360Glass.captionOnGlass
        )
    }
}

@Composable
private fun QuickAssignButton(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .club360Glass(cornerRadius = 22)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Club360Glass.burgundy,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Club360Glass.cardTitle,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Club360Glass.captionOnGlass,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CoachHubCalendarStrip(
    visibleMonth: YearMonth,
    selectedDay: LocalDate,
    eventDates: Set<LocalDate>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {
    val startOfMonth = visibleMonth.atDay(1)
    val daysInMonth = visibleMonth.lengthOfMonth()
    val firstDayOfWeekIndex = startOfMonth.dayOfWeek.value % 7 // American: Sunday = 0
    val totalCells = firstDayOfWeekIndex + daysInMonth
    val rows = ceil(totalCells / 7f).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .club360Glass(cornerRadius = 22)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous month",
                    tint = Club360Glass.burgundy
                )
            }
            Text(
                text = visibleMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + visibleMonth.year,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Club360Glass.cardTitle
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next month",
                    tint = Club360Glass.burgundy
                )
            }
        }

        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Club360Glass.captionOnGlass,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        var dayCounter = 1
        repeat(rows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { cellIndex ->
                    val currentIndex = rowIndex * 7 + cellIndex
                    val showDay = currentIndex >= firstDayOfWeekIndex && dayCounter <= daysInMonth
                    val dayDate = if (showDay) visibleMonth.atDay(dayCounter) else null
                    val isSelected = dayDate != null && dayDate == selectedDay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .then(
                                if (isSelected) Modifier
                                    .background(Club360Glass.burgundy.copy(alpha = 0.14f), CircleShape)
                                    .border(2.dp, Club360Glass.burgundy, CircleShape)
                                else Modifier
                            )
                            .then(if (dayDate != null) Modifier.clickable { onSelectDay(dayDate) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayDate != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    dayCounter.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Club360Glass.burgundy else Club360Glass.cardTitle
                                )
                                if (eventDates.contains(dayDate)) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(Club360Glass.tealDark, CircleShape)
                                    )
                                }
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubSelectedDaySessions(
    selectedDay: LocalDate,
    events: List<ScheduleEvent>,
    clientNameById: Map<String, String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Sessions on ${selectedDay.toDisplayDate()}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Club360Glass.cardTitle
        )
        if (events.isEmpty()) {
            Text(
                text = "No sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = Club360Glass.captionOnGlass
            )
        } else {
            events.forEach { ev ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .club360Glass(cornerRadius = 18)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (ev.time.isNotBlank()) {
                            Text(
                                text = ev.time,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Club360Glass.tealDark
                            )
                        }
                        val name = ev.clientId?.let { clientNameById[it] }
                        if (!name.isNullOrBlank()) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Club360Glass.captionOnGlass
                            )
                        }
                    }
                    Text(
                        text = ev.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Club360Glass.cardTitle
                    )
                    if (ev.isCompleted) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Club360Glass.taupe
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HubClientTile(
    name: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .club360Glass(cornerRadius = 24)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Club360Glass.cardTitle
            )
            Text(
                text = "Plans & schedule editor",
                style = MaterialTheme.typography.labelSmall,
                color = Club360Glass.captionOnGlass
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Club360Glass.captionOnGlass,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientPickerDialog(
    title: String,
    clients: List<ClientDto>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (clients.isEmpty()) {
                Text("No assigned clients yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(clients, key = { it.id ?: it.userId }) { client ->
                        val cid = client.id
                        Text(
                            text = client.fullName?.takeIf { it.isNotBlank() } ?: "(no name)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (cid != null) Modifier.clickable { onSelect(cid) } else Modifier)
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsTab(
    viewModel: AdminHomeViewModel = viewModel(),
    onOpenProfile: (String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val assignedClients = state.clients.filter { it.coachId != null }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        TabHeader(
            title = "Coach",
            subtitle = "Clients",
            useLogo = true
        )
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BurgundyPrimary)
                    }
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadClients() },
                            colors = ButtonDefaults.buttonColors(containerColor = Club360Glass.burgundy)
                        ) {
                            Text("Retry")
                        }
                    }
                }
                assignedClients.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No assigned clients yet. New signups appear in New clients until a coach claims them.",
                            color = Club360Glass.captionOnGlass,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(assignedClients, key = { it.id ?: it.userId }) { client ->
                            ClientCard(
                                fullName = client.fullName ?: "(no name)",
                                goal = client.goal ?: "",
                                lastActive = client.lastActive ?: "Never",
                                subtitle = "Plans, meals, progress",
                                platformRole = state.profileRolesByUserId[client.userId],
                                age = client.age,
                                heightCm = client.heightCm,
                                weightKg = client.weightKg,
                                onClick = { client.id?.let { onOpenProfile(it) } },
                                onDelete = { client.id?.let { viewModel.deleteClient(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewClientsTab(
    clients: List<ClientDto>,
    profileRolesByUserId: Map<String, String>,
    onClaim: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TabHeader(
                title = "New clients",
                subtitle = "Review intake and claim members",
                icon = Icons.Default.PersonAdd
            )
            Spacer(Modifier.height(10.dp))
        }
        if (clients.isEmpty()) {
            item {
                Text(
                    "No new clients waiting to be claimed. New signups appear here until a coach claims them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Club360Glass.captionOnGlass,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            items(clients, key = { it.id ?: it.userId }) { client ->
                NewClientCard(
                    client = client,
                    platformRole = profileRolesByUserId[client.userId],
                    onClaim = {
                        client.id?.let(onClaim)
                    }
                )
            }
        }
    }
}

@Composable
private fun NewClientCard(
    client: ClientDto,
    platformRole: String?,
    onClaim: () -> Unit
) {
    val memberSummary = buildClientMemberSummaryLine(
        client.age,
        client.heightCm,
        client.weightKg,
        client.goal.orEmpty()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .club360Glass(cornerRadius = 28)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = client.fullName ?: "(no name)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Club360Glass.cardTitle
        )
        platformRole?.let { raw ->
            Text(
                text = if (raw.equals("admin", ignoreCase = true)) "App login: Admin" else "App login: Client",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (raw.equals("admin", ignoreCase = true)) Club360Glass.tealDark
                else Club360Glass.captionOnGlass
            )
        }
        Text(
            text = if (memberSummary.isBlank()) {
                "No age, height, weight, or goal on file yet."
            } else {
                memberSummary
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (memberSummary.isBlank()) Club360Glass.captionOnGlass else Club360Glass.cardTitle
        )
        Button(
            onClick = onClaim,
            enabled = client.id != null,
            colors = ButtonDefaults.buttonColors(containerColor = Club360Glass.burgundy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Claim client")
        }
    }
}

@Composable
fun ClientCard(
    fullName: String,
    goal: String,
    lastActive: String,
    subtitle: String = "Plans, meals, progress",
    /** `public.profiles.role` for this row’s `user_id` (`admin` / `client`). */
    platformRole: String? = null,
    age: Int? = null,
    heightCm: Int? = null,
    weightKg: Int? = null,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Client") },
            text = { Text("Remove $fullName? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .club360Glass(cornerRadius = 28)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Club360Glass.cardTitle
            )
            platformRole?.let { raw ->
                val label = if (raw.equals("admin", ignoreCase = true)) {
                    "App login: Admin"
                } else {
                    "App login: Client"
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (raw.equals("admin", ignoreCase = true)) Club360Glass.tealDark
                    else Club360Glass.captionOnGlass
                )
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Club360Glass.captionOnGlass
                )
            }
            val memberSummary = buildClientMemberSummaryLine(age, heightCm, weightKg, goal)
            if (memberSummary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = memberSummary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Club360Glass.cardTitle
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Club360Glass.peachDeep
                )
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Club360Glass.captionOnGlass,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScheduleTab(
    clients: List<ClientDto>,
    viewModel: ScheduleViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scheduleSnackbar by viewModel.snackbarMessage.collectAsState()
    val scheduleSnackbarIsError by viewModel.snackbarIsError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scheduleSnackbar) {
        val msg = scheduleSnackbar ?: return@LaunchedEffect
        val isErr = scheduleSnackbarIsError
        viewModel.clearScheduleSnackbar()
        snackbarHostState.showSnackbar(
            msg,
            duration = if (isErr) SnackbarDuration.Long else SnackbarDuration.Short
        )
    }

    val month = state.currentMonth
    val startOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val today = LocalDate.now()

    val firstDayOfWeekIndex = startOfMonth.dayOfWeek.value % 7  // American: Sunday = 0
    val totalCells = firstDayOfWeekIndex + daysInMonth
    val rows = ceil(totalCells / 7f).toInt()

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Schedules",
            style = MaterialTheme.typography.titleLarge,
            color = BurgundyPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month", tint = BurgundyPrimary)
            }
            Text(
                text = month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + month.year,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month", tint = BurgundyPrimary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // American: S M T W T F S
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { d ->
                Text(d, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Column {
            var dayCounter = 1
            repeat(rows) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(7) { cellIndex ->
                        val currentIndex = rowIndex * 7 + cellIndex
                        val showDay = currentIndex >= firstDayOfWeekIndex && dayCounter <= daysInMonth
                        val dayDate = if (showDay) month.atDay(dayCounter) else null
                        val hasEvents = dayDate != null && state.events.any { it.date == dayDate }
                        val isSelected = dayDate == state.selectedDate
                        val isToday = dayDate == today

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .then(
                                    if (showDay) Modifier
                                        .clickable { viewModel.selectDate(dayDate) }
                                        .border(
                                            if (isSelected) 2.dp else 0.dp,
                                            BurgundyPrimary,
                                            CircleShape
                                        )
                                        .background(
                                            if (isToday) BurgundyPrimary.copy(alpha = 0.15f)
                                            else androidx.compose.ui.graphics.Color.Transparent,
                                            CircleShape
                                        )
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showDay) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(dayCounter.toString(), style = MaterialTheme.typography.bodyMedium)
                                    if (hasEvents) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier.size(6.dp).background(BurgundyPrimary, CircleShape)
                                        )
                                    }
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }

        state.selectedDate?.let { selected ->
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.openAddEventDialog(selected) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BurgundyPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add event on ${selected.month.value}/${selected.dayOfMonth}")
            }
        }

        if (state.eventsForSelectedDate.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Events on ${state.selectedDate?.month?.value}/${state.selectedDate?.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                color = BurgundyPrimary
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.eventsForSelectedDate, key = { it.id.orEmpty() }) { event ->
                    ScheduleEventCard(
                        event = event,
                        onMarkDone = { viewModel.markCompleted(event) },
                        onDelete = { event.id?.let { viewModel.deleteEvent(it) } }
                    )
                }
            }
        }

        if (state.events.isEmpty() && state.selectedDate == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap a date to add events. Notifications for upcoming and past-due sessions coming next.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (state.showAddEventDialog && state.addEventDate != null) {
        AddScheduleEventDialog(
            date = state.addEventDate!!,
            clients = clients,
            onDismiss = { viewModel.dismissAddEventDialog() },
            onSave = { events ->
                viewModel.addEvents(events)
            }
        )
    }
}

@Composable
private fun ScheduleEventCard(
    event: ScheduleEvent,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isPastDue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (event.isPastDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (event.time.isNotBlank()) Text(event.time, style = MaterialTheme.typography.bodySmall)
                if (event.notes.isNotBlank()) Text(event.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                if (event.isPastDue) Text("Past due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            if (!event.isCompleted) {
                TextButton(onClick = onMarkDone) { Text("Done") }
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddScheduleEventDialog(
    date: LocalDate,
    clients: List<ClientDto>,
    onDismiss: () -> Unit,
    onSave: (events: List<ScheduleEvent>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedClientName by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var repeatWeekly by remember { mutableStateOf(false) }
    var weeksText by remember { mutableStateOf("2") }
    var daysOfWeekSelected by remember {
        mutableStateOf(
            mutableMapOf<DayOfWeek, Boolean>().apply {
                DayOfWeek.values().forEach { put(it, false) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New event — ${date.toDisplayDate()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (e.g. 10:00 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                // Client selector
                if (clients.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedClientName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign to client (optional)") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.fullName ?: "(no name)") },
                                    onClick = {
                                        selectedClientName = client.fullName ?: "(no name)"
                                        selectedClientId = client.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Simple weekly recurrence pattern
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repeat weekly?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = repeatWeekly,
                        onCheckedChange = { repeatWeekly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }
                if (repeatWeekly) {
                    Text(
                        "Select days of week and number of weeks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val labels = listOf(
                        DayOfWeek.SUNDAY to "S",
                        DayOfWeek.MONDAY to "M",
                        DayOfWeek.TUESDAY to "T",
                        DayOfWeek.WEDNESDAY to "W",
                        DayOfWeek.THURSDAY to "T",
                        DayOfWeek.FRIDAY to "F",
                        DayOfWeek.SATURDAY to "S"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        labels.forEach { (dow, label) ->
                            val selected = daysOfWeekSelected[dow] == true
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    daysOfWeekSelected = daysOfWeekSelected.toMutableMap().apply {
                                        this[dow] = !(this[dow] ?: false)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = weeksText,
                        onValueChange = { weeksText = it.filter(Char::isDigit) },
                        label = { Text("Number of weeks") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) return@TextButton

                val clientId = selectedClientId

                // If repeat is off or no days selected, create a single event on the chosen date.
                val events: List<ScheduleEvent> =
                    if (!repeatWeekly || daysOfWeekSelected.values.none { it }) {
                        listOf(
                            ScheduleEvent(
                                title = title.trim(),
                                date = date,
                                time = time.trim(),
                                notes = notes.trim(),
                                clientId = clientId
                            )
                        )
                    } else {
                        val weeks = weeksText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val selectedDays = daysOfWeekSelected.filterValues { it }.keys
                        val totalDays = weeks * 7
                        (0 until totalDays).mapNotNull { offset ->
                            val d = date.plusDays(offset.toLong())
                            if (d.dayOfWeek in selectedDays) {
                                ScheduleEvent(
                                    title = title.trim(),
                                    date = d,
                                    time = time.trim(),
                                    notes = notes.trim(),
                                    clientId = clientId
                                )
                            } else null
                        }
                    }

                if (events.isNotEmpty()) {
                    onSave(events)
                }
            }) {
                Text("Save", color = BurgundyPrimary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true)
@Composable
fun AdminHomeScreenPreview() {
    Club360FitTheme {
        AdminHomeScreen(
            onOpenCoachNotifications = {},
            onOpenClientProfile = {},
            onOpenClientHub = {},
            onOpenClientWorkouts = {},
            onOpenClientMeals = {},
            onSignOut = {}
        )
    }
}
