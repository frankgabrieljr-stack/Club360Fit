package com.club360fit.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.ScheduleEvent
import com.club360fit.app.data.ScheduleRepository
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
            ScheduleRepository.addEvent(event)
            dismissAddEventDialog()
        }
    }

    fun updateEvent(event: ScheduleEvent) {
        viewModelScope.launch {
            ScheduleRepository.updateEvent(event)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            ScheduleRepository.deleteEvent(id)
        }
    }

    fun markCompleted(event: ScheduleEvent) {
        updateEvent(event.copy(isCompleted = true))
    }
}
