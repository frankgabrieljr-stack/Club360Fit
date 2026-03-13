package com.club360fit.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object ClientRepository {
    private val client = SupabaseClient.client

    suspend fun getClients(): List<ClientDto> = withContext(Dispatchers.IO) {
        // RLS handles which rows are visible
        client.auth.currentUserOrNull() ?: return@withContext emptyList()
        client.postgrest["clients"]
            .select()
            .decodeList<ClientDto>()
    }

    suspend fun getClient(id: String): ClientDto = withContext(Dispatchers.IO) {
        client.postgrest["clients"].select {
            filter {
                eq("id", id)
            }
        }.decodeSingle<ClientDto>()
    }

    suspend fun upsertClient(dto: ClientDto) = withContext(Dispatchers.IO) {
        // Upsert the client record. In a real app, you might set the coach_id here.
        client.postgrest["clients"].upsert(dto)
    }

    suspend fun deleteClient(id: String) = withContext(Dispatchers.IO) {
        client.postgrest["clients"].delete {
            filter { eq("id", id) }
        }
    }

    /**
     * Updates the metadata for the CURRENTLY logged-in user.
     * Note: Changing other users' roles requires service_role or an Edge Function.
     */
    suspend fun updateSelfRole(isAdmin: Boolean) = withContext(Dispatchers.IO) {
        client.auth.updateUser {
            data = buildJsonObject {
                put("role", JsonPrimitive(if (isAdmin) "admin" else "client"))
            }
        }
    }
}
