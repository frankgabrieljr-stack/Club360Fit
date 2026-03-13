package com.club360fit.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.club360fit.app.data.ScheduleRepository
import java.time.LocalDate

class ScheduleNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val events = ScheduleRepository.getEventsOnce()
        val today = LocalDate.now()
        val pastDue = events.filter { it.isPastDue }
        val upcomingToday = events.filter { !it.isCompleted && it.date == today }

        ensureChannel(applicationContext)

        if (pastDue.isNotEmpty()) {
            showNotification(
                applicationContext,
                id = NOTIFICATION_ID_PAST_DUE,
                title = "Past due sessions",
                text = "${pastDue.size} session(s) past due: ${pastDue.take(2).joinToString { it.title }}${if (pastDue.size > 2) "…" else ""}"
            )
        }
        if (upcomingToday.isNotEmpty()) {
            showNotification(
                applicationContext,
                id = NOTIFICATION_ID_UPCOMING,
                title = "Today's sessions",
                text = "${upcomingToday.size} session(s) today: ${upcomingToday.take(2).joinToString { it.title }}${if (upcomingToday.size > 2) "…" else ""}"
            )
        }

        return Result.success()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Schedule reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Upcoming and past-due schedule events" }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "schedule_reminders"
        private const val NOTIFICATION_ID_PAST_DUE = 1001
        private const val NOTIFICATION_ID_UPCOMING = 1002
    }
}
