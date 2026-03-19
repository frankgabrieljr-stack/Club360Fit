package com.club360fit.app.ui.screens.client

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.ClientPaymentSettingsDto
import com.club360fit.app.data.PaymentSettingsRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.qrCodeImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPaymentsScreen(
    clientId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf<ClientPaymentSettingsDto?>(null) }

    LaunchedEffect(clientId) {
        isLoading = true
        error = null
        try {
            settings = PaymentSettingsRepository.getForClient(clientId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load payment details"
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                val s = settings
                if (s == null) {
                    Text(
                        text = "No payment details have been added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    return@Column
                }

                s.note.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val venmoUrl = s.venmoUrl?.takeIf { it.isNotBlank() }
                if (venmoUrl != null) {
                    Text("Venmo", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                    Button(
                        onClick = { openUrl(context, venmoUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Venmo link")
                    }
                    Image(
                        bitmap = qrCodeImageBitmap(venmoUrl, sizePx = 560),
                        contentDescription = "Venmo QR",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(top = 8.dp)
                    )
                }

                val zelleEmail = s.zelleEmail?.takeIf { it.isNotBlank() }
                val zellePhone = s.zellePhone?.takeIf { it.isNotBlank() }
                if (zelleEmail != null || zellePhone != null) {
                    Text("Zelle", style = MaterialTheme.typography.titleMedium, color = BurgundyPrimary)
                    if (zelleEmail != null) {
                        CopyRow(label = "Email", value = zelleEmail, onCopy = { copyToClipboard(context, "Zelle email", zelleEmail) })
                        Image(
                            bitmap = qrCodeImageBitmap(zelleEmail, sizePx = 560),
                            contentDescription = "Zelle email QR",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .padding(top = 8.dp)
                        )
                    }
                    if (zellePhone != null) {
                        CopyRow(label = "Phone", value = zellePhone, onCopy = { copyToClipboard(context, "Zelle phone", zellePhone) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}

