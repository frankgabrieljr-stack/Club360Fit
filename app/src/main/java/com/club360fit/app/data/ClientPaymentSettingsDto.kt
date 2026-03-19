package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientPaymentSettingsDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("venmo_url") val venmoUrl: String? = null,
    @SerialName("zelle_email") val zelleEmail: String? = null,
    @SerialName("zelle_phone") val zellePhone: String? = null,
    val note: String = "",
    @SerialName("updated_at") val updatedAt: String? = null
)

