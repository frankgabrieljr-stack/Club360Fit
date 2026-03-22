package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ClientNotificationRepository {
    private val client = SupabaseClient.client

    suspend fun listRecent(clientId: String, limit: Int = 40): List<ClientNotificationDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["client_notifications"]
                .select {
                    filter { eq("client_id", clientId) }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ClientNotificationDto>()
        }

    suspend fun unreadCount(clientId: String): Int = withContext(Dispatchers.IO) {
        listRecent(clientId, 100).count { it.readAt == null }
    }

    suspend fun markRead(notificationId: String) = withContext(Dispatchers.IO) {
        val now = java.time.Instant.now().toString()
        client.postgrest["client_notifications"].update(
            {
                set("read_at", now)
            }
        ) {
            filter { eq("id", notificationId) }
        }
    }

    suspend fun markAllRead(clientId: String) = withContext(Dispatchers.IO) {
        val now = java.time.Instant.now().toString()
        client.postgrest["client_notifications"].update(
            { set("read_at", now) }
        ) {
            filter {
                eq("client_id", clientId)
            }
        }
    }

    /** Inserts; ignores duplicate dedupe_key (unique violation). */
    suspend fun tryInsert(row: ClientNotificationDto) = withContext(Dispatchers.IO) {
        try {
            client.postgrest["client_notifications"].insert(row)
        } catch (_: Exception) {
            /* duplicate dedupe or network */
        }
    }
}
