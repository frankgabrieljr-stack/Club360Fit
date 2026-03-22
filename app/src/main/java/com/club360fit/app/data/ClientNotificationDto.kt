package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientNotificationDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    val kind: String = "info",
    val title: String = "",
    val body: String = "",
    @SerialName("dedupe_key") val dedupeKey: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
