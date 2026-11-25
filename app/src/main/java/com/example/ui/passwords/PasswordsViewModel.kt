package com.example.ui.passwords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PasswordEntity
import com.example.data.repository.RemainderrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PasswordsUiState(
    val passwords: List<PasswordEntity> = emptyList(),
    val visiblePasswordIds: Set<String> = emptySet(),
    val deleteTargetId: String? = null,
)

class PasswordsViewModel(
    private val repository: RemainderrRepository,
) : ViewModel() {

    private val visiblePasswordIds = MutableStateFlow<Set<String>>(emptySet())
    private val deleteTargetId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PasswordsUiState> = combine(
        repository.passwords,
        visiblePasswordIds,
        deleteTargetId,
    ) { list, visibleIds, delId ->
        PasswordsUiState(
            passwords = list,
            visiblePasswordIds = visibleIds,
            deleteTargetId = delId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PasswordsUiState(),
    )

    fun togglePasswordVisibility(id: String) {
        val current = visiblePasswordIds.value
        visiblePasswordIds.value = if (id in current) current - id else current + id
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
            repository.deletePassword(id)
        }
    }

    fun addPassword(site: String, username: String, password: String) {
        if (site.isBlank() || username.isBlank() || password.isBlank()) return
        val entry = PasswordEntity(
            id = System.currentTimeMillis().toString(),
            site = site.trim(),
            username = username.trim(),
            password = password,
        )
        viewModelScope.launch {
            repository.addPassword(entry)
        }
    }

    class Factory(private val repository: RemainderrRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PasswordsViewModel(repository) as T
        }
    }
}
