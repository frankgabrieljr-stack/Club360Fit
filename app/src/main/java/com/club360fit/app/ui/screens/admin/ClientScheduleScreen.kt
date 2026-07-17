package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.data.ScheduleRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toDisplayDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScheduleScreen(
    clientId: String,
    onBack: () -> Unit,
    showAppBar: Boolean = true
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var upcoming by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }
    var past by remember { mutableStateOf<List<ScheduleEvent>>(emptyList()) }

    LaunchedEffect(clientId) {
        isLoading = true
        error = null
        try {
            val events = ScheduleRepository.getEventsForClient(clientId)
            val today = LocalDate.now()
            upcoming = events
                .filter { !it.date.isBefore(today) }
                .sortedWith(compareBy({ it.date }, { it.time }))
            past = events
                .filter { it.date.isBefore(today) }
                .sortedWith(compareBy({ it.date }, { it.time }))
        } catch (e: Exception) {
            error = e.message ?: "Failed to load schedule"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            if (showAppBar) {
                TopAppBar(
                    title = { Text("Schedule") },
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

                if (upcoming.isEmpty() && past.isEmpty()) {
                    Text(
                        text = "No sessions scheduled for this client yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (upcoming.isNotEmpty()) {
                        Text("Upcoming", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        upcoming.forEach { s ->
                            Text(
                                text = "${s.date.toDisplayDate()} at ${s.time} – ${s.title}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (s.notes.isNotBlank()) {
                                Text(
                                    text = s.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (past.isNotEmpty()) {
                        Text("Past", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        past.takeLast(20).forEach { s ->
                            Text(
                                text = "${s.date.toDisplayDate()} at ${s.time} – ${s.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

