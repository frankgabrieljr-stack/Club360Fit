package com.club360fit.app.ui.screens.admin

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.data.WorkoutPlanRepository
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
    onBack: () -> Unit
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
    
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var showMealDialog by remember { mutableStateOf(false) }

    // Read-only summaries for current plans
    var workoutSummary by remember { mutableStateOf<WorkoutPlanDto?>(null) }
    var mealSummary by remember { mutableStateOf<MealPlanDto?>(null) }

    LaunchedEffect(state.client.id) {
        val id = state.client.id ?: return@LaunchedEffect
        workoutSummary = WorkoutPlanRepository.getCurrentPlan(id)
        mealSummary = MealPlanRepository.getCurrentPlan(id)
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
                // Summary section (similar to Sarah mock)
                Text(
                    text = "Client Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                val c = state.client
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    c.age?.let { Text("Age: $it") }
                    c.heightCm?.let { Text("Height: ${it} cm") }
                    c.weightKg?.let { Text("Weight: ${it} kg") }
                    c.goal?.takeIf { it.isNotBlank() }?.let {
                        Text("Overall goal: $it")
                    }
                    c.foodRestrictions?.takeIf { it.isNotBlank() }?.let {
                        Text("Food restrictions: $it")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Meal Plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (mealSummary != null) {
                    Text(
                        text = mealSummary!!.title,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mealSummary!!.planText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "No meal plan assigned yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Workout Plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (workoutSummary != null) {
                    Text(
                        text = workoutSummary!!.title,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = workoutSummary!!.planText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "No workout plan assigned yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Button(
                    onClick = { showWorkoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary.copy(alpha = 0.1f), contentColor = BurgundyPrimary)
                ) { Text("Edit workout plan") }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showMealDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary.copy(alpha = 0.1f), contentColor = BurgundyPrimary)
                ) { Text("Edit meal plan") }

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

    if (showWorkoutDialog && state.client.id != null) {
        EditPlanDialog(
            title = "Workout plan",
            clientId = state.client.id!!,
            isWorkout = true,
            onDismiss = { showWorkoutDialog = false }
        )
    }
    if (showMealDialog && state.client.id != null) {
        EditPlanDialog(
            title = "Meal plan",
            clientId = state.client.id!!,
            isWorkout = false,
            onDismiss = { showMealDialog = false }
        )
    }
}

@Composable
private fun EditPlanDialog(
    title: String,
    clientId: String,
    isWorkout: Boolean,
    onDismiss: () -> Unit
) {
    var weekStartText by remember { mutableStateOf(LocalDate.now().toString()) }
    var planTitle by remember { mutableStateOf("") }
    var planText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId, isWorkout) {
        if (isWorkout) {
            WorkoutPlanRepository.getCurrentPlan(clientId)?.let {
                planTitle = it.title
                planText = it.planText
                weekStartText = it.weekStart.toString()
            }
        } else {
            MealPlanRepository.getCurrentPlan(clientId)?.let {
                planTitle = it.title
                planText = it.planText
                weekStartText = it.weekStart.toString()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = planTitle,
                    onValueChange = { planTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weekStartText,
                    onValueChange = { weekStartText = it },
                    label = { Text("Week start (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = planText,
                    onValueChange = { planText = it },
                    label = { Text("Plan details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    singleLine = false,
                    maxLines = 6
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && planTitle.isNotBlank() && planText.isNotBlank(),
                onClick = {
                    val date = runCatching { LocalDate.parse(weekStartText) }.getOrNull()
                    if (date == null) {
                        error = "Week start must be a valid date."
                        return@TextButton
                    }
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            if (isWorkout) {
                                WorkoutPlanRepository.upsertPlan(
                                    WorkoutPlanDto(
                                        clientId = clientId,
                                        title = planTitle,
                                        weekStart = date,
                                        planText = planText
                                    )
                                )
                            } else {
                                MealPlanRepository.upsertPlan(
                                    MealPlanDto(
                                        clientId = clientId,
                                        title = planTitle,
                                        weekStart = date,
                                        planText = planText
                                    )
                                )
                            }
                            isSaving = false
                            onDismiss()
                        } catch (e: Exception) {
                            isSaving = false
                            error = e.message ?: "Failed to save plan"
                        }
                    }
                }
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp))
                else Text("Save", color = BurgundyPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
