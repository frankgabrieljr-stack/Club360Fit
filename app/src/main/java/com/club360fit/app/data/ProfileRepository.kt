package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfileRepository {
    private val client = SupabaseClient.client

    /** `public.profiles.role` for this auth user (`admin` / `client`). Requires coach JWT with admin role for RLS. */
    suspend fun getRoleForUserId(userId: String): String? = withContext(Dispatchers.IO) {
        val trimmed = userId.trim()
        if (trimmed.isEmpty()) return@withContext null
        try {
            client.postgrest["profiles"]
                .select {
                    filter { eq("id", trimmed) }
                    limit(1)
                }
                .decodeList<ProfileRoleDto>()
                .firstOrNull()
                ?.role
                ?.trim()
                ?.lowercase()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getRolesForUserIds(userIds: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val distinct = userIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (distinct.isEmpty()) return@withContext emptyMap()
        val out = mutableMapOf<String, String>()
        for (uid in distinct) {
            getRoleForUserId(uid)?.let { out[uid] = it }
        }
        out
    }
}
