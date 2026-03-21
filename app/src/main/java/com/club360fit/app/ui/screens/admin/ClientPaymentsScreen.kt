package com.club360fit.app.ui.screens.admin

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.Add
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
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch
import java.time.LocalDate

private val paymentMethods = listOf("venmo", "zelle", "cash", "other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientPaymentsScreen(
    clientId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var venmoUrl by remember { mutableStateOf("") }
    var zelleEmail by remember { mutableStateOf("") }
    var zellePhone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var nextDueDate by remember { mutableStateOf<LocalDate?>(null) }
    var nextDueAmount by remember { mutableStateOf("") }
    var nextDueNote by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    var records by remember { mutableStateOf<List<PaymentRecordDto>>(emptyList()) }
    var pendingConfirmations by remember { mutableStateOf<List<PaymentConfirmationDto>>(emptyList()) }
    var reviewingId by remember { mutableStateOf<String?>(null) }
    var showLogDialog by remember { mutableStateOf(false) }
    var logAmount by remember { mutableStateOf("") }
    var logMethod by remember { mutableStateOf("venmo") }
    var logNote by remember { mutableStateOf("") }
    var logPaidDate by remember { mutableStateOf(LocalDate.now()) }
    var isLogging by remember { mutableStateOf(false) }

    fun loadAll() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val existing = PaymentSettingsRepository.getForClient(clientId)
                venmoUrl = existing?.venmoUrl.orEmpty()
                zelleEmail = existing?.zelleEmail.orEmpty()
                zellePhone = existing?.zellePhone.orEmpty()
                note = existing?.note.orEmpty()
                nextDueDate = existing?.nextDueDate
                nextDueAmount = existing?.nextDueAmount.orEmpty()
                nextDueNote = existing?.nextDueNote.orEmpty()
                records = PaymentRecordRepository.listForClient(clientId)
                pendingConfirmations = PaymentConfirmationRepository.listPendingForClient(clientId)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(clientId) {
        loadAll()
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
                Text(
                    text = "Payment info",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )

                OutlinedTextField(
                    value = venmoUrl,
                    onValueChange = { venmoUrl = it },
                    label = { Text("Venmo profile URL") },
                    placeholder = { Text("https://venmo.com/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 1
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
                    label = { Text("Note to client (e.g. weekly amount)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                Text(
                    text = "Upcoming due",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurgundyPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        nextDueDate?.toDisplayDate() ?: "No date set",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        val d = nextDueDate ?: LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, y, m, day ->
                                nextDueDate = LocalDate.of(y, m + 1, day)
                            },
                            d.year,
                            d.monthValue - 1,
                            d.dayOfMonth
                        ).show()
                    }) { Text("Pick date") }
                    TextButton(onClick = { nextDueDate = null }) { Text("Clear") }
                }
                OutlinedTextField(
                    value = nextDueAmount,
                    onValueChange = { nextDueAmount = it },
                    label = { Text("Amount (e.g. \$50)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nextDueNote,
                    onValueChange = { nextDueNote = it },
                    label = { Text("Due note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

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
                                        note = note.trim(),
                                        nextDueDate = nextDueDate,
                                        nextDueAmount = nextDueAmount.trim().takeIf { it.isNotBlank() },
                                        nextDueNote = nextDueNote.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                                snackbarHostState.showSnackbar(
                                    "Saved",
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                val msg = e.message ?: "Save failed"
                                error = msg
                                snackbarHostState.showSnackbar(
                                    msg,
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Text(if (isSaving) "Saving…" else "Save")
                }

                if (pendingConfirmations.isNotEmpty()) {
                    Text(
                        text = "Pending review",
                        style = MaterialTheme.typography.titleMedium,
                        color = BurgundyPrimary
                    )
                    pendingConfirmations.forEach { conf ->
                        val cid = conf.id ?: return@forEach
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        conf.amountLabel ?: "Payment",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BurgundyPrimary
                                    )
                                    conf.submittedAt?.let {
                                        Text(
                                            formatPaymentInstant(it),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    conf.method.replaceFirstChar { it.uppercaseChar() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (conf.note.isNotBlank()) {
                                    Text(
                                        conf.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                reviewingId = cid
                                                try {
                                                    PaymentConfirmationRepository.approve(cid, clientId)
                                                    loadAll()
                                                    snackbarHostState.showSnackbar("Added")
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar(
                                                        e.message ?: "Could not approve",
                                                        duration = SnackbarDuration.Long
                                                    )
                                                } finally {
                                                    reviewingId = null
                                                }
                                            }
                                        },
                                        enabled = reviewingId == null,
                                        colors = ButtonDefaults.textButtonColors(contentColor = BurgundyPrimary)
                                    ) {
                                        if (reviewingId == cid) {
                                            CircularProgressIndicator(
                                                Modifier.size(16.dp),
                                                color = BurgundyPrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text("Approve")
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                reviewingId = cid
                                                try {
                                                    PaymentConfirmationRepository.decline(cid, clientId)
                                                    loadAll()
                                                    snackbarHostState.showSnackbar("Declined")
                                                } catch (e: Exception) {
                                                    snackbarHostState.showSnackbar(
                                                        e.message ?: "Could not decline",
                                                        duration = SnackbarDuration.Long
                                                    )
                                                } finally {
                                                    reviewingId = null
                                                }
                                            }
                                        },
                                        enabled = reviewingId == null
                                    ) {
                                        Text("Decline", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        logAmount = ""
                        logMethod = "venmo"
                        logNote = ""
                        logPaidDate = LocalDate.now()
                        showLogDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log manually")
                }

                if (records.isNotEmpty()) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleMedium,
                        color = BurgundyPrimary
                    )
                    records.forEach { rec ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        rec.amountLabel ?: "—",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BurgundyPrimary
                                    )
                                    Text(
                                        formatPaymentInstant(rec.paidAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    rec.method.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLogging) showLogDialog = false },
            title = { Text("Log payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = logAmount,
                        onValueChange = { logAmount = it },
                        label = { Text("Amount (e.g. \$50)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(logPaidDate.toDisplayDate(), style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, day ->
                                    logPaidDate = LocalDate.of(y, m + 1, day)
                                },
                                logPaidDate.year,
                                logPaidDate.monthValue - 1,
                                logPaidDate.dayOfMonth
                            ).show()
                        }) { Text("Date paid") }
                    }
                    Text("Method", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentMethods.forEach { m ->
                            val selected = logMethod == m
                            TextButton(
                                onClick = { logMethod = m },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (selected) BurgundyPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(m.replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = logNote,
                        onValueChange = { logNote = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isLogging) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = BurgundyPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Saving…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isLogging,
                    onClick = {
                        scope.launch {
                            isLogging = true
                            try {
                                PaymentRecordRepository.insertForClient(
                                    clientId = clientId,
                                    amountLabel = logAmount.trim().ifBlank { null },
                                    method = logMethod,
                                    note = logNote,
                                    paidAtLocalDate = logPaidDate
                                )
                                records = PaymentRecordRepository.listForClient(clientId)
                                showLogDialog = false
                                snackbarHostState.showSnackbar("Logged")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    e.message ?: "Could not log payment",
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isLogging = false
                            }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isLogging) showLogDialog = false },
                    enabled = !isLogging
                ) { Text("Cancel") }
            }
        )
    }
}
