package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentConfirmationDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    @SerialName("amount_label") val amountLabel: String? = null,
    val note: String = "",
    val method: String = "venmo",
    /** ISO-8601 timestamptz from Postgres */
    @SerialName("submitted_at") val submittedAt: String? = null,
    val status: String = "pending",
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("payment_record_id") val paymentRecordId: String? = null
)
