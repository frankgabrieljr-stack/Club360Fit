package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.data.ClientDto
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.theme.Club360FitTheme
import com.club360fit.app.ui.utils.toFeetInches
import com.club360fit.app.ui.utils.toPounds
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

@Composable
fun AdminHomeScreen(
    onSignOut: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenClientDetails: (String) -> Unit,
    onOpenClientProfile: (String?) -> Unit,
    viewModel: AdminHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val tabs = listOf("Overview", "Clients", "Schedule")
    var selectedTab by remember { mutableIntStateOf(1) } // default to Clients

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { onOpenClientProfile(null) },
                    containerColor = BurgundyPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Client")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Admin Dashboard",
                        style = MaterialTheme.typography.headlineLarge,
                        color = BurgundyPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "My profile",
                            tint = BurgundyPrimary
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign out",
                            tint = BurgundyPrimary
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = BurgundyPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BurgundyPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        selectedContentColor = BurgundyPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewTab(clients = state.clients)
                1 -> ClientsTab(
                    viewModel = viewModel,
                    onOpenDetails = onOpenClientDetails,
                    onOpenProfile = onOpenClientProfile
                )
                2 -> ScheduleTab()
            }
        }
    }
}

@Composable
fun OverviewTab(
    clients: List<ClientDto>,
    scheduleViewModel: ScheduleViewModel = viewModel()
) {
    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val events = scheduleState.events
    val today = LocalDate.now()

    val startOfWeek = today.with(DayOfWeek.SUNDAY)
    val endOfWeek = startOfWeek.plusDays(6)

    val activeClients = clients.size
    val sessionsThisWeek = events.count { !it.date.isBefore(startOfWeek) && !it.date.isAfter(endOfWeek) }
    val completedThisWeek = events.count { it.isCompleted && !it.date.isBefore(startOfWeek) && !it.date.isAfter(endOfWeek) }
    val pastDueSessions = events.count { !it.isCompleted && it.date.isBefore(today) }

    val todayEvents = events
        .filter { it.date == today }
        .sortedBy { it.time }

    val clientById = clients.associateBy { it.id }
    val twoWeeksAgo = today.minusDays(14)
    val atRiskClients = clients.filter { client ->
        val id = client.id ?: return@filter false
        val recentEvents = events.any { it.clientId == id && !it.date.isBefore(twoWeeksAgo) }
        !recentEvents
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.headlineSmall,
            color = BurgundyPrimary
        )
        // KPI cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                title = "Active clients",
                value = activeClients.toString(),
                modifier = Modifier.weight(1f)
            )
            OverviewStatCard(
                title = "Sessions this week",
                value = sessionsThisWeek.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                title = "Completed this week",
                value = completedThisWeek.toString(),
                modifier = Modifier.weight(1f)
            )
            OverviewStatCard(
                title = "Past-due sessions",
                value = pastDueSessions.toString(),
                modifier = Modifier.weight(1f),
                emphasize = pastDueSessions > 0
            )
        }

        // Today's schedule
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Today",
            style = MaterialTheme.typography.titleMedium,
            color = BurgundyPrimary
        )
        if (todayEvents.isEmpty()) {
            Text(
                text = "No sessions scheduled today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todayEvents, key = { it.id ?: it.hashCode().toString() }) { event ->
                    val clientName = event.clientId?.let { id -> clientById[id]?.fullName } ?: ""
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleSmall)
                            if (event.time.isNotBlank()) {
                                Text(event.time, style = MaterialTheme.typography.bodySmall)
                            }
                            if (clientName.isNotBlank()) {
                                Text(
                                    text = clientName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // At-risk clients
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "At-risk clients",
            style = MaterialTheme.typography.titleMedium,
            color = BurgundyPrimary
        )
        if (atRiskClients.isEmpty()) {
            Text(
                text = "No clients flagged as at-risk in the last 14 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(atRiskClients, key = { it.id ?: it.userId }) { client ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = client.fullName.orEmpty().ifBlank { "(no name)" },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "No sessions in the last 14 days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (emphasize) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (emphasize) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ClientsTab(
    viewModel: AdminHomeViewModel = viewModel(),
    onOpenDetails: (String) -> Unit,
    onOpenProfile: (String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Clients",
            style = MaterialTheme.typography.titleLarge,
            color = BurgundyPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
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
                    Button(onClick = { viewModel.loadClients() }) {
                        Text("Retry")
                    }
                }
            }
            state.clients.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No clients yet. Tap + to add one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Demo clients
                    items(demoClientSummaries, key = { "demo_${it.id}" }) { demo ->
                        ClientCard(
                            fullName = demo.name,
                            goal = demo.goal,
                            lastActive = demo.lastActive,
                            onClick = { onOpenDetails(demo.id) },
                            onDelete = null // Can't delete demo data
                        )
                    }
                    
                    // Real clients
                    items(state.clients, key = { it.id ?: it.userId }) { client ->
                        ClientCard(
                            fullName = client.fullName ?: "(no name)",
                            goal = client.goal ?: "",
                            lastActive = client.lastActive ?: "Never",
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

@Composable
fun ClientCard(
    fullName: String,
    goal: String,
    lastActive: String,
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    age?.let {
                        Text("Age: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    heightCm?.let {
                        val (ft, inc) = it.toFeetInches()
                        Text("${ft}' ${inc}\"", style = MaterialTheme.typography.bodySmall)
                    }
                    weightKg?.let {
                        Text("${it.toPounds()} lbs", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (goal.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = goal,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleTab(
    viewModel: ScheduleViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val month = state.currentMonth
    val startOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val today = LocalDate.now()

    val firstDayOfWeekIndex = startOfMonth.dayOfWeek.value % 7  // American: Sunday = 0
    val totalCells = firstDayOfWeekIndex + daysInMonth
    val rows = ceil(totalCells / 7f).toInt()

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

    if (state.showAddEventDialog && state.addEventDate != null) {
        AddScheduleEventDialog(
            date = state.addEventDate!!,
            onDismiss = { viewModel.dismissAddEventDialog() },
            onSave = { title, time, notes ->
                viewModel.addEvent(
                    ScheduleEvent(
                        title = title,
                        date = state.addEventDate!!,
                        time = time,
                        notes = notes
                    )
                )
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
private fun AddScheduleEventDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (title: String, time: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New event — ${date.month.value}/${date.dayOfMonth}/${date.year}") },
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onSave(title.trim(), time.trim(), notes.trim())
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
        AdminHomeScreen(onSignOut = {}, onOpenProfile = {}, onOpenClientDetails = {}, onOpenClientProfile = {})
    }
}
