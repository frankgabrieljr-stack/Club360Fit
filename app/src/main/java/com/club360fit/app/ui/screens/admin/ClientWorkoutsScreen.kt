package com.club360fit.app.ui.screens.admin

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.WorkoutSessionLogDto
import com.club360fit.app.data.WorkoutSessionLogRepository
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.data.WorkoutPlanRepository
import com.club360fit.app.data.formatPaymentInstant
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.SubmitResultMessages
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientWorkoutsScreen(
    clientId: String,
    onBack: () -> Unit,
    showAppBar: Boolean = true
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<WorkoutPlanDto>>(emptyList()) }
    var sessionLogs by remember { mutableStateOf<List<WorkoutSessionLogDto>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var replyTargetId by remember { mutableStateOf<String?>(null) }
    var replyDraft by remember { mutableStateOf("") }
    var replyBusy by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId, refreshKey) {
        isLoading = true
        error = null
        try {
            plans = WorkoutPlanRepository.getAllPlans(clientId)
            val weekStart = WorkoutSessionLogRepository.weekStartSunday(LocalDate.now())
            sessionLogs = WorkoutSessionLogRepository.listForWeek(clientId, weekStart)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load workout data"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showAppBar) {
                TopAppBar(
                    title = { Text("Workout Plans") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            } else {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                plans.forEach { plan ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingId = plan.id
                                showEditDialog = true
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Week of ${plan.weekStart.toDisplayDate()} \u2013 ${plan.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = BurgundyPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = plan.planText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { editingId = plan.id; showEditDialog = true }) {
                                Text("Edit", color = BurgundyPrimary)
                            }
                        }
                    }
                }

                Button(
                    onClick = { editingId = null; showEditDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurgundyPrimary.copy(alpha = 0.1f),
                        contentColor = BurgundyPrimary
                    )
                ) {
                    Text("Add workout plan")
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Session logs (this week)",
                    style = MaterialTheme.typography.titleSmall,
                    color = BurgundyPrimary
                )
                if (sessionLogs.isEmpty()) {
                    Text(
                        "No sessions logged for this week yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    sessionLogs.forEach { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                log.sessionDate.toDisplayDate(),
                                style = MaterialTheme.typography.titleSmall,
                                color = BurgundyPrimary
                            )
                            log.noteToCoach?.takeIf { it.isNotBlank() }?.let { note ->
                                Text(
                                    "Member note: $note",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            log.coachReply?.takeIf { it.isNotBlank() }?.let { rep ->
                                Text(
                                    "Your reply: $rep",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            log.coachRepliedAt?.let { iso ->
                                Text(
                                    formatPaymentInstant(iso),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val logId = log.id
                            if (logId != null && !log.noteToCoach.isNullOrBlank()) {
                                TextButton(
                                    onClick = {
                                        replyTargetId = logId
                                        replyDraft = log.coachReply.orEmpty()
                                    }
                                ) {
                                    Text(
                                        if (log.coachReply.isNullOrBlank()) "Reply" else "Edit reply",
                                        color = BurgundyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditPlanDialog(
            title = "Workout plan",
            clientId = clientId,
            editingPlanId = editingId,
            isWorkout = true,
            onDismiss = { showEditDialog = false; editingId = null },
            onSaved = { showEditDialog = false; editingId = null; refreshKey++ },
            onSubmitResult = { success, message ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message,
                        duration = if (success) SnackbarDuration.Short else SnackbarDuration.Long
                    )
                }
            }
        )
    }

    replyTargetId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { if (!replyBusy) replyTargetId = null },
            title = { Text("Reply to workout note") },
            text = {
                OutlinedTextField(
                    value = replyDraft,
                    onValueChange = { replyDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your reply") },
                    minLines = 3,
                    maxLines = 6,
                    enabled = !replyBusy
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            replyBusy = true
                            try {
                                WorkoutSessionLogRepository.replyToWorkoutNote(
                                    clientId = clientId,
                                    workoutSessionLogId = targetId,
                                    replyText = replyDraft
                                )
                                replyTargetId = null
                                replyDraft = ""
                                refreshKey++
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    SubmitResultMessages.failure(e),
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                replyBusy = false
                            }
                        }
                    },
                    enabled = !replyBusy && replyDraft.isNotBlank()
                ) { Text("Send") }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!replyBusy) replyTargetId = null },
                    enabled = !replyBusy
                ) { Text("Cancel") }
            }
        )
    }
}
