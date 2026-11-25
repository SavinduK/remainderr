package com.example.ui.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.ClinicalHistoryFile
import com.example.data.local.ClinicalHistoryStore
import com.example.data.remote.GeminiClinicalExtractor
import com.example.data.repository.RemainderrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

data class ClinicalListUiState(
    val files: List<ClinicalHistoryFile> = emptyList(),
    val deleteTarget: ClinicalHistoryFile? = null,
    val statusError: String? = null,
)

data class ClinicalInputUiState(
    val selectedImages: List<Uri> = emptyList(),
    val isExtracting: Boolean = false,
    val alertTitle: String? = null,
    val alertMessage: String? = null,
    val isSuccess: Boolean = false,
)

data class ClinicalViewUiState(
    val fileName: String = "",
    val jsonContent: String = "",
    val isEditing: Boolean = false,
    val alertTitle: String? = null,
    val alertMessage: String? = null,
    val loadFailed: Boolean = false,
)

data class ClinicalChecklistUiState(
    val items: List<ChecklistItemEntity> = emptyList(),
    val isEditMode: Boolean = false,
    val deleteTargetId: String? = null,
)

class ClinicalHistoryViewModel(
    private val historyStore: ClinicalHistoryStore,
    private val geminiExtractor: GeminiClinicalExtractor,
    private val repository: RemainderrRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow(ClinicalListUiState())
    val listState: StateFlow<ClinicalListUiState> = _listState.asStateFlow()

    private val _inputState = MutableStateFlow(ClinicalInputUiState())
    val inputState: StateFlow<ClinicalInputUiState> = _inputState.asStateFlow()

    private val _viewState = MutableStateFlow(ClinicalViewUiState())
    val viewState: StateFlow<ClinicalViewUiState> = _viewState.asStateFlow()

    private val checklistEditMode = MutableStateFlow(false)
    private val checklistDeleteTarget = MutableStateFlow<String?>(null)

    val checklistState: StateFlow<ClinicalChecklistUiState> = combine(
        repository.checklistItems,
        checklistEditMode,
        checklistDeleteTarget,
    ) { items, edit, delId ->
        ClinicalChecklistUiState(
            items = items,
            isEditMode = edit,
            deleteTargetId = delId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClinicalChecklistUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultChecklist()
        }
        loadHistoryFiles()
    }

    // --- HISTORY LIST ---
    fun loadHistoryFiles() {
        viewModelScope.launch {
            val files = historyStore.listHistoryFiles()
            _listState.update { it.copy(files = files) }
        }
    }

    fun getShareFile(fileName: String): File = historyStore.getHistoryFile(fileName)

    fun requestDeleteFile(file: ClinicalHistoryFile) {
        _listState.update { it.copy(deleteTarget = file) }
    }

    fun dismissDeleteFile() {
        _listState.update { it.copy(deleteTarget = null) }
    }

    fun confirmDeleteFile() {
        val target = _listState.value.deleteTarget ?: return
        _listState.update { it.copy(deleteTarget = null) }
        viewModelScope.launch {
            historyStore.deleteHistoryFile(target.fileName)
            loadHistoryFiles()
        }
    }

    fun setListError(msg: String?) {
        _listState.update { it.copy(statusError = msg) }
    }

    // --- INPUT / UPLOAD NOTE ---
    fun addSelectedImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _inputState.update { it.copy(selectedImages = it.selectedImages + uris) }
    }

    fun removeSelectedImage(index: Int) {
        _inputState.update { state ->
            state.copy(selectedImages = state.selectedImages.filterIndexed { i, _ -> i != index })
        }
    }

    fun clearSelectedImages() {
        _inputState.update { it.copy(selectedImages = emptyList()) }
    }

    fun extractAndSave() {
        val uris = _inputState.value.selectedImages
        if (uris.isEmpty()) {
            _inputState.update {
                it.copy(alertTitle = "Error", alertMessage = "Please select at least one image first.", isSuccess = false)
            }
            return
        }
        viewModelScope.launch {
            _inputState.update { it.copy(isExtracting = true) }
            val result = geminiExtractor.extractAndSave(uris)
            if (result.isSuccess) {
                loadHistoryFiles()
                _inputState.update {
                    it.copy(
                        isExtracting = false,
                        selectedImages = emptyList(),
                        alertTitle = "Success",
                        alertMessage = "History extracted and saved successfully!",
                        isSuccess = true,
                    )
                }
            } else {
                _inputState.update {
                    it.copy(
                        isExtracting = false,
                        alertTitle = "Processing Error",
                        alertMessage = result.exceptionOrNull()?.message ?: "Failed to extract data.",
                        isSuccess = false,
                    )
                }
            }
        }
    }

    fun dismissInputAlert() {
        _inputState.update { it.copy(alertTitle = null, alertMessage = null, isSuccess = false) }
    }

    // --- VIEW / EDIT FILE ---
    fun loadHistoryContent(fileName: String) {
        viewModelScope.launch {
            val res = runCatching { historyStore.readHistoryFile(fileName) }
            if (res.isSuccess) {
                _viewState.value = ClinicalViewUiState(
                    fileName = fileName,
                    jsonContent = res.getOrDefault(""),
                    isEditing = false,
                )
            } else {
                _viewState.value = ClinicalViewUiState(
                    fileName = fileName,
                    alertTitle = "Error",
                    alertMessage = "Could not load file.",
                    loadFailed = true,
                )
            }
        }
    }

    fun toggleViewEditing() {
        val current = _viewState.value
        if (current.isEditing) {
            loadHistoryContent(current.fileName)
        } else {
            _viewState.update { it.copy(isEditing = true) }
        }
    }

    fun updateViewJson(text: String) {
        _viewState.update { it.copy(jsonContent = text) }
    }

    fun saveViewJson() {
        val current = _viewState.value
        viewModelScope.launch {
            val valid = runCatching {
                Json.parseToJsonElement(current.jsonContent)
                historyStore.writeHistoryFile(current.fileName, current.jsonContent)
            }
            if (valid.isSuccess) {
                loadHistoryFiles()
                _viewState.update {
                    it.copy(
                        isEditing = false,
                        alertTitle = "Saved",
                        alertMessage = "Patient history updated successfully.",
                    )
                }
            } else {
                _viewState.update {
                    it.copy(
                        alertTitle = "Invalid Format",
                        alertMessage = "Ensure the text is valid JSON before saving.",
                    )
                }
            }
        }
    }

    fun dismissViewAlert() {
        _viewState.update { it.copy(alertTitle = null, alertMessage = null) }
    }

    // --- CHECKLIST ---
    fun toggleChecklistEditMode() {
        checklistEditMode.value = !checklistEditMode.value
    }

    fun toggleChecklistItem(item: ChecklistItemEntity) {
        viewModelScope.launch {
            repository.toggleChecklistItem(item.id, item.checked)
        }
    }

    fun requestDeleteChecklistItem(id: String) {
        checklistDeleteTarget.value = id
    }

    fun dismissDeleteChecklistItem() {
        checklistDeleteTarget.value = null
    }

    fun confirmDeleteChecklistItem() {
        val id = checklistDeleteTarget.value ?: return
        checklistDeleteTarget.value = null
        viewModelScope.launch {
            repository.deleteChecklistItem(id)
        }
    }

    fun addChecklistItem(category: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addChecklistItem(category, title)
        }
    }

    class Factory(
        private val historyStore: ClinicalHistoryStore,
        private val geminiExtractor: GeminiClinicalExtractor,
        private val repository: RemainderrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ClinicalHistoryViewModel(historyStore, geminiExtractor, repository) as T
        }
    }
}
