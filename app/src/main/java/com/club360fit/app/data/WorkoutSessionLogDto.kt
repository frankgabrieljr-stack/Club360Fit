package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class WorkoutSessionLogDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("session_date") val sessionDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("week_start") val weekStart: LocalDate,
    @SerialName("created_at") val createdAt: String? = null
)
