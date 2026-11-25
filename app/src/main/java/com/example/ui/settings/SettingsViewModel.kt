package com.example.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.ClinicalHistoryStore
import com.example.data.local.SecureConfigStore
import com.example.data.repository.RemainderrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class SettingsUiState(
    val apiKey: String = "",
    val model: String = SecureConfigStore.DEFAULT_MODEL,
    val templateJson: String = "",
    val showKey: Boolean = false,
    val isAiCollapsed: Boolean = true,
    val isBackupCollapsed: Boolean = true,
    val isBusy: Boolean = false,
    val alertTitle: String? = null,
    val alertMessage: String? = null,
)

class SettingsViewModel(
    private val secureConfigStore: SecureConfigStore,
    private val historyStore: ClinicalHistoryStore,
    private val backupManager: BackupRestoreManager,
    private val repository: RemainderrRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val savedKey = secureConfigStore.getApiKey()
            val savedModel = secureConfigStore.getModel()
            val template = historyStore.getTemplateJson()
            _uiState.update {
                it.copy(
                    apiKey = savedKey,
                    model = savedModel,
                    templateJson = template,
                )
            }
        }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKey = value) }
    }

    fun selectModel(modelId: String) {
        _uiState.update { it.copy(model = modelId) }
    }

    fun toggleShowKey() {
        _uiState.update { it.copy(showKey = !it.showKey) }
    }

    fun toggleAiCollapsed() {
        _uiState.update { it.copy(isAiCollapsed = !it.isAiCollapsed) }
    }

    fun toggleBackupCollapsed() {
        _uiState.update { it.copy(isBackupCollapsed = !it.isBackupCollapsed) }
    }

    fun saveSettings() {
        val current = _uiState.value
        viewModelScope.launch {
            val result = runCatching {
                secureConfigStore.setApiKey(current.apiKey)
                secureConfigStore.setModel(current.model)
                if (current.templateJson.isNotBlank()) {
                    Json.parseToJsonElement(current.templateJson)
                    historyStore.saveTemplateJson(current.templateJson)
                }
            }
            if (result.isSuccess) {
                showAlert("Success", "Settings saved successfully!")
            } else {
                showAlert("Error", "Invalid JSON Template or error saving settings.")
            }
        }
    }

    fun backupAll(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = backupManager.backupAll(treeUri)
            _uiState.update { it.copy(isBusy = false) }
            if (result.isSuccess) {
                showAlert("Success", result.getOrDefault("Backup completed successfully!"))
            } else {
                showAlert("Notice", result.exceptionOrNull()?.message ?: "Failed to complete backup.")
            }
        }
    }

    fun restoreAll(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = backupManager.restoreAll(treeUri)
            if (result.isSuccess) {
                repository.refreshWidget()
            }
            _uiState.update { it.copy(isBusy = false) }
            if (result.isSuccess) {
                showAlert("Success", result.getOrDefault("Data restored successfully!"))
            } else {
                showAlert("Error", result.exceptionOrNull()?.message ?: "Failed to restore data.")
            }
        }
    }

    fun showAlert(title: String, message: String) {
        _uiState.update { it.copy(alertTitle = title, alertMessage = message) }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertTitle = null, alertMessage = null) }
    }

    class Factory(
        private val secureConfigStore: SecureConfigStore,
        private val historyStore: ClinicalHistoryStore,
        private val backupManager: BackupRestoreManager,
        private val repository: RemainderrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(secureConfigStore, historyStore, backupManager, repository) as T
        }
    }
}
