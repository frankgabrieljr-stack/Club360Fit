package com.club360fit.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

object PaymentConfirmationRepository {
    private val client = SupabaseClient.client

    /** Client submits “I paid” (Venmo/Zelle/etc.). */
    suspend fun submit(
        clientId: String,
        amountLabel: String?,
        note: String,
        method: String
    ) = withContext(Dispatchers.IO) {
        val safeMethod = method.trim().lowercase().ifBlank { "venmo" }
        val row = PaymentConfirmationDto(
            clientId = clientId,
            amountLabel = amountLabel?.trim()?.takeIf { it.isNotEmpty() },
            note = note.trim(),
            method = safeMethod
        )
        client.postgrest["payment_confirmations"].insert(row)
    }

    suspend fun listForClient(clientId: String): List<PaymentConfirmationDto> = withContext(Dispatchers.IO) {
        client.postgrest["payment_confirmations"]
            .select {
                filter { eq("client_id", clientId) }
                order("submitted_at", order = Order.DESCENDING)
            }
            .decodeList<PaymentConfirmationDto>()
    }

    /** Coach: pending confirmations for this client (for review). */
    suspend fun listPendingForClient(clientId: String): List<PaymentConfirmationDto> = withContext(Dispatchers.IO) {
        listForClient(clientId).filter { it.status == "pending" }
    }

    private suspend fun getById(confirmationId: String): PaymentConfirmationDto = withContext(Dispatchers.IO) {
        client.postgrest["payment_confirmations"]
            .select {
                filter { eq("id", confirmationId) }
            }
            .decodeSingle<PaymentConfirmationDto>()
    }

    /**
     * Coach: creates a [payment_records] row from the confirmation and marks it approved.
     */
    suspend fun approve(confirmationId: String, clientId: String) = withContext(Dispatchers.IO) {
        val conf = getById(confirmationId)
        require(conf.clientId == clientId) { "Confirmation does not match client" }
        require(conf.status == "pending") { "This confirmation was already reviewed" }

        val paidAtLocalDate = conf.submittedAt?.let { parseIsoToLocalDate(it) }
            ?: java.time.LocalDate.now()
        val note = buildString {
            if (conf.note.isNotBlank()) append(conf.note)
            if (isNotEmpty()) append(" · ")
            append("Confirmed in app by client")
        }

        val recordId = PaymentRecordRepository.insertForClient(
            clientId = clientId,
            amountLabel = conf.amountLabel,
            method = conf.method,
            note = note,
            paidAtLocalDate = paidAtLocalDate
        )

        val uid = client.auth.currentUserOrNull()?.id
        val reviewedAt = Instant.now().toString()
        client.postgrest["payment_confirmations"].update(
            {
                set("status", "approved")
                set("reviewed_at", reviewedAt)
                set("reviewed_by", uid)
                set("payment_record_id", recordId)
            }
        ) {
            filter { eq("id", confirmationId) }
        }
    }

    /** Coach: mark as declined (does not create a payment record). */
    suspend fun decline(confirmationId: String, clientId: String) = withContext(Dispatchers.IO) {
        val conf = getById(confirmationId)
        require(conf.clientId == clientId) { "Confirmation does not match client" }
        require(conf.status == "pending") { "This confirmation was already reviewed" }

        val uid = client.auth.currentUserOrNull()?.id
        val reviewedAt = Instant.now().toString()
        client.postgrest["payment_confirmations"].update(
            {
                set("status", "declined")
                set("reviewed_at", reviewedAt)
                set("reviewed_by", uid)
                setToNull("payment_record_id")
            }
        ) {
            filter { eq("id", confirmationId) }
        }
    }
}
