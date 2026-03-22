package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DailyHabitLogDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("log_date") val logDate: LocalDate,
    @SerialName("water_done") val waterDone: Boolean = false,
    val steps: Int? = null,
    @SerialName("sleep_hours") val sleepHours: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
