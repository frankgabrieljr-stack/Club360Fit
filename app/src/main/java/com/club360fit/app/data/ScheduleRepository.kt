package com.club360fit.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Schedule events stored in Supabase table schedule_events.
 * Events are scoped by user_id (current user). Exposes eventsFlow for UI and getEventsOnce() for workers.
 */
object ScheduleRepository {

    private val client = SupabaseClient.client
    private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    val eventsFlow: StateFlow<List<ScheduleEvent>> = _events.asStateFlow()

    suspend fun loadEvents() = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id ?: run {
            _events.value = emptyList()
            return@withContext
        }
        _events.value = client.postgrest["schedule_events"].select {
            filter { eq("user_id", uid) }
        }.decodeList<ScheduleEvent>()
    }

    /** One-shot fetch for workers (e.g. notifications). Returns empty if not signed in. */
    suspend fun getEventsOnce(): List<ScheduleEvent> = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
        client.postgrest["schedule_events"].select {
            filter { eq("user_id", uid) }
        }.decodeList<ScheduleEvent>()
    }

    suspend fun addEvent(event: ScheduleEvent) = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Not signed in")
        val withUser = event.copy(userId = uid)
        client.postgrest["schedule_events"].insert(withUser)
        loadEvents()
    }

    suspend fun updateEvent(event: ScheduleEvent) = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Not signed in")
        if (event.userId.isNotBlank() && event.userId != uid) return@withContext
        val withUser = event.copy(userId = uid)
        client.postgrest["schedule_events"].upsert(withUser)
        loadEvents()
    }

    suspend fun deleteEvent(id: String) = withContext(Dispatchers.IO) {
        client.postgrest["schedule_events"].delete {
            filter { eq("id", id) }
        }
        loadEvents()
    }

    suspend fun markCompleted(id: String, completed: Boolean) = withContext(Dispatchers.IO) {
        val event = _events.value.find { it.id == id } ?: return@withContext
        updateEvent(event.copy(isCompleted = completed))
    }
}
