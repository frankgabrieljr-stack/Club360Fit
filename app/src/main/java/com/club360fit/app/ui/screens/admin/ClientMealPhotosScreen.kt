package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.MealPhotoDayGroup
import com.club360fit.app.data.MealPhotoRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.SubmitResultMessages
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientMealPhotosScreen(
    clientId: String,
    onBack: () -> Unit
) {
    var dayGroups by remember { mutableStateOf<List<MealPhotoDayGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                dayGroups = MealPhotoDayGroup.grouped(MealPhotoRepository.listForClient(clientId))
            } catch (e: Exception) {
                error = e.message ?: "Could not load meal photos"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(clientId) { reload() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Client meal photos") },
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
        when {
            loading && dayGroups.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            }

            error != null && dayGroups.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            dayGroups.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No meal photos from this client yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            "Daily meal log — leave feedback on any meal that needs a tweak.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(dayGroups, key = { it.logDate.toString() }) { day ->
                        CoachMealDaySection(
                            day = day,
                            clientId = clientId,
                            onSaved = {
                                reload()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        SubmitResultMessages.SAVED_SUCCESS,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onError = { msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        SubmitResultMessages.failure(msg),
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CoachMealDaySection(
    day: MealPhotoDayGroup,
    clientId: String,
    onSaved: () -> Unit,
    onError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(day.displayTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BurgundyPrimary)
                Text(day.logDate.toDisplayDate(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    day.mealCountLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BurgundyPrimary)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                if (day.pendingReviewCount > 0) {
                    Text(
                        "${day.pendingReviewCount} to review",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(BurgundyPrimary.copy(alpha = 0.75f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        if (day.slotsPresent.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                day.slotsPresent.forEach { slot ->
                    Text(
                        slot.label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        day.logs.forEach { log ->
            MealPhotoReviewCard(
                clientId = clientId,
                item = log,
                showsDateHeadline = false,
                onSaved = onSaved,
                onError = onError
            )
        }
    }
}
