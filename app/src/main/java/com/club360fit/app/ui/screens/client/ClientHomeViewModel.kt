package com.club360fit.app.ui.screens.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.ClientSelfRepository
import com.club360fit.app.data.MealPlanDto
import com.club360fit.app.data.MealPlanRepository
import com.club360fit.app.data.ProgressCheckInDto
import com.club360fit.app.data.ProgressRepository
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.data.ScheduleRepository
import com.club360fit.app.data.WorkoutPlanDto
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.data.WorkoutPlanRepository
import com.club360fit.app.data.ClientDto
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ClientHomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    /** First name or email local-part for "Welcome, …" */
    val welcomeName: String = "there",
    val clientId: String? = null,
    val canViewNutrition: Boolean = true,
    val canViewWorkouts: Boolean = true,
    val canViewPayments: Boolean = true,
    val canViewEvents: Boolean = true,
    val nextSession: ScheduleEvent? = null,
    val upcomingSessions: List<ScheduleEvent> = emptyList(),
    val workoutPlan: WorkoutPlanDto? = null,
    val mealPlan: MealPlanDto? = null,
    val workoutPlans: List<WorkoutPlanDto> = emptyList(),
    val mealPlans: List<MealPlanDto> = emptyList(),
    val progressCheckIns: List<ProgressCheckInDto> = emptyList()
)

class ClientHomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ClientHomeUiState())
    val uiState: StateFlow<ClientHomeUiState> = _uiState

    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = ClientHomeUiState(isLoading = true)
                val client = ClientSelfRepository.getOwnClient()
                    ?: run {
                        _uiState.value = ClientHomeUiState(
                            isLoading = false,
                            error = "No client profile found."
                        )
                        return@launch
                    }
                val clientId = client.id
                    ?: run {
                        _uiState.value = ClientHomeUiState(
                            isLoading = false,
                            error = "No client profile found."
                        )
                        return@launch
                    }
                val email = SupabaseClient.client.auth.currentUserOrNull()?.email
                val welcomeName = welcomeNameFrom(client, email)
                
                // Load schedule events attached to this client
                val events = ScheduleRepository.getEventsForClient(clientId)
                val today = LocalDate.now()
                val upcoming = events
                    .filter { !it.date.isBefore(today) && !it.isCompleted }
                    .sortedWith(compareBy({ it.date }, { it.time }))
                val next = upcoming.firstOrNull()
                
                val workout = WorkoutPlanRepository.getCurrentPlan(clientId)
                val meal = MealPlanRepository.getCurrentPlan(clientId)
                val allWorkouts = WorkoutPlanRepository.getAllPlans(clientId)
                val allMeals = MealPlanRepository.getAllPlans(clientId)
                val checkIns = ProgressRepository.getOwnCheckIns(clientId)
                
                _uiState.value = ClientHomeUiState(
                    isLoading = false,
                    welcomeName = welcomeName,
                    clientId = clientId,
                    canViewNutrition = client.canViewNutrition,
                    canViewWorkouts = client.canViewWorkouts,
                    canViewPayments = client.canViewPayments,
                    canViewEvents = client.canViewEvents,
                    nextSession = next,
                    upcomingSessions = upcoming,
                    workoutPlan = workout,
                    mealPlan = meal,
                    workoutPlans = allWorkouts,
                    mealPlans = allMeals,
                    progressCheckIns = checkIns
                )
            } catch (e: Exception) {
                _uiState.value = ClientHomeUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }

    private fun welcomeNameFrom(client: ClientDto, email: String?): String {
        val full = client.fullName?.trim()
        if (!full.isNullOrBlank()) {
            return full.split(Regex("\\s+")).first()
        }
        val local = email?.substringBefore("@")?.trim()
        return if (!local.isNullOrBlank()) local else "there"
    }
}
