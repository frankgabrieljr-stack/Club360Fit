package com.club360fit.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

object PaymentRecordRepository {
    private val client = SupabaseClient.client

    suspend fun listForClient(clientId: String): List<PaymentRecordDto> = withContext(Dispatchers.IO) {
        client.postgrest["payment_records"]
            .select {
                filter { eq("client_id", clientId) }
                order("paid_at", order = Order.DESCENDING)
            }
            .decodeList<PaymentRecordDto>()
    }

    /**
     * Inserts a payment record (coach flow). Returns the new row id (client-generated UUID).
     */
    suspend fun insertForClient(
        clientId: String,
        amountLabel: String?,
        method: String,
        note: String,
        paidAtLocalDate: LocalDate
    ): String = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id
        val paidAt = paidAtLocalDate
            .atTime(LocalTime.NOON)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toString()
        val recordId = UUID.randomUUID().toString()
        val row = PaymentRecordDto(
            id = recordId,
            clientId = clientId,
            amountLabel = amountLabel?.trim()?.takeIf { it.isNotEmpty() },
            paidAt = paidAt,
            method = method,
            note = note.trim(),
            recordedBy = uid
        )
        client.postgrest["payment_records"].insert(row)
        recordId
    }

    suspend fun deleteRecord(id: String) = withContext(Dispatchers.IO) {
        client.postgrest["payment_records"].delete {
            filter { eq("id", id) }
        }
    }
}

/** User-facing display for ISO timestamps from Supabase. */
/** Parse Postgres/ISO timestamps to a local date (for linking confirmations → payment date). */
fun parseIsoToLocalDate(iso: String): LocalDate = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
} catch (_: Exception) {
    try {
        OffsetDateTime.parse(iso).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (_: Exception) {
        LocalDate.now()
    }
}

fun formatPaymentInstant(iso: String): String = try {
    val instant = Instant.parse(iso)
    instant.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
} catch (_: Exception) {
    try {
        java.time.OffsetDateTime.parse(iso).toInstant()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
    } catch (_: Exception) {
        iso
    }
}
