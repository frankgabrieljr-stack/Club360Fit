package com.club360fit.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest

object ClientSelfRepository {
    private val client = SupabaseClient.client

    /** Current user's client profile row, if any. */
    suspend fun getOwnClient(): ClientDto? {
        val uid = client.auth.currentUserOrNull()?.id ?: return null
        return client.postgrest["clients"]
            .select {
                filter { eq("user_id", uid) }
                limit(1)
            }
            .decodeList<ClientDto>()
            .firstOrNull()
    }

    suspend fun getOwnClientId(): String? = getOwnClient()?.id
}
