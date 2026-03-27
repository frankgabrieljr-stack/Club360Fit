package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Minimal row for `public.profiles` role lookups (coach/admin view). */
@Serializable
data class ProfileRoleDto(
    val id: String? = null,
    val role: String = "client"
)
