package com.club360fit.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.ClientDto
import com.club360fit.app.data.ClientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClientProfileUiState(
    val isLoading: Boolean = false,
    val client: ClientDto = ClientDto(
        id = null,
        userId = "",
        fullName = "",
        goal = ""
    ),
    val isAdmin: Boolean = false,
    val error: String? = null
)

class ClientProfileViewModel(
    private val clientId: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClientProfileUiState(isLoading = clientId != null))
    val uiState: StateFlow<ClientProfileUiState> = _uiState.asStateFlow()

    init {
        if (clientId != null) {
            viewModelScope.launch {
                try {
                    val dto = ClientRepository.getClient(clientId)
                    _uiState.value = ClientProfileUiState(
                        isLoading = false,
                        client = dto,
                        isAdmin = false // Default or load from metadata if possible
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load client"
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            client = _uiState.value.client.copy(fullName = name)
        )
    }

    fun updateGoal(goal: String) {
        _uiState.value = _uiState.value.copy(
            client = _uiState.value.client.copy(goal = goal)
        )
    }

    fun updateHeight(heightCm: Int?) {
        _uiState.value = _uiState.value.copy(
            client = _uiState.value.client.copy(heightCm = heightCm)
        )
    }

    fun updateWeight(weightKg: Int?) {
        _uiState.value = _uiState.value.copy(
            client = _uiState.value.client.copy(weightKg = weightKg)
        )
    }

    fun updateAge(age: Int?) {
        _uiState.value = _uiState.value.copy(
            client = _uiState.value.client.copy(age = age)
        )
    }

    fun updatePrivilege(
        nutrition: Boolean? = null,
        workouts: Boolean? = null,
        payments: Boolean? = null,
        events: Boolean? = null
    ) {
        val c = _uiState.value.client
        _uiState.value = _uiState.value.copy(
            client = c.copy(
                canViewNutrition = nutrition ?: c.canViewNutrition,
                canViewWorkouts = workouts ?: c.canViewWorkouts,
                canViewPayments = payments ?: c.canViewPayments,
                canViewEvents = events ?: c.canViewEvents
            )
        )
    }

    fun setAdmin(isAdmin: Boolean) {
        _uiState.value = _uiState.value.copy(isAdmin = isAdmin)
    }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        val dto = state.client
        
        viewModelScope.launch {
            try {
                ClientRepository.upsertClient(dto)
                // Note: role management should be done via Supabase Dashboard for now
                // to avoid the admin user accidentally changing their own role.
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Save failed"
                )
            }
        }
    }
}
