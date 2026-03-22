package com.club360fit.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.club360fit.app.data.ClientNotificationDto
import com.club360fit.app.data.ClientNotificationRepository
import com.club360fit.app.data.ClientSelfRepository
import com.club360fit.app.data.ScheduleRepository
import com.club360fit.app.data.WorkoutSessionLogRepository
import java.time.LocalDate

/**
 * Runs periodically for logged-in clients: missed scheduled sessions → in-app row + local notification.
 */
class ClientAdherenceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val client = ClientSelfRepository.getOwnClient() ?: return Result.success()
        val clientId = client.id ?: return Result.success()
        if (!client.canViewEvents) return Result.success()

        val events = ScheduleRepository.getEventsForClient(clientId)
        val yesterday = LocalDate.now().minusDays(1)
        val missed = events.filter {
            it.date == yesterday && !it.isCompleted
        }
        if (missed.isEmpty()) return Result.success()

        val hadWorkout = WorkoutSessionLogRepository.hasSessionOn(clientId, yesterday)
        if (hadWorkout) return Result.success()

        for (ev in missed) {
            val eid = ev.id ?: continue
            val dedupe = "missed_${clientId}_${yesterday}_$eid"
            ClientNotificationRepository.tryInsert(
                ClientNotificationDto(
                    clientId = clientId,
                    kind = "missed_workout",
                    title = "Session not logged",
                    body = "You had \"${ev.title}\" scheduled. Log a workout or update your schedule.",
                    dedupeKey = dedupe
                )
            )
        }
        return Result.success()
    }
}
