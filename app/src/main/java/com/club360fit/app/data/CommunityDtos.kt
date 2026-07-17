package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityPostDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("author_display_name") val authorDisplayName: String = "Member",
    @SerialName("coach_display_name") val coachDisplayName: String? = "Coach",
    val category: String = "tip",
    val body: String = "",
    @SerialName("created_at") val createdAt: String? = null
) {
    val categoryLabel: String
        get() = when (category.lowercase()) {
            "win" -> "Win"
            "question" -> "Question"
            "encouragement" -> "Cheer"
            else -> "Tip"
        }
}

@Serializable
data class CommunityCommentDto(
    val id: String? = null,
    @SerialName("post_id") val postId: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("author_user_id") val authorUserId: String? = null,
    @SerialName("is_coach_reply") val isCoachReply: Boolean = false,
    @SerialName("author_display_name") val authorDisplayName: String = "Member",
    val body: String = "",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CommunityMemberDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("member_display_name") val memberDisplayName: String = "Member",
    @SerialName("coach_id") val coachId: String,
    @SerialName("coach_display_name") val coachDisplayName: String = "Coach",
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class CommunityPostInsert(
    @SerialName("client_id") val clientId: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("author_display_name") val authorDisplayName: String,
    @SerialName("coach_display_name") val coachDisplayName: String,
    val category: String,
    val body: String
)

@Serializable
data class CommunityCommentInsert(
    @SerialName("post_id") val postId: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("author_user_id") val authorUserId: String,
    @SerialName("is_coach_reply") val isCoachReply: Boolean,
    @SerialName("author_display_name") val authorDisplayName: String,
    val body: String
)
