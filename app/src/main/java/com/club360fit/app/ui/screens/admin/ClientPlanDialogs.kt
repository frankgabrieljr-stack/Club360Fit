package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.MealPlanDto
import com.club360fit.app.data.MealPlanRepository
import com.club360fit.app.data.ProgressCheckInDto
import com.club360fit.app.data.ProgressRepository
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.data.WorkoutPlanRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlanDialog(
    title: String,
    clientId: String,
    editingPlanId: String?,
    isWorkout: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {}
) {
    var weekStartText by remember { mutableStateOf(LocalDate.now().toString()) }
    var planTitle by remember { mutableStateOf("") }
    var planText by remember { mutableStateOf("") }
    var expectedSessionsText by remember { mutableStateOf("4") }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId, isWorkout, editingPlanId) {
        if (editingPlanId != null) {
            if (isWorkout) {
                WorkoutPlanRepository.getPlanById(editingPlanId)?.let {
                    planTitle = it.title
                    planText = it.planText
                    weekStartText = it.weekStart.toString()
                    expectedSessionsText = it.expectedSessions.coerceIn(1, 14).toString()
                }
            } else {
                MealPlanRepository.getPlanById(editingPlanId)?.let {
                    planTitle = it.title
                    planText = it.planText
                    weekStartText = it.weekStart.toString()
                }
            }
        } else {
            weekStartText = LocalDate.now().toString()
            planTitle = ""
            planText = ""
            expectedSessionsText = "4"
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
                if (isWorkout) {
                    OutlinedTextField(
                        value = expectedSessionsText,
                        onValueChange = { expectedSessionsText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Sessions per week (for completion %)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
                                val exp = expectedSessionsText.toIntOrNull()?.coerceIn(1, 14) ?: 4
                                WorkoutPlanRepository.upsertPlan(
                                    WorkoutPlanDto(
                                        id = editingPlanId,
                                        clientId = clientId,
                                        title = planTitle,
                                        weekStart = date,
                                        planText = planText,
                                        expectedSessions = exp
                                    )
                                )
                            } else {
                                MealPlanRepository.upsertPlan(
                                    MealPlanDto(
                                        id = editingPlanId,
                                        clientId = clientId,
                                        title = planTitle,
                                        weekStart = date,
                                        planText = planText
                                    )
                                )
                            }
                            isSaving = false
                            onSaved()
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddProgressCheckInDialog(
    clientId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var weightText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var workoutDone by remember { mutableStateOf(false) }
    var mealsFollowed by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Progress check-in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg, optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    maxLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Workout completed?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = workoutDone,
                        onCheckedChange = { workoutDone = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meals followed?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = mealsFollowed,
                        onCheckedChange = { mealsFollowed = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                    if (date == null) {
                        error = "Enter a valid date."
                        return@TextButton
                    }
                    val weight = weightText.toDoubleOrNull()
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            ProgressRepository.addCheckIn(
                                ProgressCheckInDto(
                                    clientId = clientId,
                                    checkInDate = date,
                                    weightKg = weight,
                                    notes = notes,
                                    workoutDone = workoutDone,
                                    mealsFollowed = mealsFollowed
                                )
                            )
                            isSaving = false
                            onSaved()
                            onDismiss()
                        } catch (e: Exception) {
                            isSaving = false
                            error = e.message ?: "Failed to save"
                        }
                    }
                }
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp))
                else Text("Save", color = BurgundyPrimary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

