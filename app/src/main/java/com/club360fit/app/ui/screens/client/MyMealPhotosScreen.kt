package com.club360fit.app.ui.screens.client

import android.Manifest
import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.club360fit.app.data.MealPhotoDayGroup
import com.club360fit.app.data.MealPhotoLogDto
import com.club360fit.app.data.MealPhotoRepository
import com.club360fit.app.data.MealPhotoSlot
import com.club360fit.app.data.resolvedSlot
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.readBytesFromUri
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMealPhotosScreen(
    clientId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var dayGroups by remember { mutableStateOf<List<MealPhotoDayGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var showAddDialog by remember { mutableStateOf(false) }
    var mealDate by remember { mutableStateOf(LocalDate.now()) }
    var mealSlot by remember { mutableStateOf(MealPhotoSlot.suggestedForNow()) }
    var notesDraft by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            try {
                dayGroups = MealPhotoDayGroup.grouped(MealPhotoRepository.listForClient(clientId))
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    userMessageForMealPhotoError(e),
                    duration = SnackbarDuration.Long
                )
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(clientId) { reload() }

    suspend fun uploadBytes(bytes: ByteArray, filename: String) {
        uploading = true
        try {
            MealPhotoRepository.uploadAndInsert(
                clientId = clientId,
                bytes = bytes,
                logDate = mealDate,
                notes = notesDraft,
                originalFilename = filename,
                mealSlot = mealSlot
            )
            snackbarHostState.showSnackbar("Meal photo uploaded")
            showAddDialog = false
            notesDraft = ""
            mealDate = LocalDate.now()
            mealSlot = MealPhotoSlot.suggestedForNow()
            reload()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(
                userMessageForMealPhotoError(e),
                duration = SnackbarDuration.Long
            )
        } finally {
            uploading = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readBytesFromUri(context, uri) ?: throw IllegalStateException("Could not read image")
            uploadBytes(bytes, "gallery.jpg")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (!success || uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = readBytesFromUri(context, uri) ?: throw IllegalStateException("Could not read photo")
            uploadBytes(bytes, "camera.jpg")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is needed to take a meal photo.")
            }
            return@rememberLauncherForActivityResult
        }
        val dir = File(context.cacheDir, "meal_photos").apply { mkdirs() }
        val photoFile = File.createTempFile("meal_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Meal photos") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    mealDate = LocalDate.now()
                    mealSlot = MealPhotoSlot.suggestedForNow()
                    notesDraft = ""
                    showAddDialog = true
                },
                containerColor = BurgundyPrimary
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Log meal photo")
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading && dayGroups.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BurgundyPrimary)
                    }
                }

                dayGroups.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Your daily meals",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BurgundyPrimary
                        )
                        Text(
                            "Log breakfast, lunch, dinner, and snacks each day so your coach can review portions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            Text(
                                "Your daily meals",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BurgundyPrimary
                            )
                            Text(
                                "Photos are grouped by day — breakfast through snacks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(dayGroups, key = { it.logDate.toString() }) { day ->
                            MemberMealDaySection(
                                day = day,
                                onDelete = { item ->
                                    val logId = item.id ?: return@MemberMealDaySection
                                    scope.launch {
                                        try {
                                            MealPhotoRepository.deleteOwn(clientId, logId)
                                            snackbarHostState.showSnackbar("Meal photo removed")
                                            reload()
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar(
                                                userMessageForMealPhotoError(e),
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!uploading) showAddDialog = false },
            title = { Text("Log meal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Date: ${mealDate.toDisplayDate()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> mealDate = LocalDate.of(y, m + 1, d) },
                            mealDate.year,
                            mealDate.monthValue - 1,
                            mealDate.dayOfMonth
                        ).show()
                    }) { Text("Change date") }

                    Text("Which meal?", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealPhotoSlot.entries.forEach { slot ->
                            FilterChip(
                                selected = mealSlot == slot,
                                onClick = { mealSlot = slot },
                                label = { Text(slot.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notesDraft,
                        onValueChange = { notesDraft = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            enabled = !uploading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Camera")
                        }
                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !uploading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Gallery")
                        }
                    }
                    if (uploading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = BurgundyPrimary)
                            Text("Uploading…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { if (!uploading) showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MemberMealDaySection(
    day: MealPhotoDayGroup,
    onDelete: (MealPhotoLogDto) -> Unit
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
        day.logs.forEach { item ->
            MealPhotoRow(item = item, imageUrl = MealPhotoRepository.publicUrlFor(item.storagePath), onDelete = { onDelete(item) })
        }
    }
}

@Composable
private fun MealPhotoRow(
    item: MealPhotoLogDto,
    imageUrl: String,
    onDelete: () -> Unit
) {
    val slot = item.resolvedSlot
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                slot.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = BurgundyPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(BurgundyPrimary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
        if (!item.notes.isNullOrBlank()) {
            Text(item.notes.orEmpty(), style = MaterialTheme.typography.bodySmall)
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Meal photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        if (!item.coachFeedback.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Coach feedback", style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
                    Text(item.coachFeedback.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    item.coachFeedbackUpdatedAt?.let { iso ->
                        Text(
                            formatCoachFeedbackTimeForClient(iso),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatCoachFeedbackTimeForClient(iso: String): String =
    try {
        val instant = Instant.parse(iso)
        val z = instant.atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("MMM dd yyyy · h:mm a").format(z)
    } catch (_: Exception) {
        iso
    }
