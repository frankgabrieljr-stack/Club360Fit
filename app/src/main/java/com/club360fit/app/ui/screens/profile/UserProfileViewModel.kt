package com.club360fit.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.ui.utils.SubmitResultMessages
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UserProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private val supabase = SupabaseClient.client

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val user = withContext(Dispatchers.IO) {
                    supabase.auth.currentUserOrNull()
                } ?: run {
                    _state.value = UserProfileUiState(
                        isLoading = false,
                        errorMessage = "Not signed in"
                    )
                    return@launch
                }
                _state.value = buildStateFromUser(user).copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    private fun buildStateFromUser(user: io.github.jan.supabase.gotrue.user.UserInfo): UserProfileUiState {
        val meta = user.userMetadata

        // Resolve first/last name from metadata or split "name"
        fun metaString(key: String): String? =
            meta?.get(key)?.jsonPrimitive?.content?.takeIf { it != "null" }?.trim('"')

        val nameStr = metaString("name")
        val firstName = metaString("first_name")
            ?: nameStr?.substringBefore(" ")?.takeIf { it.isNotBlank() }
            ?: ""
        val lastName = metaString("last_name")
            ?: nameStr?.substringAfter(" ", "")?.takeIf { it.isNotBlank() }
            ?: ""

        // Avatar URL
        val avatarUrl = metaString("avatar_url") ?: metaString("picture")

        // Role
        val role = metaString("role") ?: "client"
        val roleLabel = if (role == "admin") "Admin" else "Client"

        // Last login formatted (lastSignInAt is Instant in gotrue-kt)
        val lastLoginFormatted = try {
            val lastSignIn = user.lastSignInAt
            if (lastSignIn != null) {
                val javaInstant = java.time.Instant.ofEpochMilli(lastSignIn.toEpochMilliseconds())
                val zoned = javaInstant.atZone(ZoneId.systemDefault())
                zoned.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
            } else null
        } catch (_: Exception) { null }

        return UserProfileUiState(
            firstName = firstName,
            lastName = lastName,
            email = user.email ?: "",
            lastLoginFormatted = lastLoginFormatted,
            avatarUrl = avatarUrl,
            roleLabel = roleLabel
        )
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingAvatar = true, errorMessage = null)
            try {
                val user = supabase.auth.currentUserOrNull()
                    ?: throw IllegalStateException("Not signed in")

                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("Could not read image")

                // Match Supabase auth.uid() casing (Kotlin UUID string may differ from Postgres text).
                val path = "${user.id.lowercase()}/avatar.jpg"
                val bucket = supabase.storage.from(SupabaseClient.AVATARS_BUCKET)

                withContext(Dispatchers.IO) {
                    bucket.upload(path, bytes, upsert = true)
                }

                val publicUrl = bucket.publicUrl(path)

                withContext(Dispatchers.IO) {
                    supabase.auth.updateUser {
                        data = buildJsonObject {
                            put("avatar_url", JsonPrimitive(publicUrl))
                        }
                    }
                }

                loadProfile()
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    uploadSuccessMessage = SubmitResultMessages.UPLOAD_SUCCESS
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    errorMessage = e.message ?: "Upload failed"
                )
            }
        }
    }

    fun clearUploadSuccessMessage() {
        _state.value = _state.value.copy(uploadSuccessMessage = null)
    }
}
