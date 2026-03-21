package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentRecordDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    @SerialName("amount_label") val amountLabel: String? = null,
    @SerialName("amount_cents") val amountCents: Int? = null,
    /** ISO-8601 timestamptz from Postgres */
    @SerialName("paid_at") val paidAt: String,
    val method: String = "other",
    val note: String = "",
    @SerialName("recorded_by") val recordedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
