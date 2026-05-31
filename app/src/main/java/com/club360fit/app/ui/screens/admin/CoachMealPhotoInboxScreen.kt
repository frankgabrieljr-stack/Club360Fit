package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.ClientDto
import com.club360fit.app.data.MealPhotoLogDto
import com.club360fit.app.data.MealPhotoRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.theme.Club360Glass
import com.club360fit.app.ui.theme.club360ScreenBackground
import com.club360fit.app.ui.utils.SubmitResultMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ClientMealGroup(
    val clientId: String,
    val displayName: String,
    val logs: List<MealPhotoLogDto>
)

/**
 * Coach-wide meal photo feed (newest activity first), aligned with iOS `CoachMealPhotoInboxView`.
 */
@Composable
fun CoachMealPhotoInboxScreen(
    clients: List<ClientDto>,
    onOpenClientHub: (clientId: String, displayTitle: String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var groups by remember { mutableStateOf<List<ClientMealGroup>>(emptyList()) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val out = mutableListOf<ClientMealGroup>()
                    for (c in clients) {
                        val id = c.id ?: continue
                        val logs = MealPhotoRepository.listForClient(id)
                        if (logs.isNotEmpty()) {
                            val name = c.fullName?.trim()?.takeIf { it.isNotEmpty() } ?: "(no name)"
                            out.add(ClientMealGroup(clientId = id, displayName = name, logs = logs))
                        }
                    }
                    out.sortedByDescending { g ->
                        g.logs.maxOfOrNull { it.createdAt ?: "" } ?: ""
                    }
                }
                groups = loaded
            } catch (e: Exception) {
                error = e.message ?: "Could not load meal inbox"
                groups = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(clients.map { it.id }.toString()) {
        reload()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .club360ScreenBackground()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
        when {
            loading && groups.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            }

            error != null && groups.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 18.dp)
                ) {
                    MealInboxHeader()
                    Spacer(Modifier.height(24.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            groups.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 18.dp)
                ) {
                    MealInboxHeader()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "No meal photos yet. When clients log meals, they appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Club360Glass.captionOnGlass
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        MealInboxHeader()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Review photos from all clients in one place.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Club360Glass.captionOnGlass
                        )
                    }
                    items(groups, key = { it.clientId }) { group ->
                        Text(
                            group.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Club360Glass.burgundy
                        )
                        Button(
                            onClick = { onOpenClientHub(group.clientId, group.displayName) },
                            colors = ButtonDefaults.buttonColors(containerColor = Club360Glass.burgundy),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("Open client hub")
                        }
                        group.logs.forEach { log ->
                            MealPhotoReviewCard(
                                clientId = group.clientId,
                                item = log,
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
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MealInboxHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = Club360Glass.tealDark,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Meal inbox",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Club360Glass.burgundy
            )
            Text(
                text = "Review client meal photos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Club360Glass.burgundy
            )
        }
    }
}
