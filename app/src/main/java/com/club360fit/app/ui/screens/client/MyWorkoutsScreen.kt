package com.club360fit.app.ui.screens.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.AdherenceMetricsCalculator
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.data.WorkoutPlanRepository
import com.club360fit.app.data.WorkoutSessionLogRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.SubmitResultMessages
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWorkoutsScreen(
    clientId: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<WorkoutPlanDto>>(emptyList()) }
    var weekLogged by remember { mutableStateOf(0) }
    var weekExpected by remember { mutableStateOf(4) }
    var isLogging by remember { mutableStateOf(false) }
    // A2: optional note to coach
    var workoutNoteForCoach by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val weekStart = AdherenceMetricsCalculator.weekStartSunday(today)

    fun refreshWeek() {
        scope.launch {
            try {
                weekLogged = WorkoutSessionLogRepository.countForWeek(clientId, weekStart)
                weekExpected = WorkoutPlanRepository.getCurrentPlan(clientId)?.expectedSessions?.coerceIn(1, 14) ?: 4
            } catch (_: Exception) { /* ignore */ }
        }
    }

    LaunchedEffect(clientId) {
        isLoading = true
        error = null
        try {
            plans = WorkoutPlanRepository.getAllPlans(clientId)
            weekLogged = WorkoutSessionLogRepository.countForWeek(clientId, weekStart)
            weekExpected = WorkoutPlanRepository.getCurrentPlan(clientId)?.expectedSessions?.coerceIn(1, 14) ?: 4
        } catch (e: Exception) {
            error = e.message ?: "Failed to load workout plans"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Workouts") },
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
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurgundyPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Text("This week", style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                val pct = if (weekExpected <= 0) 0f else (weekLogged.toFloat() / weekExpected.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = pct,
                    modifier = Modifier.fillMaxWidth(),
                    color = BurgundyPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "$weekLogged / $weekExpected sessions \u00b7 ${(pct * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // A2: Optional note to coach field
                OutlinedTextField(
                    value = workoutNoteForCoach,
                    onValueChange = { workoutNoteForCoach = it },
                    label = { Text("Optional note to coach") },
                    placeholder = { Text("e.g. swapped squats due to knee pain") },
                    supportingText = { Text("Sent to your coach with today's workout log.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isLogging
                )

                Button(
                    onClick = {
                        scope.launch {
                            isLogging = true
                            try {
                                WorkoutSessionLogRepository.logSession(
                                    clientId,
                                    today,
                                    noteToCoach = workoutNoteForCoach.trim().ifBlank { null }
                                )
                                workoutNoteForCoach = ""
                                refreshWeek()
                                snackbarHostState.showSnackbar(
                                    SubmitResultMessages.LOGGED_SUCCESS,
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    SubmitResultMessages.failure(e),
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isLogging = false
                            }
                        }
                    },
                    enabled = !isLogging,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text(if (isLogging) "Saving\u2026" else "Log a workout today")
                }

                Spacer(Modifier.height(12.dp))

                if (plans.isEmpty()) {
                    Text(
                        text = "No workout plans assigned yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    plans.forEach { plan ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Week of ${plan.weekStart.toDisplayDate()} \u2013 ${plan.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = BurgundyPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(plan.planText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
