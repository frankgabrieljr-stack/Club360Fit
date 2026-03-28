package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.data.ClientRepository
import com.club360fit.app.data.AdherenceMetricsRepository
import com.club360fit.app.data.AdherenceSnapshot
import com.club360fit.app.data.DailyHabitLogDto
import com.club360fit.app.data.DailyHabitRepository
import com.club360fit.app.data.MealPlanDto
import com.club360fit.app.data.MealPlanRepository
import com.club360fit.app.data.ProgressCheckInDto
import com.club360fit.app.data.ProgressRepository
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.data.WorkoutPlanRepository
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.data.ScheduleRepository
import com.club360fit.app.ui.utils.toDisplayDate
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.fromFeetInches
import com.club360fit.app.ui.utils.formatWeightLbsFromKg
import com.club360fit.app.ui.utils.fromPounds
import com.club360fit.app.ui.utils.toFeetInches
import com.club360fit.app.ui.utils.SubmitResultMessages
import com.club360fit.app.ui.utils.toPounds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(
    clientId: String?,
    onBack: () -> Unit,
    onOpenWorkouts: (String) -> Unit,
    onOpenMeals: (String) -> Unit,
    onOpenProgress: (String) -> Unit,
    onOpenSchedule: (String) -> Unit,
    onOpenPayments: (String) -> Unit
) {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClientProfileViewModel(clientId) as T
        }
    }
    val viewModel: ClientProfileViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var transferTargetCoachId by remember(clientId) { mutableStateOf("") }
    var transferBusy by remember { mutableStateOf(false) }
    var transferError by remember { mutableStateOf<String?>(null) }
    var showTransferConfirm by remember { mutableStateOf(false) }
    var showTransferSuccess by remember { mutableStateOf(false) }
    var showCoachDirectory by remember { mutableStateOf(false) }

    var workoutPlans by remember { mutableStateOf<List<WorkoutPlanDto>>(emptyList()) }
    var mealPlans by remember { mutableStateOf<List<MealPlanDto>>(emptyList()) }
    var progressCheckIns by remember { mutableStateOf<List<ProgressCheckInDto>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }
    var upcomingSessions by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }
    var pastSessions by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }
    var adherence by remember { mutableStateOf<AdherenceSnapshot?>(null) }
    var habitHistory by remember { mutableStateOf<List<DailyHabitLogDto>>(emptyList()) }

    LaunchedEffect(state.client.id, refreshKey) {
        val id = state.client.id ?: return@LaunchedEffect
        workoutPlans = WorkoutPlanRepository.getAllPlans(id)
        mealPlans = MealPlanRepository.getAllPlans(id)
        progressCheckIns = ProgressRepository.getForClient(id)
        adherence = runCatching { AdherenceMetricsRepository.loadSnapshotForCoachView(id) }.getOrNull()
        val t = LocalDate.now()
        habitHistory = runCatching {
            DailyHabitRepository.listRange(id, t.minusDays(13), t)
        }.getOrDefault(emptyList())

        val allSessions = ScheduleRepository.getEventsForClient(id)
        val today = LocalDate.now()
        upcomingSessions = allSessions
            .filter { !it.date.isBefore(today) }
            .sortedWith(compareBy({ it.date }, { it.time }))
        pastSessions = allSessions
            .filter { it.date.isBefore(today) }
            .sortedWith(compareBy({ it.date }, { it.time }))
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val name = state.client.fullName?.takeIf { it.isNotBlank() }
                    Text(name ?: if (clientId == null) "New Client" else "Client Profile")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BurgundyPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = BurgundyPrimary
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BurgundyPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                val c = state.client
                // Summary card
                Text(
                    text = "Client Profile Summary",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                state.platformAccountRole?.let { raw ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (raw.equals("admin", ignoreCase = true)) {
                            "App login: Admin (coach tools in Club360Fit)"
                        } else {
                            "App login: Client (member app)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = BurgundyPrimary.copy(alpha = 0.92f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        c.age?.let { Text("Age: $it") }
                        c.heightCm?.let {
                            val (ft, inc) = it.toFeetInches()
                            Text("Height: ${ft}' ${inc}\"")
                        }
                        c.weightKg?.let { w ->
                            formatWeightLbsFromKg(w)?.let { label -> Text("Weight: $label") }
                        }
                        c.goal?.trim()?.takeIf { it.isNotEmpty() }?.let { Text("Goal: $it") }
                        c.foodRestrictions?.takeIf { it.isNotBlank() }?.let { Text("Food restrictions: $it") }
                    }
                }

                adherence?.let { a ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Adherence (this week)", style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Compliance ${a.weeklyComplianceScore}%", style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                            Text("Workouts: ${a.sessionsLoggedThisWeek}/${a.expectedSessions} (${a.workoutCompletionPercent}%)", style = MaterialTheme.typography.bodySmall)
                            Text("Streak: ${a.currentStreakDays} days (best ${a.longestStreakDays})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (habitHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Habit logs (recent)", style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                    habitHistory.take(14).forEach { h ->
                        Text(
                            "${h.logDate.toDisplayDate()}: water ${if (h.waterDone) "✓" else "—"} · steps ${h.steps ?: "—"} · sleep ${h.sleepHours ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tiles (2-column layout)
                val idForNav = c.id
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryTile(
                        title = "Workouts",
                        subtitle = "${workoutPlans.size} plan${if (workoutPlans.size == 1) "" else "s"}",
                        icon = Icons.Default.FitnessCenter,
                        modifier = Modifier.weight(1f),
                        enabled = idForNav != null,
                        onClick = { idForNav?.let(onOpenWorkouts) }
                    )
                    CategoryTile(
                        title = "Meal Plans",
                        subtitle = "${mealPlans.size} plan${if (mealPlans.size == 1) "" else "s"} · photos",
                        icon = Icons.Default.Restaurant,
                        modifier = Modifier.weight(1f),
                        enabled = idForNav != null,
                        onClick = { idForNav?.let(onOpenMeals) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryTile(
                        title = "Progress",
                        subtitle = "${progressCheckIns.size} log${if (progressCheckIns.size == 1) "" else "s"}",
                        icon = Icons.Default.ShowChart,
                        modifier = Modifier.weight(1f),
                        enabled = idForNav != null,
                        onClick = { idForNav?.let(onOpenProgress) }
                    )
                    CategoryTile(
                        title = "Schedule",
                        subtitle = "${upcomingSessions.size} upcoming",
                        icon = Icons.Default.Event,
                        modifier = Modifier.weight(1f),
                        enabled = idForNav != null,
                        onClick = { idForNav?.let(onOpenSchedule) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                WideCategoryTile(
                    title = "Payments",
                    subtitle = "Venmo / Zelle settings",
                    icon = Icons.Default.Payments,
                    enabled = idForNav != null,
                    onClick = { idForNav?.let(onOpenPayments) }
                )

                if (clientId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Transfer to another coach",
                        style = MaterialTheme.typography.titleMedium,
                        color = BurgundyPrimary
                    )
                    Text(
                        text = "Paste another coach’s user ID, or open the directory below. You must be this member’s current coach.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showCoachDirectory = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BurgundyPrimary)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse coaches & copy IDs", fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = transferTargetCoachId,
                        onValueChange = {
                            transferTargetCoachId = it
                            transferError = null
                        },
                        label = { Text("Target coach user UUID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !transferBusy
                    )
                    transferError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showTransferConfirm = true },
                        enabled = !transferBusy && transferTargetCoachId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (transferBusy) "Transferring…" else "Transfer client")
                    }
                }

                if (showTransferConfirm && clientId != null) {
                    AlertDialog(
                        onDismissRequest = { if (!transferBusy) showTransferConfirm = false },
                        title = { Text("Transfer this client?") },
                        text = { Text("You will no longer see this client in your list.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        transferBusy = true
                                        transferError = null
                                        try {
                                            ClientRepository.transferClientToCoach(clientId, transferTargetCoachId)
                                            showTransferConfirm = false
                                            showTransferSuccess = true
                                        } catch (e: Exception) {
                                            transferError = e.message ?: "Transfer failed"
                                            showTransferConfirm = false
                                        } finally {
                                            transferBusy = false
                                        }
                                    }
                                },
                                enabled = !transferBusy
                            ) { Text("Transfer") }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showTransferConfirm = false },
                                enabled = !transferBusy
                            ) { Text("Cancel") }
                        }
                    )
                }

                if (showTransferSuccess) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Transfer complete") },
                        text = {
                            Text(
                                "This member is now assigned to the other coach. They will no longer appear in your client list."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showTransferSuccess = false
                                    onBack()
                                }
                            ) { Text("OK") }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Editable form
                OutlinedTextField(
                    value = state.client.fullName ?: "",
                    onValueChange = viewModel::updateName,
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.client.age?.toString() ?: "",
                        onValueChange = { viewModel.updateAge(it.toIntOrNull()) },
                        label = { Text("Age") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Imperial Height
                    val heightCm = state.client.heightCm ?: 0
                    val (feet, inches) = heightCm.toFeetInches()
                    var feetText by remember(heightCm) { mutableStateOf(if (feet > 0) feet.toString() else "") }
                    var inchesText by remember(heightCm) { mutableStateOf(if (inches > 0) inches.toString() else "") }

                    OutlinedTextField(
                        value = feetText,
                        onValueChange = {
                            feetText = it.filter(Char::isDigit)
                            val f = feetText.toIntOrNull() ?: 0
                            val i = inchesText.toIntOrNull() ?: 0
                            viewModel.updateHeight(fromFeetInches(f, i))
                        },
                        label = { Text("Ht (ft)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inchesText,
                        onValueChange = {
                            inchesText = it.filter(Char::isDigit)
                            val f = feetText.toIntOrNull() ?: 0
                            val i = inchesText.toIntOrNull() ?: 0
                            viewModel.updateHeight(fromFeetInches(f, i))
                        },
                        label = { Text("Ht (in)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Imperial Weight
                val weightKg = state.client.weightKg ?: 0
                var weightLbsText by remember(weightKg) {
                    mutableStateOf(if (weightKg > 0) weightKg.toPounds().toString() else "")
                }
                
                OutlinedTextField(
                    value = weightLbsText,
                    onValueChange = {
                        weightLbsText = it.filter(Char::isDigit)
                        val lbs = weightLbsText.toIntOrNull()
                        viewModel.updateWeight(lbs?.let { fromPounds(it) })
                    },
                    label = { Text("Weight (lbs)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                ClientGoalDropdown(
                    selectedGoal = state.client.goal,
                    onGoalSelected = viewModel::updateGoal,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "View privileges",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Text(
                    "Turn sections off to hide those tiles on the client’s home screen (they can still sign in).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrivSwitchRow(
                    label = "Nutrition plans",
                    subtitle = "Meals & meal photos tiles.",
                    checked = state.client.canViewNutrition,
                    onChecked = { viewModel.updatePrivilege(nutrition = it) }
                )
                PrivSwitchRow(
                    label = "Workout programs",
                    subtitle = "Workouts tile.",
                    checked = state.client.canViewWorkouts,
                    onChecked = { viewModel.updatePrivilege(workouts = it) }
                )
                PrivSwitchRow(
                    label = "Payments",
                    subtitle = "Venmo / Zelle tile.",
                    checked = state.client.canViewPayments,
                    onChecked = { viewModel.updatePrivilege(payments = it) }
                )
                PrivSwitchRow(
                    label = "Run club events",
                    subtitle = "Next session card, Schedule tile, and schedule data.",
                    checked = state.client.canViewEvents,
                    onChecked = { viewModel.updatePrivilege(events = it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Coach/admin access for a member is not edited here. In Supabase: Authentication → Users → select the user → User metadata → set \"role\" to \"admin\"; they must sign out and back in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BurgundyPrimary)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            viewModel.save(
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            SubmitResultMessages.SAVED_SUCCESS,
                                            duration = SnackbarDuration.Short
                                        )
                                        delay(450)
                                        onBack()
                                    }
                                },
                                onError = { msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            SubmitResultMessages.failure(msg),
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BurgundyPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("Save") }
                }
            }
        }
    }
    if (showCoachDirectory) {
        CoachDirectoryScreen(
            onBack = { showCoachDirectory = false },
            onSelectForTransfer = { id ->
                transferTargetCoachId = id
                showCoachDirectory = false
            },
            modifier = Modifier.fillMaxSize()
        )
    }
    }
}

@Composable
private fun PrivSwitchRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
        )
    }
}

@Composable
private fun CategoryTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    androidx.compose.material3.Card(
        modifier = modifier
            .aspectRatio(1.55f)
            .clickable(enabled = enabled, onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = container)
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
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WideCategoryTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container = if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = container)
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
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
