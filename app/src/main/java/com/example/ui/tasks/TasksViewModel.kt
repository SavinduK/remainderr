package com.example.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.TaskEntity
import com.example.data.repository.RemainderrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class TasksUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMonth: YearMonth = YearMonth.now(),
    val isEditMode: Boolean = false,
    val deleteTargetId: String? = null,
)

class TasksViewModel(
    private val repository: RemainderrRepository,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val viewMonth = MutableStateFlow(YearMonth.now())
    private val isEditMode = MutableStateFlow(false)
    private val deleteTargetId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TasksUiState> = combine(
        repository.tasks,
        selectedDate,
        viewMonth,
        isEditMode,
        deleteTargetId,
    ) { tasks, selDate, month, edit, delId ->
        TasksUiState(
            tasks = tasks,
            selectedDate = selDate,
            viewMonth = month,
            isEditMode = edit,
            deleteTargetId = delId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState(),
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        if (YearMonth.from(date) != viewMonth.value) {
            viewMonth.value = YearMonth.from(date)
        }
    }

    fun changeMonth(delta: Long) {
        viewMonth.value = viewMonth.value.plusMonths(delta)
    }

    fun toggleEditMode() {
        isEditMode.value = !isEditMode.value
    }

    fun toggleComplete(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id, task.completed)
        }
    }

    fun requestDelete(id: String) {
        deleteTargetId.value = id
    }

    fun dismissDelete() {
        deleteTargetId.value = null
    }

    fun confirmDelete() {
        val id = deleteTargetId.value ?: return
        deleteTargetId.value = null
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    fun saveTask(
        title: String,
        description: String,
        date: LocalDate,
        startTime: LocalTime?,
        endTime: LocalTime?,
    ) {
        val zone = ZoneId.systemDefault()
        val dateIso = date.atStartOfDay(zone).toInstant().toString()
        val startIso = startTime?.let { date.atTime(it).atZone(zone).toInstant().toString() }
        val endIso = endTime?.let { date.atTime(it).atZone(zone).toInstant().toString() }

        val task = TaskEntity(
            id = System.currentTimeMillis().toString(),
            title = title.trim(),
            description = description.trim(),
            completed = false,
            date = dateIso,
            startTime = startIso,
            endTime = endIso,
            createdDate = Instant.now().toString(),
        )
        viewModelScope.launch {
            repository.addTask(task)
        }
    }

    class Factory(private val repository: RemainderrRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TasksViewModel(repository) as T
        }
    }
}
