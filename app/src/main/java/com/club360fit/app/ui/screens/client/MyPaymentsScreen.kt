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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.ClientPaymentSettingsDto
import com.club360fit.app.data.PaymentConfirmationDto
import com.club360fit.app.data.PaymentConfirmationRepository
import com.club360fit.app.data.PaymentRecordDto
import com.club360fit.app.data.PaymentRecordRepository
import com.club360fit.app.data.PaymentSettingsRepository
import com.club360fit.app.data.formatPaymentInstant
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.qrCodeImageBitmap
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch

private val clientConfirmMethods = listOf("venmo", "zelle")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPaymentsScreen(
    clientId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf<ClientPaymentSettingsDto?>(null) }
    var records by remember { mutableStateOf<List<PaymentRecordDto>>(emptyList()) }
    var confirmations by remember { mutableStateOf<List<PaymentConfirmationDto>>(emptyList()) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmAmount by remember { mutableStateOf("") }
    var confirmMethod by remember { mutableStateOf("venmo") }
    var confirmNote by remember { mutableStateOf("") }
    var isSubmittingConfirm by remember { mutableStateOf(false) }

    fun reloadPayments() {
        scope.launch {
            try {
                settings = PaymentSettingsRepository.getForClient(clientId)
                records = PaymentRecordRepository.listForClient(clientId)
                confirmations = PaymentConfirmationRepository.listForClient(clientId)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load payment details"
            }
        }
    }

    LaunchedEffect(clientId) {
        isLoading = true
        error = null
        try {
            settings = PaymentSettingsRepository.getForClient(clientId)
            records = PaymentRecordRepository.listForClient(clientId)
            confirmations = PaymentConfirmationRepository.listForClient(clientId)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load payment details"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                val s = settings
                if (s == null) {
                    Text(
                        text = "Your coach hasn’t set up payment info yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {

                val hasDue = s.nextDueDate != null ||
                    !s.nextDueAmount.isNullOrBlank() ||
                    !s.nextDueNote.isNullOrBlank()
                if (hasDue) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Upcoming due",
                                style = MaterialTheme.typography.titleMedium,
                                color = BurgundyPrimary
                            )
                            s.nextDueDate?.let {
                                Text(
                                    "Due: ${it.toDisplayDate()}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            s.nextDueAmount?.takeIf { it.isNotBlank() }?.let {
                                Text("Amount: $it", style = MaterialTheme.typography.bodyMedium)
                            }
                            s.nextDueNote?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                s.note.takeIf { it.isNotBlank() }?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                val venmoUrl = s.venmoUrl?.takeIf { it.isNotBlank() }
                if (venmoUrl != null) {
                    Text("Venmo", style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                    Button(
                        onClick = { openUrl(context, venmoUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Venmo")
                    }
                    Image(
                        bitmap = qrCodeImageBitmap(venmoUrl, sizePx = 560),
                        contentDescription = "Venmo QR",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(top = 8.dp)
                    )
                }

                val zelleEmail = s.zelleEmail?.takeIf { it.isNotBlank() }
                val zellePhone = s.zellePhone?.takeIf { it.isNotBlank() }
                if (zelleEmail != null || zellePhone != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Zelle", style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                    if (zelleEmail != null) {
                        CopyRow(label = "Email", value = zelleEmail, onCopy = { copyToClipboard(context, "Zelle email", zelleEmail) })
                        Image(
                            bitmap = qrCodeImageBitmap(zelleEmail, sizePx = 560),
                            contentDescription = "Zelle email QR",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .padding(top = 8.dp)
                        )
                    }
                    if (zellePhone != null) {
                        CopyRow(label = "Phone", value = zellePhone, onCopy = { copyToClipboard(context, "Zelle phone", zellePhone) })
                    }
                }

                Button(
                    onClick = {
                        confirmAmount = s.nextDueAmount.orEmpty()
                        confirmMethod = "venmo"
                        confirmNote = ""
                        showConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text("I paid")
                }

                if (confirmations.isNotEmpty()) {
                    confirmations.forEach { c ->
                        val statusLabel = when (c.status) {
                            "pending" -> "Pending"
                            "approved" -> "Verified"
                            "declined" -> "Declined"
                            else -> c.status
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        c.amountLabel ?: "Payment",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BurgundyPrimary
                                    )
                                    c.submittedAt?.let {
                                        Text(
                                            formatPaymentInstant(it),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    "${statusLabel} · ${c.method.replaceFirstChar { it.uppercaseChar() }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (c.status) {
                                        "pending" -> MaterialTheme.colorScheme.primary
                                        "approved" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                                if (c.note.isNotBlank()) {
                                    Text(c.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (records.isNotEmpty()) {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleMedium,
                        color = BurgundyPrimary
                    )
                    records.forEach { rec ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        rec.amountLabel ?: "Payment",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BurgundyPrimary
                                    )
                                    Text(
                                        formatPaymentInstant(rec.paidAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    rec.method.replaceFirstChar { it.uppercaseChar() },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (rec.note.isNotBlank()) {
                                    Text(rec.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingConfirm) showConfirmDialog = false },
            title = { Text("Confirm payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = confirmAmount,
                        onValueChange = { confirmAmount = it },
                        label = { Text("Amount") },
                        placeholder = { Text("\$50") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Method", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        clientConfirmMethods.forEach { m ->
                            val selected = confirmMethod == m
                            TextButton(
                                onClick = { confirmMethod = m },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (selected) BurgundyPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(m.replaceFirstChar { it.uppercaseChar() })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = confirmNote,
                        onValueChange = { confirmNote = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isSubmittingConfirm) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = BurgundyPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sending…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSubmittingConfirm,
                    onClick = {
                        scope.launch {
                            isSubmittingConfirm = true
                            try {
                                PaymentConfirmationRepository.submit(
                                    clientId = clientId,
                                    amountLabel = confirmAmount.trim().ifBlank { null },
                                    note = confirmNote,
                                    method = confirmMethod
                                )
                                showConfirmDialog = false
                                reloadPayments()
                                snackbarHostState.showSnackbar(
                                    "Sent",
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    e.message ?: "Could not send confirmation",
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isSubmittingConfirm = false
                            }
                        }
                    }
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isSubmittingConfirm) showConfirmDialog = false },
                    enabled = !isSubmittingConfirm
                ) { Text("Cancel") }
            }
        )
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
