package com.example.ui.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.NoteEntity
import com.example.data.repository.RemainderrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

enum class NotesTab { ALL, FAVORITES, CATEGORIES }

data class NotesUiState(
    val allNotes: List<NoteEntity> = emptyList(),
    val filteredNotes: List<NoteEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val activeTab: NotesTab = NotesTab.ALL,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val statusMessage: String? = null,
)

class NotesViewModel(
    private val repository: RemainderrRepository,
    private val backupManager: BackupRestoreManager,
) : ViewModel() {

    private val activeTab = MutableStateFlow(NotesTab.ALL)
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val statusMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    val uiState: StateFlow<NotesUiState> = combine(
        repository.notes,
        repository.categories,
        activeTab,
        selectedCategory,
        searchQuery,
        statusMessage,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val notes = values[0] as List<NoteEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<String>
        val tab = values[2] as NotesTab
        val selCat = values[3] as String?
        val query = values[4] as String
        val status = values[5] as String?

        val filtered = notes
            .filter { note ->
                val matchesTab = when (tab) {
                    NotesTab.ALL -> true
                    NotesTab.FAVORITES -> note.isFavorite
                    NotesTab.CATEGORIES -> selCat != null && note.category == selCat
                }
                val q = query.trim().lowercase()
                val matchesSearch = q.isEmpty() ||
                    note.title.lowercase().contains(q) ||
                    note.content.lowercase().contains(q)
                matchesTab && matchesSearch
            }
            .sortedWith(
                compareBy<NoteEntity> { it.isCompleted }
                    .thenByDescending { runCatching { Instant.parse(it.createdAt).toEpochMilli() }.getOrDefault(0L) },
            )

        NotesUiState(
            allNotes = notes,
            filteredNotes = filtered,
            categories = categories,
            activeTab = tab,
            selectedCategory = selCat,
            searchQuery = query,
            statusMessage = status,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState(),
    )

    fun setTab(tab: NotesTab) {
        activeTab.value = tab
        if (tab != NotesTab.CATEGORIES) {
            selectedCategory.value = null
        }
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch { repository.toggleNoteFavorite(note) }
    }

    fun toggleCompleted(note: NoteEntity) {
        viewModelScope.launch { repository.toggleNoteCompleted(note) }
    }

    fun setNoteCategory(noteId: String, category: String) {
        viewModelScope.launch { repository.setNoteCategory(noteId, category) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repository.deleteNote(noteId) }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addCategory(trimmed) }
    }

    fun deleteCategory(name: String) {
        if (selectedCategory.value == name) {
            selectedCategory.value = null
        }
        viewModelScope.launch { repository.deleteCategory(name) }
    }

    suspend fun getNoteById(id: String): NoteEntity? = repository.getNoteById(id)

    fun saveNote(
        existingId: String?,
        title: String,
        content: String,
        isFavorite: Boolean,
        onDone: () -> Unit,
    ) {
        if (content.isBlank() && title.isBlank()) {
            onDone()
            return
        }
        viewModelScope.launch {
            val existing = existingId?.let { repository.getNoteById(it) }
            val entity = NoteEntity(
                id = existingId ?: System.currentTimeMillis().toString(),
                title = "",
                content = content.ifBlank { title },
                createdAt = Instant.now().toString(),
                isFavorite = isFavorite,
                isCompleted = existing?.isCompleted ?: false,
                category = existing?.category,
            )
            repository.saveNote(entity)
            onDone()
        }
    }

    fun backupNotes(treeUri: Uri) {
        viewModelScope.launch {
            val res = backupManager.backupNotesOnly(treeUri)
            statusMessage.value = res.getOrElse { it.message ?: "Failed to complete backup." }
        }
    }

    fun restoreNotes(treeUri: Uri) {
        viewModelScope.launch {
            val res = backupManager.restoreNotesOnly(treeUri)
            statusMessage.value = res.getOrElse { it.message ?: "Failed to restore data." }
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    class Factory(
        private val repository: RemainderrRepository,
        private val backupManager: BackupRestoreManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(repository, backupManager) as T
        }
    }
}
