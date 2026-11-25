package com.example.ui.diary

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.DiaryEntryModel
import com.example.data.local.DiaryFileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiaryUiState(
    val entries: List<DiaryEntryModel> = emptyList(),
    val statusMessage: String? = null,
)

class DiaryViewModel(
    private val diaryStore: DiaryFileStore,
    private val backupManager: BackupRestoreManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            val loaded = diaryStore.loadEntries()
            _uiState.value = _uiState.value.copy(entries = loaded)
        }
    }

    fun deleteEntry(folderPath: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            diaryStore.deleteEntry(folderPath)
            loadEntries()
            onDone()
        }
    }

    fun createEntry(
        title: String,
        text: String,
        dateIso: String,
        pickedImageUris: List<Uri>,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            diaryStore.createEntry(title, text, dateIso, pickedImageUris)
            loadEntries()
            onSaved()
        }
    }

    suspend fun loadSingleEntry(folderPath: String): DiaryEntryModel? {
        return diaryStore.loadEntry(folderPath)
    }

    fun updateEntry(
        folderPath: String,
        existingId: Long,
        title: String,
        text: String,
        dateIso: String,
        sources: List<String>,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            diaryStore.updateEntry(folderPath, existingId, title, text, dateIso, sources)
            loadEntries()
            onSaved()
        }
    }

    fun backupDiary(treeUri: Uri) {
        viewModelScope.launch {
            val result = backupManager.backupDiaryOnly(treeUri)
            _uiState.value = _uiState.value.copy(
                statusMessage = result.getOrElse { it.message ?: "Failed to complete backup." },
            )
        }
    }

    fun restoreDiary(treeUri: Uri) {
        viewModelScope.launch {
            val result = backupManager.restoreDiaryOnly(treeUri)
            loadEntries()
            _uiState.value = _uiState.value.copy(
                statusMessage = result.getOrElse { it.message ?: "Failed to restore data." },
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    class Factory(
        private val diaryStore: DiaryFileStore,
        private val backupManager: BackupRestoreManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiaryViewModel(diaryStore, backupManager) as T
        }
    }
}
