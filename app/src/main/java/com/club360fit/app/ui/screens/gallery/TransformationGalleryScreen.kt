package com.club360fit.app.ui.screens.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.club360fit.app.data.TransformationImage
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.readBytesFromUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TransformationGalleryScreen(
    onBack: () -> Unit,
    viewModel: TransformationGalleryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-advance carousel every 3 seconds
    LaunchedEffect(state.images.size) {
        if (state.images.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(3000)
            currentIndex = if (state.images.isEmpty()) 0 else (currentIndex + 1) % state.images.size
        }
    }

    if (state.error != null) {
        LaunchedEffect(state.error) {
            state.error?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bytes = readBytesFromUri(context, it)
                if (bytes != null) {
                    viewModel.addImage(bytes, "photo.jpg")
                } else {
                    snackbarHostState.showSnackbar("Unable to read image")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transformation Gallery") },
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
            if (state.isAdmin) {
                FloatingActionButton(
                    onClick = {
                        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    containerColor = BurgundyPrimary
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Add photo")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            // higher-contrast vignette style
                            androidx.compose.ui.graphics.Color(0xFF000000),
                            androidx.compose.ui.graphics.Color(0xFF111111),
                            androidx.compose.ui.graphics.Color(0xFF333333),
                            androidx.compose.ui.graphics.Color(0xFF000000)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }

                state.images.isEmpty() -> {
                    Text(
                        text = "No transformations yet. Admins can add photos using the + button.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val img = state.images.getOrNull(currentIndex)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true),
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(
                                targetState = img,
                                animationSpec = tween(durationMillis = 1200)
                            ) { currentImage ->
                                if (currentImage != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(currentImage.url)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Transformation photo",
                                        modifier = Modifier
                                            .fillMaxWidth(0.96f)
                                            .aspectRatio(3f / 4f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (state.images.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (state.images.isNotEmpty()) {
                                            currentIndex =
                                                if (currentIndex == 0) state.images.lastIndex
                                                else currentIndex - 1
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (state.images.isNotEmpty()) {
                                            currentIndex = (currentIndex + 1) % state.images.size
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (state.isAdmin) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Tap a thumbnail to delete.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.images, key = { it.path }) { image ->
                                    ThumbnailWithDelete(
                                        image = image,
                                        isSelected = image == img,
                                        onDelete = { viewModel.deleteImage(image) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailWithDelete(
    image: TransformationImage,
    isSelected: Boolean,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        AsyncImage(
            model = image.url,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

