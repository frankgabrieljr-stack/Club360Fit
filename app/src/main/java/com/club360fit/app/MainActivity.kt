package com.club360fit.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.PermissionChecker
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.ui.theme.Club360FitTheme
import com.club360fit.app.ui.navigation.Club360FitNavHost
import com.club360fit.app.ui.navigation.Routes
import io.github.jan.supabase.gotrue.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle possible Supabase password-reset deeplink
        SupabaseClient.client.handleDeeplinks(intent)

        val data = intent?.data
        val startDestination = if (data?.scheme == "club360fit" && data.host == "reset") {
            Routes.RESET_PASSWORD
        } else {
            Routes.WELCOME
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
        setContent {
            Club360FitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Club360FitNavHost(startDestination = startDestination)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseClient.client.handleDeeplinks(intent)
    }
}
