package com.club360fit.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.data.ScheduleRepository
import com.club360fit.app.ui.utils.SubmitResultMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ScheduleUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val events: List<ScheduleEvent> = emptyList(),
    val selectedDate: LocalDate? = null,
    val eventsForSelectedDate: List<ScheduleEvent> = emptyList(),
    val showAddEventDialog: Boolean = false,
    val addEventDate: LocalDate? = null
)

class ScheduleViewModel : ViewModel() {

    init {
        viewModelScope.launch { ScheduleRepository.loadEvents() }
    }

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _showAddEventDialog = MutableStateFlow(false)
    private val _addEventDate = MutableStateFlow<LocalDate?>(null)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _snackbarIsError = MutableStateFlow(false)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()
    val snackbarIsError: StateFlow<Boolean> = _snackbarIsError.asStateFlow()

    val eventsFlow: StateFlow<List<ScheduleEvent>> = ScheduleRepository.eventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ScheduleUiState> = combine(
        _currentMonth,
        eventsFlow,
        _selectedDate,
        _showAddEventDialog,
        _addEventDate
    ) { month, events, selected, showDialog, addDate ->
        val eventsForSelected = selected?.let { d -> events.filter { it.date == d } } ?: emptyList()
        ScheduleUiState(
            currentMonth = month,
            events = events,
            selectedDate = selected,
            eventsForSelectedDate = eventsForSelected,
            showAddEventDialog = showDialog,
            addEventDate = addDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun goToToday() {
        _currentMonth.value = YearMonth.now()
    }

    /** Opens the calendar on the given month and selects that day (used from Overview KPI tiles). */
    fun jumpToDate(date: LocalDate) {
        _currentMonth.value = YearMonth.from(date)
        _selectedDate.value = date
    }

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun openAddEventDialog(date: LocalDate) {
        _addEventDate.value = date
        _showAddEventDialog.value = true
    }

    fun dismissAddEventDialog() {
        _showAddEventDialog.value = false
        _addEventDate.value = null
    }

    fun addEvent(event: ScheduleEvent) {
        viewModelScope.launch {
            try {
                ScheduleRepository.addEvent(event)
                dismissAddEventDialog()
                showSnackbar(SubmitResultMessages.SAVED_SUCCESS, isError = false)
            } catch (e: Exception) {
                showSnackbar(SubmitResultMessages.failure(e), isError = true)
            }
        }
    }

    fun addEvents(events: List<ScheduleEvent>) {
        viewModelScope.launch {
            try {
                events.forEach { ScheduleRepository.addEvent(it) }
                dismissAddEventDialog()
                showSnackbar(SubmitResultMessages.SAVED_SUCCESS, isError = false)
            } catch (e: Exception) {
                showSnackbar(SubmitResultMessages.failure(e), isError = true)
            }
        }
    }

    fun updateEvent(event: ScheduleEvent) {
        viewModelScope.launch {
            try {
                ScheduleRepository.updateEvent(event)
                showSnackbar(SubmitResultMessages.SAVED_SUCCESS, isError = false)
            } catch (e: Exception) {
                showSnackbar(SubmitResultMessages.failure(e), isError = true)
            }
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            try {
                ScheduleRepository.deleteEvent(id)
                showSnackbar(SubmitResultMessages.DELETE_SUCCESS, isError = false)
            } catch (e: Exception) {
                showSnackbar(SubmitResultMessages.failure(e), isError = true)
            }
        }
    }

    fun markCompleted(event: ScheduleEvent) {
        viewModelScope.launch {
            try {
                ScheduleRepository.updateEvent(event.copy(isCompleted = true))
                showSnackbar(SubmitResultMessages.MARKED_COMPLETE_SUCCESS, isError = false)
            } catch (e: Exception) {
                showSnackbar(SubmitResultMessages.failure(e), isError = true)
            }
        }
    }

    fun markIncomplete(event: ScheduleEvent) {
        viewModelScope.launch {
            try {
                ScheduleRepository.updateEvent(event.copy(isCompleted = false))
                showSnackbar("Marked incomplete", isError = false)
            } catch (e: Exception) {
                showSnackbar(SubmitResultMessages.failure(e), isError = true)
            }
        }
    }

    private fun showSnackbar(message: String, isError: Boolean) {
        _snackbarMessage.value = message
        _snackbarIsError.value = isError
    }

    fun clearScheduleSnackbar() {
        _snackbarMessage.value = null
        _snackbarIsError.value = false
    }
}
