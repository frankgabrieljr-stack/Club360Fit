package com.club360fit.app.ui.screens.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.club360fit.app.data.DailyHabitLogDto
import com.club360fit.app.data.DailyHabitRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDailyHabitsScreen(
    clientId: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var waterDone by remember { mutableStateOf(false) }
    var stepsText by remember { mutableStateOf("") }
    var sleepText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(clientId) {
        isLoading = true
        try {
            DailyHabitRepository.getForDay(clientId, today)?.let { h ->
                waterDone = h.waterDone
                stepsText = h.steps?.toString() ?: ""
                sleepText = h.sleepHours?.toString() ?: ""
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily habits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(
                    today.toDisplayDate(),
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Text(
                    "Log once per day. Any entry counts toward your streak.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Water goal met", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = waterDone,
                        onCheckedChange = { waterDone = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = BurgundyPrimary)
                    )
                }
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Steps (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sleepText,
                    onValueChange = { sleepText = it },
                    label = { Text("Sleep (hours, optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. 7.5") }
                )
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            error = null
                            try {
                                val steps = stepsText.toIntOrNull()
                                val sleep = sleepText.toDoubleOrNull()
                                DailyHabitRepository.upsertDay(
                                    DailyHabitLogDto(
                                        clientId = clientId,
                                        logDate = today,
                                        waterDone = waterDone,
                                        steps = steps,
                                        sleepHours = sleep
                                    )
                                )
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text(if (isSaving) "Saving…" else "Save today")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
