package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class MealPlanDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("week_start") val weekStart: LocalDate,
    @SerialName("plan_text") val planText: String,
    @SerialName("created_at") val createdAt: String? = null
)
