package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ClientPaymentSettingsDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("venmo_url") val venmoUrl: String? = null,
    @SerialName("zelle_email") val zelleEmail: String? = null,
    @SerialName("zelle_phone") val zellePhone: String? = null,
    val note: String = "",
    /** Shown to client as “upcoming due” (coach sets). */
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("next_due_date") val nextDueDate: LocalDate? = null,
    @SerialName("next_due_amount") val nextDueAmount: String? = null,
    @SerialName("next_due_note") val nextDueNote: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

