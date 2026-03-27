package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

object WorkoutSessionLogRepository {
    private val client = SupabaseClient.client

    fun weekStartSunday(d: LocalDate): LocalDate = d.with(DayOfWeek.SUNDAY)

        suspend fun logSession(clientId: String, sessionDate: LocalDate, noteToCoach: String? = null) = withContext(Dispatchers.IO) {
        val row = WorkoutSessionLogDto(
            clientId = clientId,
            sessionDate = sessionDate,
            weekStart = weekStartSunday(sessionDate),
                        noteToCoach = noteToCoach
        )
        try {
            client.postgrest["workout_session_logs"].insert(row)
        } catch (_: Exception) {
            /* duplicate day */
        }
    }

    suspend fun hasSessionOn(clientId: String, date: LocalDate): Boolean = withContext(Dispatchers.IO) {
        client.postgrest["workout_session_logs"]
            .select {
                filter {
                    eq("client_id", clientId)
                    eq("session_date", date.toString())
                }
                limit(1)
            }
            .decodeList<WorkoutSessionLogDto>()
            .isNotEmpty()
    }

    suspend fun countForWeek(clientId: String, weekStart: LocalDate): Int = withContext(Dispatchers.IO) {
        client.postgrest["workout_session_logs"]
            .select {
                filter {
                    eq("client_id", clientId)
                    eq("week_start", weekStart.toString())
                }
            }
            .decodeList<WorkoutSessionLogDto>()
            .size
    }

    suspend fun listForWeek(clientId: String, weekStart: LocalDate): List<WorkoutSessionLogDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["workout_session_logs"]
                .select {
                    filter {
                        eq("client_id", clientId)
                        eq("week_start", weekStart.toString())
                    }
                    order("session_date", order = Order.DESCENDING)
                }
                .decodeList<WorkoutSessionLogDto>()
        }
}

    /** Coach updates coach_reply and coach_replied_at on a session log. */
    suspend fun replyToWorkoutNote(
        sessionLogId: String,
        replyText: String
    ) = withContext(Dispatchers.IO) {
        val trimmed = replyText.trim()
        require(trimmed.isNotEmpty()) { "Reply text must not be empty" }
        val now = java.time.Instant.now().toString()
        client.postgrest["workout_session_logs"].update(
            {
                set("coach_reply", trimmed)
                set("coach_replied_at", now)
            }
        ) {
            filter { eq("id", sessionLogId) }
        }
    }

    /** Fetch recent session logs for a client (coach view). */
    suspend fun fetchForClient(clientId: String, limit: Int = 20): List<WorkoutSessionLogDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["workout_session_logs"]
                .select {
                    filter { eq("client_id", clientId) }
                    order("session_date", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<WorkoutSessionLogDto>()
        }
