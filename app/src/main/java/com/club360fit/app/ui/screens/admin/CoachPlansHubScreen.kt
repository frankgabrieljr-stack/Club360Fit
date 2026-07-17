package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.club360fit.app.ui.theme.BurgundyPrimary

enum class CoachPlansHubTab(val routeKey: String, val label: String) {
    SCHEDULE("schedule", "Schedule"),
    WORKOUTS("workouts", "Workouts"),
    MEALS("meals", "Meals");

    companion object {
        fun fromRoute(raw: String?): CoachPlansHubTab =
            entries.firstOrNull { it.routeKey.equals(raw, ignoreCase = true) } ?: SCHEDULE
    }
}

/**
 * Unified Plans & schedule hub (iOS `CoachPlansHubView`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachPlansHubScreen(
    clientId: String,
    displayTitle: String,
    initialTab: CoachPlansHubTab = CoachPlansHubTab.SCHEDULE,
    onBack: () -> Unit,
    onOpenMealPhotos: () -> Unit
) {
    val tabs = CoachPlansHubTab.entries
    var selected by remember(initialTab) {
        mutableIntStateOf(tabs.indexOf(initialTab).coerceAtLeast(0))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Plans & schedule")
                        Text(
                            displayTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selected) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selected == index,
                        onClick = { selected = index },
                        text = { Text(tab.label) }
                    )
                }
            }
            when (tabs[selected]) {
                CoachPlansHubTab.SCHEDULE -> ClientScheduleScreen(
                    clientId = clientId,
                    onBack = onBack,
                    showAppBar = false
                )
                CoachPlansHubTab.WORKOUTS -> ClientWorkoutsScreen(
                    clientId = clientId,
                    onBack = onBack,
                    showAppBar = false
                )
                CoachPlansHubTab.MEALS -> ClientMealsScreen(
                    clientId = clientId,
                    onBack = onBack,
                    onOpenMealPhotos = onOpenMealPhotos,
                    showAppBar = false
                )
            }
        }
    }
}
