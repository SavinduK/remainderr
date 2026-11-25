package com.example.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.ClinicalChecklistDefaults
import com.example.data.local.CreditCardEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PasswordEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TransactionEntity
import com.example.widget.RemainderrWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemainderrRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    // --- TASKS ---
    val tasks: Flow<List<TaskEntity>> = db.taskDao().observeAll()

    suspend fun addTask(task: TaskEntity) {
        db.taskDao().insert(task)
        refreshWidget()
    }

    suspend fun toggleTaskCompleted(id: String, currentCompleted: Boolean) {
        db.taskDao().updateCompleted(id, !currentCompleted)
        refreshWidget()
    }

    suspend fun deleteTask(id: String) {
        db.taskDao().deleteById(id)
        refreshWidget()
    }

    suspend fun refreshWidget() {
        runCatching { RemainderrWidget().updateAll(context) }
    }

    // --- TRANSACTIONS & CREDIT CARDS ---
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    val creditCards: Flow<List<CreditCardEntity>> = db.creditCardDao().observeAll()

    suspend fun addTransaction(tx: TransactionEntity) {
        db.transactionDao().insert(tx)
    }

    suspend fun deleteTransaction(id: String) {
        db.transactionDao().deleteById(id)
    }

    suspend fun addCreditCard(card: CreditCardEntity) {
        db.creditCardDao().insert(card)
    }

    suspend fun deleteCreditCard(id: String) {
        db.creditCardDao().deleteById(id)
    }

    // --- NOTES & CATEGORIES ---
    val notes: Flow<List<NoteEntity>> = db.noteDao().observeAll()
    val categories: Flow<List<String>> = db.categoryDao().observeAll().map { list -> list.map { it.name } }

    suspend fun ensureDefaultCategories() {
        if (db.categoryDao().getAllOnce().isEmpty()) {
            db.categoryDao().insertAll(
                ClinicalChecklistDefaults.defaultNoteCategories.mapIndexed { index, name ->
                    CategoryEntity(name = name, sortOrder = index)
                },
            )
        }
    }

    suspend fun getNoteById(id: String): NoteEntity? = db.noteDao().getById(id)

    suspend fun saveNote(note: NoteEntity) {
        db.noteDao().insert(note)
    }

    suspend fun toggleNoteFavorite(note: NoteEntity) {
        db.noteDao().updateFavorite(note.id, !note.isFavorite)
    }

    suspend fun toggleNoteCompleted(note: NoteEntity) {
        db.noteDao().updateCompleted(note.id, !note.isCompleted)
    }

    suspend fun setNoteCategory(noteId: String, category: String) {
        db.noteDao().updateCategory(noteId, category)
    }

    suspend fun deleteNote(id: String) {
        db.noteDao().deleteById(id)
    }

    suspend fun addCategory(name: String) {
        val current = db.categoryDao().getAllOnce()
        if (current.none { it.name == name }) {
            db.categoryDao().insert(CategoryEntity(name = name, sortOrder = current.size))
        }
    }

    suspend fun deleteCategory(name: String) {
        db.categoryDao().deleteByName(name)
        db.noteDao().clearCategoryFromNotes(name)
    }

    // --- PASSWORDS ---
    val passwords: Flow<List<PasswordEntity>> = db.passwordDao().observeAll()

    suspend fun addPassword(entry: PasswordEntity) {
        db.passwordDao().insert(entry)
    }

    suspend fun deletePassword(id: String) {
        db.passwordDao().deleteById(id)
    }

    // --- CLINICAL CHECKLIST ---
    val checklistItems: Flow<List<ChecklistItemEntity>> = db.checklistDao().observeAll()

    suspend fun ensureDefaultChecklist() {
        if (db.checklistDao().count() == 0) {
            db.checklistDao().insertAll(ClinicalChecklistDefaults.initialChecklistItems())
        }
    }

    suspend fun toggleChecklistItem(id: String, currentlyChecked: Boolean) {
        db.checklistDao().updateChecked(id, !currentlyChecked)
    }

    suspend fun addChecklistItem(category: String, title: String) {
        val newItem = ChecklistItemEntity(
            id = System.currentTimeMillis().toString(),
            category = category.ifBlank { "Custom Clinical Notes" },
            title = title.trim(),
            checked = false,
            sortOrder = -1000 + (System.currentTimeMillis() % 1000).toInt(),
        )
        db.checklistDao().insert(newItem)
    }

    suspend fun deleteChecklistItem(id: String) {
        db.checklistDao().deleteById(id)
    }
}
