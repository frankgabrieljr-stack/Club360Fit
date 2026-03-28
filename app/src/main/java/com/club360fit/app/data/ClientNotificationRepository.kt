package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

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

    /** All notifications visible to the signed-in coach (newest first). Matches iOS `fetchNotificationsForCoach`. */
    suspend fun listForCoach(limit: Int = 80): List<ClientNotificationDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["client_notifications"]
                .select {
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ClientNotificationDto>()
        }

    /**
     * Coach hub feed scoped to [clientIds] (typically `ClientRepository.getClients()` ids).
     * Defense-in-depth on top of RLS so each coach only sees their roster’s notifications.
     */
    suspend fun listForCoach(clientIds: Set<String>, limit: Int = 80): List<ClientNotificationDto> =
        withContext(Dispatchers.IO) {
            if (clientIds.isEmpty()) return@withContext emptyList()
            listForCoach((limit * 4).coerceAtMost(500).coerceAtLeast(limit))
                .filter { it.clientId in clientIds }
                .take(limit)
        }

    suspend fun coachUnreadCount(): Int = withContext(Dispatchers.IO) {
        listForCoach(400).count { it.coachReadAt == null }
    }

    /** Unread coach notifications for [clientIds] only (matches [listForCoach] scoping). */
    suspend fun coachUnreadCount(clientIds: Set<String>): Int = withContext(Dispatchers.IO) {
        if (clientIds.isEmpty()) return@withContext 0
        listForCoach(clientIds, 400).count { it.coachReadAt == null }
    }

    suspend fun markCoachReadAsCoach(notificationId: String) = withContext(Dispatchers.IO) {
        val now = Instant.now().toString()
        client.postgrest["client_notifications"].update(
            { set("coach_read_at", now) }
        ) {
            filter { eq("id", notificationId) }
        }
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

    /** Permanently removes a row (RLS applies). Requires migration `019_client_notifications_delete_policies`. */
    suspend fun delete(notificationId: String) = withContext(Dispatchers.IO) {
        client.postgrest["client_notifications"].delete {
            filter { eq("id", notificationId) }
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

    /** Coach-facing alert: hidden from member `Updates` (`visible_to_client` = false). Aligns with iOS `notifyCoachAboutClient`. */
    suspend fun notifyCoachAboutClient(
        clientId: String,
        kind: String,
        title: String,
        body: String,
        refType: String? = null,
        refId: String? = null,
        dedupeKey: String? = null
    ) {
        tryInsert(
            ClientNotificationDto(
                clientId = clientId,
                kind = kind,
                title = title,
                body = body,
                refType = refType,
                refId = refId,
                dedupeKey = dedupeKey,
                visibleToClient = false
            )
        )
    }

    /** Member-visible notification (`visible_to_client` = true). Aligns with iOS `notifyMemberFromCoach`. */
    suspend fun notifyMemberFromCoach(
        clientId: String,
        kind: String,
        title: String,
        body: String,
        refType: String? = null,
        refId: String? = null,
        dedupeKey: String? = null
    ) {
        tryInsert(
            ClientNotificationDto(
                clientId = clientId,
                kind = kind,
                title = title,
                body = body,
                refType = refType,
                refId = refId,
                dedupeKey = dedupeKey,
                visibleToClient = true
            )
        )
    }
}
