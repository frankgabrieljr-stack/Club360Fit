package com.club360fit.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CommunityRepository {
    private val client get() = SupabaseClient.client

    suspend fun fetchPosts(limit: Int = 50): List<CommunityPostDto> = withContext(Dispatchers.IO) {
        client.postgrest["community_posts"]
            .select {
                order("created_at", order = Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList()
    }

    suspend fun fetchMemberDirectory(): List<CommunityMemberDto> = withContext(Dispatchers.IO) {
        client.postgrest.rpc("fetch_community_member_directory").decodeList()
    }

    suspend fun fetchComments(postId: String): List<CommunityCommentDto> = withContext(Dispatchers.IO) {
        client.postgrest["community_comments"]
            .select {
                filter { eq("post_id", postId) }
                order("created_at", order = Order.ASCENDING)
            }
            .decodeList()
    }

    suspend fun createPost(
        clientId: String,
        coachId: String,
        authorDisplayName: String,
        coachDisplayName: String,
        category: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        val safeCategory = category.lowercase().takeIf {
            it in setOf("tip", "win", "question", "encouragement")
        } ?: "tip"
        val trimmed = body.trim()
        require(trimmed.isNotEmpty())
        client.postgrest["community_posts"].insert(
            CommunityPostInsert(
                clientId = clientId,
                coachId = coachId,
                authorDisplayName = authorDisplayName,
                coachDisplayName = coachDisplayName.ifBlank { "Coach" },
                category = safeCategory,
                body = trimmed
            )
        )
    }

    suspend fun createMemberComment(
        postId: String,
        clientId: String,
        authorDisplayName: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id?.toString()
            ?: error("Not signed in")
        val trimmed = body.trim()
        require(trimmed.isNotEmpty())
        client.postgrest["community_comments"].insert(
            CommunityCommentInsert(
                postId = postId,
                clientId = clientId,
                authorUserId = uid.lowercase(),
                isCoachReply = false,
                authorDisplayName = authorDisplayName,
                body = trimmed
            )
        )
    }

    suspend fun createCoachComment(
        postId: String,
        authorDisplayName: String,
        body: String
    ) = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id?.toString()
            ?: error("Not signed in")
        val trimmed = body.trim()
        require(trimmed.isNotEmpty())
        val name = authorDisplayName.trim().ifBlank { "Coach" }
        client.postgrest["community_comments"].insert(
            CommunityCommentInsert(
                postId = postId,
                clientId = null,
                authorUserId = uid.lowercase(),
                isCoachReply = true,
                authorDisplayName = name,
                body = trimmed
            )
        )
    }

    suspend fun deletePost(id: String) = withContext(Dispatchers.IO) {
        client.postgrest["community_posts"].delete { filter { eq("id", id) } }
    }

    suspend fun deleteComment(id: String) = withContext(Dispatchers.IO) {
        client.postgrest["community_comments"].delete { filter { eq("id", id) } }
    }
}
