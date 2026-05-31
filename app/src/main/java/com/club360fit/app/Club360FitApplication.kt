package com.club360fit.app

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.club360fit.app.worker.ClientAdherenceWorker
import com.club360fit.app.worker.PaymentDueReminderWorker
import com.club360fit.app.worker.ScheduleNotificationWorker
import java.util.concurrent.TimeUnit

/**
 * Application class for Club 360 Fit. Schedules notification worker for upcoming/past-due events.
 */
class Club360FitApplication : Application() {
    private companion object {
        const val TAG = "Club360Push"
    }

    override fun onCreate() {
        super.onCreate()
        initializeFirebaseIfConfigured()
        scheduleNotificationWorker()
        scheduleClientAdherenceWorker()
        schedulePaymentDueReminderWorker()
    }

    private fun initializeFirebaseIfConfigured() {
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            debugLog("Firebase already initialized")
            return
        }
        val appId = BuildConfig.FIREBASE_ANDROID_APP_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val senderId = BuildConfig.FIREBASE_SENDER_ID
        if (appId.isBlank() || apiKey.isBlank() || projectId.isBlank() || senderId.isBlank()) {
            debugLog("Firebase not initialized: one or more BuildConfig Firebase values are blank")
            return
        }

        val options = FirebaseOptions.Builder()
            .setApplicationId(appId)
            .setApiKey(apiKey)
            .setProjectId(projectId)
            .setGcmSenderId(senderId)
            .build()
        FirebaseApp.initializeApp(this, options)
        debugLog("Firebase initialized from BuildConfig")
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message)
        }
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

    private fun scheduleClientAdherenceWorker() {
        val request = PeriodicWorkRequestBuilder<ClientAdherenceWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "client_adherence",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun schedulePaymentDueReminderWorker() {
        val request = PeriodicWorkRequestBuilder<PaymentDueReminderWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "payment_due_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
