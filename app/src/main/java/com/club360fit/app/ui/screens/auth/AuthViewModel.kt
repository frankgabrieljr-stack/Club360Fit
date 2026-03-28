package com.club360fit.app.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.BuildConfig
import com.club360fit.app.data.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Auth form state. Used for both sign-in and create-account; optional fields only for create-account.
 */
data class AuthUiState(
    val name: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val medicalConditions: String = "",
    val foodRestrictions: String = "",
    val mealsPerDay: String = "",
    val workoutFrequency: String = "",
    val overallGoal: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resetEmailSent: Boolean = false,
    val resetErrorMessage: String? = null
)

class AuthViewModel : ViewModel() {

    companion object {
        private const val TAG = "Club360Auth"
    }

    private fun friendlyNetworkAuthMessage(throwable: Throwable): String? {
        var t: Throwable? = throwable
        while (t != null) {
            if (t is UnknownHostException) {
                return "Can’t reach Club360Fit servers. Check Wi‑Fi or cellular data. " +
                    "If you’re online, confirm the app was built with the correct Supabase project URL (see project README / dashboard)."
            }
            t = t.cause
        }
        val msg = throwable.message ?: return null
        if (msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("No address associated with hostname", ignoreCase = true)
        ) {
            return "Can’t reach Club360Fit servers (DNS lookup failed). Your Supabase URL is probably fine — " +
                "this often happens on the Android Emulator: try Cold Boot the AVD (Device Manager → ⋮ → Cold Boot), " +
                "toggle the emulator’s Wi‑Fi in Settings → Network, turn off VPN on your Mac, or test on a physical phone. " +
                "If it still fails, confirm Project Settings → API → Project URL matches BuildConfig."
        }
        return null
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateName(value: String) = _uiState.update { it.copy(name = value) }
    fun updateAge(value: String) = _uiState.update { it.copy(age = value) }
    fun updateHeight(value: String) = _uiState.update { it.copy(height = value) }
    fun updateWeight(value: String) = _uiState.update { it.copy(weight = value) }
    fun updatePhone(value: String) = _uiState.update { it.copy(phone = value) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value) }
    fun updateMedicalConditions(value: String) = _uiState.update { it.copy(medicalConditions = value) }
    fun updateFoodRestrictions(value: String) = _uiState.update { it.copy(foodRestrictions = value) }
    fun updateMealsPerDay(value: String) = _uiState.update { it.copy(mealsPerDay = value) }
    fun updateWorkoutFrequency(value: String) = _uiState.update { it.copy(workoutFrequency = value) }
    fun updateOverallGoal(value: String) = _uiState.update { it.copy(overallGoal = value) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearResetMessage() = _uiState.update { it.copy(resetEmailSent = false, resetErrorMessage = null) }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(resetEmailSent = false, resetErrorMessage = null) }
            val trimmed = email.trim()
            if (trimmed.isBlank()) {
                _uiState.update { it.copy(resetErrorMessage = "Enter your email to receive a reset link.") }
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    SupabaseClient.client.auth.resetPasswordForEmail(
                        email = trimmed,
                        redirectUrl = "club360fit://reset"
                    )
                }
                _uiState.update { it.copy(resetEmailSent = true) }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "resetPassword failed; url=${BuildConfig.SUPABASE_URL}", e)
                }
                val shown = friendlyNetworkAuthMessage(e) ?: (e.message ?: "Failed to send reset email.")
                _uiState.update { it.copy(resetErrorMessage = shown) }
            }
        }
    }

    fun submit(isSignIn: Boolean, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = _uiState.value

            if (state.email.isBlank() || state.password.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Email and password are required") }
                return@launch
            }

            try {
                // Run Supabase calls on IO dispatcher
                val isAdminResult = withContext(Dispatchers.IO) {
                    val supabase = SupabaseClient.client
                    if (isSignIn) {
                        supabase.auth.signInWith(Email) {
                            email = state.email.trim()
                            password = state.password
                        }
                    } else {
                        supabase.auth.signUpWith(Email) {
                            email = state.email.trim()
                            password = state.password
                            data = buildJsonObject {
                                put("name", JsonPrimitive(state.name))
                                put("age", JsonPrimitive(state.age))
                                put("height_cm", JsonPrimitive(state.height))
                                put("weight_kg", JsonPrimitive(state.weight))
                                put("phone", JsonPrimitive(state.phone))
                                put("medical_conditions", JsonPrimitive(state.medicalConditions))
                                put("food_restrictions", JsonPrimitive(state.foodRestrictions))
                                put("meals_per_day", JsonPrimitive(state.mealsPerDay))
                                put("workout_frequency", JsonPrimitive(state.workoutFrequency))
                                // Backward/forward compatible: some DB triggers expect `goal`, older ones used `overall_goal`.
                                put("overall_goal", JsonPrimitive(state.overallGoal))
                                put("goal", JsonPrimitive(state.overallGoal))
                                // Coach/admin is assigned in Supabase (Auth → Users → metadata), not from the app.
                                put("role", JsonPrimitive("client"))
                            }
                        }
                    }

                    // Retrieve user info and check role from metadata
                    val user = supabase.auth.retrieveUserForCurrentSession(updateSession = true)
                    val role = user.userMetadata?.get("role")?.jsonPrimitive?.contentOrNull
                    role == "admin"
                }

                _uiState.update { it.copy(isLoading = false) }
                onSuccess(isAdminResult)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "submit failed; url=${BuildConfig.SUPABASE_URL}", e)
                }
                val raw = e.message ?: "Authentication failed"
                val message = friendlyNetworkAuthMessage(e)
                    ?: if (raw.contains("Database error saving new user", ignoreCase = true)) {
                        raw +
                            "\n\nThis is a Supabase database issue (not your form). Often a trigger on auth.users failed. Check Supabase → Logs → Postgres and triggers on auth.users."
                    } else {
                        raw
                    }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        }
    }
}
