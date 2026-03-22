package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

object DailyHabitRepository {
    private val client = SupabaseClient.client

    suspend fun getForDay(clientId: String, date: LocalDate): DailyHabitLogDto? = withContext(Dispatchers.IO) {
        client.postgrest["daily_habit_logs"]
            .select {
                filter {
                    eq("client_id", clientId)
                    eq("log_date", date.toString())
                }
                limit(1)
            }
            .decodeList<DailyHabitLogDto>()
            .firstOrNull()
    }

    suspend fun listRange(clientId: String, from: LocalDate, to: LocalDate): List<DailyHabitLogDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["daily_habit_logs"]
                .select {
                    filter { eq("client_id", clientId) }
                    order("log_date", order = Order.DESCENDING)
                    limit(200)
                }
                .decodeList<DailyHabitLogDto>()
                .filter { !it.logDate.isBefore(from) && !it.logDate.isAfter(to) }
        }

    suspend fun upsertDay(row: DailyHabitLogDto) = withContext(Dispatchers.IO) {
        val existing = getForDay(row.clientId, row.logDate)
        if (existing?.id != null) {
            client.postgrest["daily_habit_logs"].update(
                {
                    set("water_done", row.waterDone)
                    set("steps", row.steps)
                    set("sleep_hours", row.sleepHours)
                }
            ) {
                filter { eq("id", existing.id!!) }
            }
        } else {
            client.postgrest["daily_habit_logs"].insert(row)
        }
    }
}
