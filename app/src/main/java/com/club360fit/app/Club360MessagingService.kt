package com.club360fit.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.club360fit.app.data.PushRegistrationRepository
import com.club360fit.app.ui.navigation.NotificationDeepLink
import com.club360fit.app.ui.navigation.paymentKindFromPushData
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Club360MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (BuildConfig.DEBUG) {
            Log.i("Club360Push", "Firebase delivered refreshed FCM token")
        }
        CoroutineScope(Dispatchers.IO).launch {
            PushRegistrationRepository.registerAndroidToken(
                token = token,
                deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        if (data.isEmpty()) return

        val title = message.notification?.title ?: data["title"] ?: return
        val body = message.notification?.body ?: data["body"] ?: ""

        ensureChannel()

        val openPayments = paymentKindFromPushData(data)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openPayments) {
                putExtra(NotificationDeepLink.EXTRA_DEEP_LINK, NotificationDeepLink.PAYMENTS)
            }
        }
        val pending = PendingIntent.getActivity(
            this,
            if (openPayments) REQUEST_OPEN_PAYMENTS else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Club360Fit updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Workouts, meals, payments, and schedule" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "club360_updates"
        private const val REQUEST_OPEN_PAYMENTS = 9001
    }
}
