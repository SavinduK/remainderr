package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object TasksRoute
@Serializable object AddTaskRoute

@Serializable object DiaryRoute
@Serializable object AddDiaryEntryRoute
@Serializable data class EditDiaryEntryRoute(val folderPath: String)

@Serializable object TransactionsRoute
@Serializable object AddTransactionRoute

@Serializable object NotesRoute
@Serializable data class AddNoteRoute(val noteId: String? = null)

@Serializable object SettingsRoute
@Serializable object PasswordsRoute
