package com.club360fit.app.ui.screens.profile

data class UserProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val lastLoginFormatted: String? = null,   // null → hide the line
    val avatarUrl: String? = null,
    val isUploadingAvatar: Boolean = false,
    val roleLabel: String = "Client",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val displayName: String get() = when {
        firstName.isNotBlank() || lastName.isNotBlank() ->
            "$firstName $lastName".trim()
        else -> email.substringBefore("@").ifBlank { "Member" }
    }
}
