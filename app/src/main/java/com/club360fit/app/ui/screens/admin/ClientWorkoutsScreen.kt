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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.data.WorkoutPlanRepository
import com.club360fit.app.data.WorkoutSessionLogDto
import com.club360fit.app.data.WorkoutSessionLogRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientWorkoutsScreen(
    clientId: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<WorkoutPlanDto>>(emptyList()) }
    var sessionLogs by remember { mutableStateOf<List<WorkoutSessionLogDto>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    // B3: coach reply state
    var replyingToLog by remember { mutableStateOf<WorkoutSessionLogDto?>(null) }
    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId, refreshKey) {
        isLoading = true
        error = null
        try {
            plans = WorkoutPlanRepository.getAllPlans(clientId)
            sessionLogs = WorkoutSessionLogRepository.fetchForClient(clientId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load workout data"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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

                // -- Workout Plans section --
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

                // -- B3: Session Logs with notes section --
                val logsWithNotes = sessionLogs.filter { !it.noteToCoach.isNullOrBlank() || !it.coachReply.isNullOrBlank() }
                if (logsWithNotes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Workout Notes",
                        style = MaterialTheme.typography.titleSmall,
                        color = BurgundyPrimary
                    )
                    logsWithNotes.forEach { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = log.sessionDate.toDisplayDate(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!log.noteToCoach.isNullOrBlank()) {
                                    Text(
                                        text = "\uD83D\uDCAC Client: ${log.noteToCoach}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (!log.coachReply.isNullOrBlank()) {
                                    Text(
                                        text = "\uD83D\uDCDD Coach: ${log.coachReply}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BurgundyPrimary
                                    )
                                    log.coachRepliedAt?.let { ts ->
                                        Text(
                                            text = ts.take(10),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (!log.noteToCoach.isNullOrBlank()) {
                                    TextButton(
                                        onClick = {
                                            replyingToLog = log
                                            replyText = log.coachReply ?: ""
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
    }

    // -- B3: Edit Plan Dialog (unchanged) --
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

    // -- B3: Coach reply dialog --
    replyingToLog?.let { log ->
        AlertDialog(
            onDismissRequest = { if (!isSendingReply) { replyingToLog = null; replyText = "" } },
            title = { Text("Reply to workout note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Client note: ${log.noteToCoach}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Your reply") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 5,
                        enabled = !isSendingReply
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = log.id ?: return@TextButton
                        val text = replyText.trim()
                        if (text.isBlank()) return@TextButton
                        scope.launch {
                            isSendingReply = true
                            try {
                                WorkoutSessionLogRepository.replyToWorkoutNote(id, text)
                                replyingToLog = null
                                replyText = ""
                                refreshKey++
                                snackbarHostState.showSnackbar(
                                    "Reply sent",
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Failed to send reply: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isSendingReply = false
                            }
                        }
                    },
                    enabled = !isSendingReply && replyText.isNotBlank()
                ) {
                    Text(if (isSendingReply) "Sending\u2026" else "Send reply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { replyingToLog = null; replyText = "" },
                    enabled = !isSendingReply
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
