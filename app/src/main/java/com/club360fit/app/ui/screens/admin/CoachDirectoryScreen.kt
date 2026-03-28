package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.CoachDirectoryProfileDto
import com.club360fit.app.data.ProfileRepository
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.ui.theme.BurgundyPrimary
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachDirectoryScreen(
    onBack: () -> Unit,
    onSelectForTransfer: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val client = SupabaseClient.client

    var rows by remember { mutableStateOf<List<CoachDirectoryProfileDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copiedId by remember { mutableStateOf<String?>(null) }
    var detailProfile by remember { mutableStateOf<CoachDirectoryProfileDto?>(null) }

    val currentUserId = remember {
        client.auth.currentUserOrNull()?.id?.trim()?.lowercase()
    }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            rows = ProfileRepository.fetchCoachDirectoryProfiles()
        } catch (e: Exception) {
            rows = emptyList()
            errorMessage = e.message ?: "Failed to load coaches"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(copiedId) {
        val id = copiedId ?: return@LaunchedEffect
        delay(1600)
        if (copiedId == id) copiedId = null
    }

    Box(modifier = modifier) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Coaches", color = BurgundyPrimary) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BurgundyPrimary)
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                rows.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No coach profiles",
                            style = MaterialTheme.typography.titleMedium,
                            color = BurgundyPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            text = "Coach accounts appear here when they have admin access in Club360Fit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Each coach’s User ID is their Supabase Auth UUID — use it when transferring a client to them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(rows, key = { it.id }) { row ->
                            CoachDirectoryRow(
                                row = row,
                                currentUserId = currentUserId,
                                copiedId = copiedId,
                                onSelectForTransfer = onSelectForTransfer,
                                onOpenProfile = { detailProfile = row },
                                onCopy = { idLower ->
                                    clipboard.setText(AnnotatedString(idLower))
                                    copiedId = idLower
                                },
                                onUseForTransfer = { idLower ->
                                    onSelectForTransfer?.invoke(idLower)
                                    onBack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
        detailProfile?.let { profile ->
            CoachProfileDetailScreen(
                profile = profile,
                onBack = { detailProfile = null },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@Composable
private fun CoachDirectoryRow(
    row: CoachDirectoryProfileDto,
    currentUserId: String?,
    copiedId: String?,
    onSelectForTransfer: ((String) -> Unit)?,
    onOpenProfile: () -> Unit,
    onCopy: (String) -> Unit,
    onUseForTransfer: (String) -> Unit
) {
    val idLower = row.id.trim().lowercase()
    val selfLower = currentUserId?.trim()?.lowercase()
    val isSelf = selfLower != null && selfLower == idLower
    val displayName = coachDirectoryDisplayName(row)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = CardDefaults.shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (isSelf) {
                    Text(
                        text = "(you)",
                        style = MaterialTheme.typography.labelMedium,
                        color = BurgundyPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            row.email?.trim()?.takeIf { it.isNotEmpty() }?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = idLower,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onOpenProfile) {
                    Text("View profile")
                }
                OutlinedButton(onClick = { onCopy(idLower) }) {
                    Text(if (copiedId == idLower) "Copied" else "Copy ID")
                }
                if (onSelectForTransfer != null && !isSelf) {
                    Button(
                        onClick = { onUseForTransfer(idLower) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BurgundyPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Use for transfer")
                    }
                }
            }
        }
    }
}

private fun coachDirectoryDisplayName(row: CoachDirectoryProfileDto): String {
    row.fullName?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val email = row.email?.trim()?.takeIf { it.isNotEmpty() } ?: return "Coach ${row.id.take(8)}…"
    val local = email.substringBefore("@", missingDelimiterValue = email).trim()
    return local.ifBlank { "Coach ${row.id.take(8)}…" }
}
