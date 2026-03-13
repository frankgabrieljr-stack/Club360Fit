package com.club360fit.app.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.theme.White
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ResetPasswordScreen(
    onPasswordResetDone: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Set a new password",
            style = MaterialTheme.typography.headlineSmall,
            color = BurgundyPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter your new password below. After saving, you'll be signed in.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BurgundyPrimary,
                focusedLabelColor = BurgundyPrimary,
                cursorColor = BurgundyPrimary
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BurgundyPrimary,
                focusedLabelColor = BurgundyPrimary,
                cursorColor = BurgundyPrimary
            )
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (newPassword.isBlank() || confirmPassword.isBlank()) {
                    error = "Please enter and confirm your new password."
                    return@Button
                }
                if (newPassword != confirmPassword) {
                    error = "Passwords do not match."
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    error = null
                    try {
                        val isAdmin = withContext(Dispatchers.IO) {
                            val client = SupabaseClient.client
                            client.auth.updateUser {
                                password = newPassword
                            }
                            // Fetch user metadata to determine role
                            val user = client.auth.retrieveUserForCurrentSession(updateSession = true)
                            val role = user.userMetadata?.get("role")?.jsonPrimitive?.contentOrNull
                            role == "admin"
                        }
                        isLoading = false
                        onPasswordResetDone(isAdmin)
                    } catch (e: Exception) {
                        isLoading = false
                        error = e.message ?: "Failed to update password."
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BurgundyPrimary, contentColor = White)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    color = White
                )
            } else {
                Text("Save new password")
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BurgundyPrimary
            )
        ) {
            Text("Cancel")
        }
    }
}

