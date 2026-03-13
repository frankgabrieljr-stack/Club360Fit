package com.club360fit.app.ui.screens.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.ClientSelfRepository
import com.club360fit.app.data.MealPlanDto
import com.club360fit.app.data.MealPlanRepository
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.data.ScheduleRepository
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.data.WorkoutPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ClientHomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val nextSession: ScheduleEvent? = null,
    val workoutPlan: WorkoutPlanDto? = null,
    val mealPlan: MealPlanDto? = null
)

class ClientHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ClientHomeUiState())
    val uiState: StateFlow<ClientHomeUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = ClientHomeUiState(isLoading = true)
                val clientId = ClientSelfRepository.getOwnClientId()
                    ?: run {
                        _uiState.value = ClientHomeUiState(
                            isLoading = false,
                            error = "No client profile found."
                        )
                        return@launch
                    }
                
                // Load events
                ScheduleRepository.loadEvents()
                val events = ScheduleRepository.eventsFlow.value
                val today = LocalDate.now()
                val upcoming = events
                    .filter { !it.date.isBefore(today) && !it.isCompleted }
                    .sortedWith(compareBy({ it.date }, { it.time }))
                val next = upcoming.firstOrNull()
                
                val workout = WorkoutPlanRepository.getCurrentPlan(clientId)
                val meal = MealPlanRepository.getCurrentPlan(clientId)
                
                _uiState.value = ClientHomeUiState(
                    isLoading = false,
                    nextSession = next,
                    workoutPlan = workout,
                    mealPlan = meal
                )
            } catch (e: Exception) {
                _uiState.value = ClientHomeUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }
}
