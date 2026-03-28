package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.ClientNotificationDto
import com.club360fit.app.data.ClientNotificationRepository
import com.club360fit.app.data.ClientRepository
import com.club360fit.app.data.WorkoutSessionLogRepository
import com.club360fit.app.data.formatPaymentInstant
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.SubmitResultMessages
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachHubNotificationsScreen(
    onBack: () -> Unit,
    onUnreadChanged: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<ClientNotificationDto>>(emptyList()) }
    var clientNameById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var replyTarget by remember { mutableStateOf<ClientNotificationDto?>(null) }
    var replyDraft by remember { mutableStateOf("") }
    var replyBusy by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true
            try {
                val coachClients = runCatching { ClientRepository.getClients() }.getOrNull().orEmpty()
                val clientIds = coachClients.mapNotNull { it.id }.toSet()
                clientNameById = coachClients
                    .mapNotNull { c ->
                        val id = c.id ?: return@mapNotNull null
                        id to (c.fullName?.ifBlank { null } ?: "(no name)")
                    }
                    .toMap()
                items = ClientNotificationRepository.listForCoach(clientIds, 80)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Updates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BurgundyPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = BurgundyPrimary
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BurgundyPrimary)
            }
        } else if (items.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No coach updates yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id ?: "${it.clientId}_${it.createdAt}" }) { n ->
                    val nId = n.id
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (n.coachReadAt == null) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val clientLabel = clientNameById[n.clientId] ?: n.clientId.take(8)
                            Text(
                                clientLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = BurgundyPrimary
                            )
                            Text(n.title, style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                            Text(n.body, style = MaterialTheme.typography.bodyMedium)
                            n.createdAt?.let {
                                Text(
                                    formatPaymentInstant(it),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val canReply = (n.kind ?: "").lowercase() == "workout_session_logged"
                            if (canReply && nId != null) {
                                TextButton(
                                    onClick = {
                                        replyTarget = n
                                        replyDraft = ""
                                    }
                                ) {
                                    Text("Reply", color = BurgundyPrimary)
                                }
                            }
                            if (nId != null) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            ClientNotificationRepository.markCoachReadAsCoach(nId)
                                            reload()
                                            onUnreadChanged()
                                        }
                                    }
                                ) {
                                    Text("Mark read", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    replyTarget?.let { target ->
        val notificationId = target.id
        AlertDialog(
            onDismissRequest = { if (!replyBusy) replyTarget = null },
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
                                    clientId = target.clientId,
                                    workoutSessionLogId = target.refId,
                                    replyText = replyDraft
                                )
                                notificationId?.let { ClientNotificationRepository.markCoachReadAsCoach(it) }
                                replyTarget = null
                                replyDraft = ""
                                reload()
                                onUnreadChanged()
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
                TextButton(onClick = { if (!replyBusy) replyTarget = null }, enabled = !replyBusy) {
                    Text("Cancel")
                }
            }
        )
    }
}
