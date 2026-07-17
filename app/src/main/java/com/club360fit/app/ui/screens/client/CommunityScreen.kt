package com.club360fit.app.ui.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.ClientSelfRepository
import com.club360fit.app.data.CommunityCommentDto
import com.club360fit.app.data.CommunityPostDto
import com.club360fit.app.data.CommunityRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import kotlinx.coroutines.launch

enum class CommunityViewerMode { MEMBER, COACH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    mode: CommunityViewerMode = CommunityViewerMode.MEMBER,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var posts by remember { mutableStateOf<List<CommunityPostDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var ownClientId by remember { mutableStateOf<String?>(null) }
    var ownCoachId by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf("Member") }
    var showCompose by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<CommunityPostDto?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            try {
                if (mode == CommunityViewerMode.MEMBER) {
                    val client = ClientSelfRepository.getOwnClient()
                    ownClientId = client?.id
                    ownCoachId = client?.coachId
                    displayName = client?.fullName?.trim()?.takeIf { it.isNotEmpty() } ?: "Member"
                } else {
                    displayName = "Coach"
                }
                posts = CommunityRepository.fetchPosts()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Could not load community")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(mode) { reload() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BurgundyPrimary)
                    }
                },
                actions = {
                    if (mode == CommunityViewerMode.MEMBER && !ownCoachId.isNullOrBlank()) {
                        TextButton(onClick = { showCompose = true }) { Text("Post") }
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
            loading && posts.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BurgundyPrimary)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            if (mode == CommunityViewerMode.COACH)
                                "Browse the peer feed and reply with encouragement. Replies show a Coach badge."
                            else
                                "Share tips, wins, and questions. Your coach name shows with each post.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(posts, key = { it.id ?: it.body }) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPost = post },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        post.categoryLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BurgundyPrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(BurgundyPrimary.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                    Text(
                                        post.authorDisplayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(post.body, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Coach: ${post.coachDisplayName ?: "Coach"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCompose && mode == CommunityViewerMode.MEMBER) {
        var category by remember { mutableStateOf("tip") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCompose = false },
            title = { Text("New post") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("tip", "win", "question", "encouragement").forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = { Text(c.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Share something supportive") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cid = ownClientId
                    val coach = ownCoachId
                    if (cid.isNullOrBlank() || coach.isNullOrBlank()) return@TextButton
                    scope.launch {
                        try {
                            CommunityRepository.createPost(
                                clientId = cid,
                                coachId = coach,
                                authorDisplayName = displayName,
                                coachDisplayName = "Coach",
                                category = category,
                                body = body
                            )
                            showCompose = false
                            reload()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(e.message ?: "Could not post")
                        }
                    }
                }) { Text("Post") }
            },
            dismissButton = {
                TextButton(onClick = { showCompose = false }) { Text("Cancel") }
            }
        )
    }

    selectedPost?.let { post ->
        PostDetailDialog(
            post = post,
            mode = mode,
            ownClientId = ownClientId,
            authorDisplayName = displayName,
            onDismiss = { selectedPost = null },
            onChanged = { reload() },
            onError = { scope.launch { snackbarHostState.showSnackbar(it) } }
        )
    }
}

@Composable
private fun PostDetailDialog(
    post: CommunityPostDto,
    mode: CommunityViewerMode,
    ownClientId: String?,
    authorDisplayName: String,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<CommunityCommentDto>>(emptyList()) }
    var reply by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(post.id) {
        loading = true
        comments = runCatching { CommunityRepository.fetchComments(post.id.orEmpty()) }.getOrDefault(emptyList())
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(post.authorDisplayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(post.body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("Replies", style = MaterialTheme.typography.labelLarge, color = BurgundyPrimary)
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = BurgundyPrimary)
                } else {
                    comments.forEach { c ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(c.authorDisplayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                if (c.isCoachReply) {
                                    Text(
                                        "Coach",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(BurgundyPrimary)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(c.body, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (mode == CommunityViewerMode.COACH) "Reply as coach" else "Add a reply") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val postId = post.id ?: return@Button
                scope.launch {
                    try {
                        when (mode) {
                            CommunityViewerMode.MEMBER -> {
                                val cid = ownClientId ?: return@launch
                                CommunityRepository.createMemberComment(postId, cid, authorDisplayName, reply)
                            }
                            CommunityViewerMode.COACH -> {
                                CommunityRepository.createCoachComment(postId, authorDisplayName, reply)
                            }
                        }
                        reply = ""
                        comments = CommunityRepository.fetchComments(postId)
                        onChanged()
                    } catch (e: Exception) {
                        onError(e.message ?: "Could not send reply")
                    }
                }
            }) { Text("Send reply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
