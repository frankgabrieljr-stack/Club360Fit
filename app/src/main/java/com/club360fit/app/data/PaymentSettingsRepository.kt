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
}

