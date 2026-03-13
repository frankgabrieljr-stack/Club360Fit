package com.club360fit.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.ClientDto
import com.club360fit.app.data.ClientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminHomeUiState(
    val clients: List<ClientDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminHomeUiState())
    val uiState: StateFlow<AdminHomeUiState> = _uiState.asStateFlow()

    init {
        loadClients()
    }

    fun loadClients() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val clients = ClientRepository.getClients()
                _uiState.value = AdminHomeUiState(clients = clients, isLoading = false)
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
