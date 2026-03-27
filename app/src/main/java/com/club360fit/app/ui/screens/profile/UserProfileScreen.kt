package com.club360fit.app.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.SubmitResultMessages
import com.club360fit.app.ui.theme.OnBurgundy
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    showTopBarBack: Boolean = true,
    viewModel: UserProfileViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showAvatarEditor by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var coachHeadline by remember { mutableStateOf("") }
    var coachSpecialties by remember { mutableStateOf("") }
    var coachAvailability by remember { mutableStateOf("") }

    LaunchedEffect(state.uploadSuccessMessage) {
        state.uploadSuccessMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearUploadSuccessMessage()
        }
    }

    LaunchedEffect(state.profileSavedMessage) {
        state.profileSavedMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearProfileSavedMessage()
        }
    }

    LaunchedEffect(
        state.firstName,
        state.lastName,
        state.bio,
        state.location,
        state.timezone,
        state.phone,
        state.coachHeadline,
        state.coachSpecialties,
        state.coachAvailability
    ) {
        firstName = state.firstName
        lastName = state.lastName
        bio = state.bio
        location = state.location
        timezone = state.timezone
        phone = state.phone
        coachHeadline = state.coachHeadline
        coachSpecialties = state.coachSpecialties
        coachAvailability = state.coachAvailability
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                SubmitResultMessages.failure(msg),
                duration = SnackbarDuration.Long
            )
        }
    }
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bmp = decodeBitmapFromUri(context, uri)
        if (bmp == null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Unable to load photo. Please try another image.",
                    duration = SnackbarDuration.Long
                )
            }
        } else {
            pendingBitmap = bmp
            showAvatarEditor = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    if (showTopBarBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = BurgundyPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = BurgundyPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        .background(BurgundyPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.avatarUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(state.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = BurgundyPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BurgundyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isUploadingAvatar) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = OnBurgundy
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change photo",
                                modifier = Modifier.size(20.dp),
                                tint = OnBurgundy
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Change photo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { viewModel.removeAvatar() },
                    enabled = !state.isUploadingAvatar
                ) { Text("Remove photo") }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = BurgundyPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                state.lastLoginFormatted?.let { formatted ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last login: $formatted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = CardDefaults.shape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.roleLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = BurgundyPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = CardDefaults.shape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Profile details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Short bio") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location (city, country)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = timezone,
                            onValueChange = { timezone = it },
                            label = { Text("Timezone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (state.roleLabel == "Admin") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Coach details",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = coachHeadline,
                                onValueChange = { coachHeadline = it },
                                label = { Text("Coach headline") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = coachSpecialties,
                                onValueChange = { coachSpecialties = it },
                                label = { Text("Specialties (comma separated)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                            OutlinedTextField(
                                value = coachAvailability,
                                onValueChange = { coachAvailability = it },
                                label = { Text("Availability") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.saveProfileDetails(
                                    firstName = firstName,
                                    lastName = lastName,
                                    bio = bio,
                                    location = location,
                                    timezone = timezone,
                                    phone = phone,
                                    coachHeadline = if (state.roleLabel == "Admin") coachHeadline else null,
                                    coachSpecialties = if (state.roleLabel == "Admin") coachSpecialties else null,
                                    coachAvailability = if (state.roleLabel == "Admin") coachAvailability else null
                                )
                            },
                            enabled = !state.isSavingProfile,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BurgundyPrimary,
                                contentColor = OnBurgundy
                            )
                        ) {
                            Text(if (state.isSavingProfile) "Saving..." else "Save details")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = CardDefaults.shape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Email",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Email changes should use a separate secure flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (state.roleLabel == "Admin") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = CardDefaults.shape
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Coach profile preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = coachHeadline.trim().ifBlank { "Certified Coach" },
                                style = MaterialTheme.typography.titleSmall,
                                color = BurgundyPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Specialties",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = coachSpecialties.trim().ifBlank { "Strength, nutrition, accountability" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Availability",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = coachAvailability.trim().ifBlank { "Mon-Fri, 8am-6pm (local time)" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = CardDefaults.shape
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Coach & admin access",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "New sign-ups are always clients. To promote someone to coach/admin: in Supabase open Authentication → Users, select their account, and under User metadata set the key \"role\" to the string \"admin\", then save. They must sign out and sign in again. The app cannot change another user’s role from the phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                state.errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.loadProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurgundyPrimary,
                        contentColor = OnBurgundy
                    )
                ) {
                    Text("Refresh profile")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Sign out")
                }
            }
        }
    }

    if (showAvatarEditor && pendingBitmap != null) {
        AvatarCropDialog(
            bitmap = pendingBitmap!!,
            onDismiss = {
                showAvatarEditor = false
                pendingBitmap = null
            },
            onUse = { cropped ->
                showAvatarEditor = false
                pendingBitmap = null
                viewModel.uploadAvatarBitmap(cropped)
            }
        )
    }
}

@Composable
private fun AvatarCropDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onUse: (Bitmap) -> Unit
) {
    var userScale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    fun baseScale(size: IntSize): Float {
        if (size.width == 0 || size.height == 0) return 1f
        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val vw = size.width.toFloat()
        val vh = size.height.toFloat()
        return maxOf(vw / bw, vh / bh)
    }

    fun clampOffset(proposed: Offset, scale: Float, size: IntSize): Offset {
        val b = baseScale(size) * scale
        val displayedW = bitmap.width * b
        val displayedH = bitmap.height * b
        val maxX = ((displayedW - size.width) / 2f).coerceAtLeast(0f)
        val maxY = ((displayedH - size.height) / 2f).coerceAtLeast(0f)
        return Offset(
            x = proposed.x.coerceIn(-maxX, maxX),
            y = proposed.y.coerceIn(-maxY, maxY)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pinch to zoom and drag to reposition.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .pointerInput(bitmap, userScale, viewportSize) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (userScale * zoom).coerceIn(1f, 4f)
                                userScale = newScale
                                offset = clampOffset(offset + pan, newScale, viewportSize)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = userScale
                                scaleY = userScale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .onSizeChanged { viewportSize = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cropped = cropBitmapForAvatar(bitmap, userScale, offset, viewportSize)
                    if (cropped != null) onUse(cropped) else onDismiss()
                }
            ) { Text("Use photo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun decodeBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (_: Exception) {
        null
    }
}

private fun cropBitmapForAvatar(bitmap: Bitmap, userScale: Float, offset: Offset, viewport: IntSize): Bitmap? {
    if (viewport.width == 0 || viewport.height == 0) return null

    val bw = bitmap.width.toFloat().coerceAtLeast(1f)
    val bh = bitmap.height.toFloat().coerceAtLeast(1f)
    val vw = viewport.width.toFloat()
    val vh = viewport.height.toFloat()
    val base = maxOf(vw / bw, vh / bh)
    val totalScale = base * userScale.coerceAtLeast(1f)

    val srcSize = minOf(vw, vh) / totalScale
    val srcLeft = (bw / 2f) - (srcSize / 2f) - (offset.x / totalScale)
    val srcTop = (bh / 2f) - (srcSize / 2f) - (offset.y / totalScale)

    val clampedLeft = srcLeft.coerceIn(0f, (bw - srcSize).coerceAtLeast(0f))
    val clampedTop = srcTop.coerceIn(0f, (bh - srcSize).coerceAtLeast(0f))

    val srcRect = Rect(
        clampedLeft.toInt(),
        clampedTop.toInt(),
        (clampedLeft + srcSize).toInt().coerceAtMost(bitmap.width),
        (clampedTop + srcSize).toInt().coerceAtMost(bitmap.height)
    )
    if (srcRect.width() <= 1 || srcRect.height() <= 1) return null

    val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val dst = Rect(0, 0, out.width, out.height)
    canvas.drawBitmap(bitmap, srcRect, dst, null)
    return out
}
