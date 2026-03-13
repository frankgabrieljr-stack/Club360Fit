package com.club360fit.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.club360fit.app.ui.theme.BurgundyPrimary

@Composable
fun ClientHomeScreen(
    onSignOut: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: ClientHomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var showMealDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Client Home",
                style = MaterialTheme.typography.headlineLarge,
                color = BurgundyPrimary
            )
            IconButton(onClick = onOpenProfile) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "My profile",
                    tint = BurgundyPrimary
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurgundyPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Next session", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        val next = state.nextSession
                        if (next == null) {
                            Text("No upcoming sessions scheduled.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("${next.date} at ${next.time}", style = MaterialTheme.typography.bodyLarge)
                            if (next.notes.isNotBlank()) {
                                Text(next.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.workoutPlan != null) { showWorkoutDialog = true }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("This week's workout", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        Text(
                            text = state.workoutPlan?.title ?: "No workout plan assigned yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.workoutPlan != null) {
                            Text(
                                text = "Tap to view full details",
                                style = MaterialTheme.typography.labelSmall,
                                color = BurgundyPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.mealPlan != null) { showMealDialog = true }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("This week's meals", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                        Text(
                            text = state.mealPlan?.title ?: "No meal plan assigned yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.mealPlan != null) {
                            Text(
                                text = "Tap to view full details",
                                style = MaterialTheme.typography.labelSmall,
                                color = BurgundyPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("Sign out")
                }
            }
        }
    }

    if (showWorkoutDialog && state.workoutPlan != null) {
        AlertDialog(
            onDismissRequest = { showWorkoutDialog = false },
            title = { Text(state.workoutPlan!!.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Week start: ${state.workoutPlan!!.weekStart}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(state.workoutPlan!!.planText, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showWorkoutDialog = false }) {
                    Text("Close", color = BurgundyPrimary)
                }
            }
        )
    }

    if (showMealDialog && state.mealPlan != null) {
        AlertDialog(
            onDismissRequest = { showMealDialog = false },
            title = { Text(state.mealPlan!!.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Week start: ${state.mealPlan!!.weekStart}", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(state.mealPlan!!.planText, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showMealDialog = false }) {
                    Text("Close", color = BurgundyPrimary)
                }
            }
        )
    }
}

@Composable
private fun Box(modifier: Modifier, contentAlignment: Alignment, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = contentAlignment) {
        content()
    }
}
