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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.club360fit.app.ui.utils.fromPounds
import com.club360fit.app.ui.utils.toFeetInches
import com.club360fit.app.ui.utils.toPounds
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
    
    var workoutPlans by remember { mutableStateOf<List<WorkoutPlanDto>>(emptyList()) }
    var mealPlans by remember { mutableStateOf<List<MealPlanDto>>(emptyList()) }
    var progressCheckIns by remember { mutableStateOf<List<ProgressCheckInDto>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }
    var upcomingSessions by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }
    var pastSessions by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }

    LaunchedEffect(state.client.id, refreshKey) {
        val id = state.client.id ?: return@LaunchedEffect
        workoutPlans = WorkoutPlanRepository.getAllPlans(id)
        mealPlans = MealPlanRepository.getAllPlans(id)
        progressCheckIns = ProgressRepository.getForClient(id)

        val allSessions = ScheduleRepository.getEventsForClient(id)
        val today = LocalDate.now()
        upcomingSessions = allSessions
            .filter { !it.date.isBefore(today) }
            .sortedWith(compareBy({ it.date }, { it.time }))
        pastSessions = allSessions
            .filter { it.date.isBefore(today) }
            .sortedWith(compareBy({ it.date }, { it.time }))
    }

    Scaffold(
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
                        c.weightKg?.let { Text("Weight: ${it.toPounds()} lbs") }
                        c.foodRestrictions?.takeIf { it.isNotBlank() }?.let { Text("Food Restrictions: $it") }
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
                        subtitle = "${mealPlans.size} plan${if (mealPlans.size == 1) "" else "s"}",
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

                OutlinedTextField(
                    value = state.client.goal ?: "",
                    onValueChange = viewModel::updateGoal,
                    label = { Text("Goal") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "View privileges",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Text(
                    "Control what this client can see in the app (meals, workouts, payments, events).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrivSwitchRow(
                    label = "Nutrition plans",
                    checked = state.client.canViewNutrition,
                    onChecked = { viewModel.updatePrivilege(nutrition = it) }
                )
                PrivSwitchRow(
                    label = "Workout programs",
                    checked = state.client.canViewWorkouts,
                    onChecked = { viewModel.updatePrivilege(workouts = it) }
                )
                PrivSwitchRow(
                    label = "Payments",
                    checked = state.client.canViewPayments,
                    onChecked = { viewModel.updatePrivilege(payments = it) }
                )
                PrivSwitchRow(
                    label = "Run club events",
                    checked = state.client.canViewEvents,
                    onChecked = { viewModel.updatePrivilege(events = it) }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Admin access", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.isAdmin,
                        onCheckedChange = viewModel::setAdmin,
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }
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
                        onClick = { viewModel.save(onDone = onBack) },
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
}

@Composable
private fun PrivSwitchRow(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
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
