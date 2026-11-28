package com.example.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.NoteEntity
import com.example.data.repository.RemainderrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

enum class NotesTab { ALL, CATEGORIES }

data class NotesUiState(
    val allNotes: List<NoteEntity> = emptyList(),
    val filteredNotes: List<NoteEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val activeTab: NotesTab = NotesTab.ALL,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
)

class NotesViewModel(
    private val repository: RemainderrRepository,
) : ViewModel() {

    private val activeTab = MutableStateFlow(NotesTab.ALL)
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")

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
    ) { notes, categories, tab, selCat, query ->
        val filtered = notes
            .filter { note ->
                val matchesTab = when (tab) {
                    NotesTab.ALL -> true
                    NotesTab.CATEGORIES -> selCat != null && note.category == selCat
                }
                val q = query.trim().lowercase()
                val matchesSearch = q.isEmpty() ||
                    note.title.lowercase().contains(q) ||
                    note.content.lowercase().contains(q)
                matchesTab && matchesSearch
            }
            .sortedByDescending {
                runCatching { Instant.parse(it.createdAt).toEpochMilli() }.getOrDefault(0L)
            }

        NotesUiState(
            allNotes = notes,
            filteredNotes = filtered,
            categories = categories,
            activeTab = tab,
            selectedCategory = selCat,
            searchQuery = query,
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
        content: String,
        onDone: () -> Unit,
    ) {
        if (content.isBlank()) {
            onDone()
            return
        }
        viewModelScope.launch {
            val existing = existingId?.let { repository.getNoteById(it) }
            val entity = NoteEntity(
                id = existingId ?: System.currentTimeMillis().toString(),
                title = "",
                content = content,
                createdAt = Instant.now().toString(),
                isFavorite = false,
                isCompleted = false,
                category = existing?.category,
            )
            repository.saveNote(entity)
            onDone()
        }
    }

    class Factory(
        private val repository: RemainderrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(repository) as T
        }
    }
}
