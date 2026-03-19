package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.club360fit.app.data.ClientPaymentSettingsDto
import com.club360fit.app.data.PaymentSettingsRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientPaymentsScreen(
    clientId: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var venmoUrl by remember { mutableStateOf("") }
    var zelleEmail by remember { mutableStateOf("") }
    var zellePhone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clientId) {
        isLoading = true
        error = null
        try {
            val existing = PaymentSettingsRepository.getForClient(clientId)
            venmoUrl = existing?.venmoUrl.orEmpty()
            zelleEmail = existing?.zelleEmail.orEmpty()
            zellePhone = existing?.zellePhone.orEmpty()
            note = existing?.note.orEmpty()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load payment settings"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payments") },
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Set Venmo and/or Zelle info for this client to use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = venmoUrl,
                    onValueChange = { venmoUrl = it },
                    label = { Text("Venmo link (URL)") },
                    placeholder = { Text("https://venmo.com/...") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = zelleEmail,
                    onValueChange = { zelleEmail = it },
                    label = { Text("Zelle email (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = zellePhone,
                    onValueChange = { zellePhone = it },
                    label = { Text("Zelle phone (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    maxLines = 3
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Spacer(Modifier.height(6.dp))
                Button(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        error = null
                        scope.launch {
                            try {
                                PaymentSettingsRepository.upsert(
                                    ClientPaymentSettingsDto(
                                        clientId = clientId,
                                        venmoUrl = venmoUrl.trim().takeIf { it.isNotBlank() },
                                        zelleEmail = zelleEmail.trim().takeIf { it.isNotBlank() },
                                        zellePhone = zellePhone.trim().takeIf { it.isNotBlank() },
                                        note = note.trim()
                                    )
                                )
                                isSaving = false
                            } catch (e: Exception) {
                                isSaving = false
                                error = e.message ?: "Save failed"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text(if (isSaving) "Saving…" else "Save")
                }
            }
        }
    }
}

