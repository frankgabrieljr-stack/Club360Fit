package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row from `public.profiles` for coach directory; [id] is the Supabase Auth user id (use for transfers). */
@Serializable
data class CoachDirectoryProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val role: String = "client"
)
