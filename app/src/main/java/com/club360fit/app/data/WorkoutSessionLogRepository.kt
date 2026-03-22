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

    suspend fun logSession(clientId: String, sessionDate: LocalDate) = withContext(Dispatchers.IO) {
        val row = WorkoutSessionLogDto(
            clientId = clientId,
            sessionDate = sessionDate,
            weekStart = weekStartSunday(sessionDate)
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
