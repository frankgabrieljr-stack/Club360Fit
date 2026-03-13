package com.club360fit.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.club360fit.app.worker.ScheduleNotificationWorker
import java.util.concurrent.TimeUnit

/**
 * Application class for Club 360 Fit. Schedules notification worker for upcoming/past-due events.
 */
class Club360FitApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleNotificationWorker()
    }

    private fun scheduleNotificationWorker() {
        val request = PeriodicWorkRequestBuilder<ScheduleNotificationWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "schedule_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
