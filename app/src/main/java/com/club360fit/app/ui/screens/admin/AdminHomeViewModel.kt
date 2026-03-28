package com.club360fit.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.ClientDto
import com.club360fit.app.data.ClientRepository
import com.club360fit.app.data.ClientNotificationRepository
import com.club360fit.app.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminHomeUiState(
    val clients: List<ClientDto> = emptyList(),
    /** `clients.user_id` → `public.profiles.role` (`admin` / `client`). */
    val profileRolesByUserId: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminHomeUiState())
    val uiState: StateFlow<AdminHomeUiState> = _uiState.asStateFlow()

    private val _coachUnreadCount = MutableStateFlow(0)
    val coachUnreadCount: StateFlow<Int> = _coachUnreadCount.asStateFlow()

    init {
        loadClients()
    }

    fun refreshCoachUnread() {
        viewModelScope.launch {
            _coachUnreadCount.value = runCatching {
                val ids = ClientRepository.getClients().mapNotNull { it.id }.toSet()
                ClientNotificationRepository.coachUnreadCount(ids)
            }.getOrDefault(0)
        }
    }

    fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val clients = ClientRepository.getClients()
                val userIds = clients.map { it.userId }.filter { it.isNotBlank() }.distinct()
                val roles = ProfileRepository.getRolesForUserIds(userIds)
                _uiState.value = AdminHomeUiState(
                    clients = clients,
                    profileRolesByUserId = roles,
                    isLoading = false
                )
                refreshCoachUnread()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load clients"
                )
            }
        }
    }

    fun deleteClient(id: String) {
        viewModelScope.launch {
            try {
                ClientRepository.deleteClient(id)
                loadClients()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete client"
                )
            }
        }
    }
}
