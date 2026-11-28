package com.example.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
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
    companion object {
        val defaultNoteCategories = listOf("Work", "Personal", "Ideas", "To-Do")
    }

    // --- TASKS ---
    val tasks: Flow<List<TaskEntity>> = db.taskDao().observeAll()

    suspend fun getTasksOnce(): List<TaskEntity> = db.taskDao().getAllOnce()

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

    suspend fun replaceTasks(tasks: List<TaskEntity>) {
        db.withTransaction {
            db.taskDao().deleteAll()
            if (tasks.isNotEmpty()) db.taskDao().insertAll(tasks)
        }
        refreshWidget()
    }

    suspend fun refreshWidget() {
        runCatching { RemainderrWidget().updateAll(context) }
    }

    // --- TRANSACTIONS & CREDIT CARDS ---
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    val creditCards: Flow<List<CreditCardEntity>> = db.creditCardDao().observeAll()

    suspend fun getTransactionsOnce(): List<TransactionEntity> = db.transactionDao().getAllOnce()

    suspend fun getCreditCardsOnce(): List<CreditCardEntity> = db.creditCardDao().getAllOnce()

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

    suspend fun replaceTransactionsAndCards(
        transactions: List<TransactionEntity>,
        cards: List<CreditCardEntity>?,
    ) {
        db.withTransaction {
            db.transactionDao().deleteAll()
            if (transactions.isNotEmpty()) db.transactionDao().insertAll(transactions)
            if (cards != null) {
                db.creditCardDao().deleteAll()
                if (cards.isNotEmpty()) db.creditCardDao().insertAll(cards)
            }
        }
    }

    // --- NOTES & CATEGORIES ---
    val notes: Flow<List<NoteEntity>> = db.noteDao().observeAll()
    val categories: Flow<List<String>> = db.categoryDao().observeAll().map { list -> list.map { it.name } }

    suspend fun getNotesOnce(): List<NoteEntity> = db.noteDao().getAllOnce()

    suspend fun getCategoriesOnce(): List<String> = db.categoryDao().getAllOnce().map { it.name }

    suspend fun ensureDefaultCategories() {
        if (db.categoryDao().getAllOnce().isEmpty()) {
            db.categoryDao().insertAll(
                defaultNoteCategories.mapIndexed { index, name ->
                    CategoryEntity(name = name, sortOrder = index)
                },
            )
        }
    }

    suspend fun getNoteById(id: String): NoteEntity? = db.noteDao().getById(id)

    suspend fun saveNote(note: NoteEntity) {
        db.noteDao().insert(note)
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

    suspend fun replaceNotesAndCategories(notes: List<NoteEntity>, categories: List<String>) {
        db.withTransaction {
            db.noteDao().deleteAll()
            if (notes.isNotEmpty()) db.noteDao().insertAll(notes)
            if (categories.isNotEmpty()) {
                db.categoryDao().deleteAll()
                db.categoryDao().insertAll(
                    categories.distinct().mapIndexed { index, name ->
                        CategoryEntity(name = name, sortOrder = index)
                    },
                )
            }
        }
    }

    // --- PASSWORDS ---
    val passwords: Flow<List<PasswordEntity>> = db.passwordDao().observeAll()

    suspend fun getPasswordsOnce(): List<PasswordEntity> = db.passwordDao().getAllOnce()

    suspend fun addPassword(entry: PasswordEntity) {
        db.passwordDao().insert(entry)
    }

    suspend fun deletePassword(id: String) {
        db.passwordDao().deleteById(id)
    }

    suspend fun replacePasswords(passwords: List<PasswordEntity>) {
        db.withTransaction {
            db.passwordDao().deleteAll()
            if (passwords.isNotEmpty()) db.passwordDao().insertAll(passwords)
        }
    }
}
