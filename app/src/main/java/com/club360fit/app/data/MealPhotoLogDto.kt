package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class MealPhotoLogDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("log_date") val logDate: LocalDate,
    @SerialName("storage_path") val storagePath: String,
    /** DB may return JSON null; treat as empty in UI via .orEmpty() */
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
