package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.MealPlanDto
import com.club360fit.app.data.MealPlanRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toDisplayDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientMealsScreen(
    clientId: String,
    onBack: () -> Unit,
    onOpenMealPhotos: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<MealPlanDto>>(emptyList()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(clientId, refreshKey) {
        isLoading = true
        error = null
        try {
            plans = MealPlanRepository.getAllPlans(clientId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load meal plans"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Plans") },
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

                OutlinedButton(
                    onClick = onOpenMealPhotos,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = BurgundyPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("View meal photos", color = BurgundyPrimary)
                }

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
                            text = "Week of ${plan.weekStart.toDisplayDate()} – ${plan.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = BurgundyPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = plan.planText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                editingId = plan.id
                                showEditDialog = true
                            }) { Text("Edit", color = BurgundyPrimary) }
                        }
                    }
                }

                Button(
                    onClick = {
                        editingId = null
                        showEditDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurgundyPrimary.copy(alpha = 0.1f),
                        contentColor = BurgundyPrimary
                    )
                ) {
                    Text("Add meal plan")
                }
            }
        }
    }

    if (showEditDialog) {
        EditPlanDialog(
            title = "Meal plan",
            clientId = clientId,
            editingPlanId = editingId,
            isWorkout = false,
            onDismiss = {
                showEditDialog = false
                editingId = null
            },
            onSaved = {
                showEditDialog = false
                editingId = null
                refreshKey++
            }
        )
    }
}

