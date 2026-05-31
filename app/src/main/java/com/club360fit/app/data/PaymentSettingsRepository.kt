package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaymentSettingsRepository {
    private val client = SupabaseClient.client

    suspend fun getForClient(clientId: String): ClientPaymentSettingsDto? = withContext(Dispatchers.IO) {
        client.postgrest["client_payment_settings"]
            .select {
                filter { eq("client_id", clientId) }
                limit(1)
            }
            .decodeList<ClientPaymentSettingsDto>()
            .firstOrNull()
    }

    suspend fun upsert(settings: ClientPaymentSettingsDto) = withContext(Dispatchers.IO) {
        client.postgrest["client_payment_settings"].upsert(settings)
    }

    /**
     * After coach logs or approves a payment, bump [ClientPaymentSettingsDto.nextDueDate]
     * when [dueRecurrence] is weekly or monthly.
     */
    suspend fun advanceRecurringDueDate(clientId: String) = withContext(Dispatchers.IO) {
        val settings = getForClient(clientId) ?: return@withContext
        val due = settings.nextDueDate ?: return@withContext
        val next = when (settings.dueRecurrence.lowercase()) {
            "weekly" -> due.plusWeeks(1)
            "monthly" -> due.plusMonths(1)
            else -> return@withContext
        }
        upsert(settings.copy(nextDueDate = next))
    }
}

