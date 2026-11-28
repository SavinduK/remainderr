package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.components.MainTab
import com.example.ui.components.RemainderrBottomBar
import com.example.ui.diary.AddDiaryEntryScreen
import com.example.ui.diary.DiaryScreen
import com.example.ui.diary.DiaryViewModel
import com.example.ui.diary.EditDiaryEntryScreen
import com.example.ui.navigation.AddDiaryEntryRoute
import com.example.ui.navigation.AddNoteRoute
import com.example.ui.navigation.AddTaskRoute
import com.example.ui.navigation.AddTransactionRoute
import com.example.ui.navigation.DiaryRoute
import com.example.ui.navigation.EditDiaryEntryRoute
import com.example.ui.navigation.NotesRoute
import com.example.ui.navigation.PasswordsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.TasksRoute
import com.example.ui.navigation.TransactionsRoute
import com.example.ui.notes.AddNoteScreen
import com.example.ui.notes.NotesScreen
import com.example.ui.notes.NotesViewModel
import com.example.ui.passwords.PasswordsScreen
import com.example.ui.passwords.PasswordsViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.tasks.AddTaskScreen
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel
import com.example.ui.theme.RemainderrTheme
import com.example.ui.transactions.AddTransactionScreen
import com.example.ui.transactions.TransactionsScreen
import com.example.ui.transactions.TransactionsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as RemainderrApplication).container

        setContent {
            RemainderrTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val destination = backStackEntry?.destination

                val tasksViewModel: TasksViewModel = viewModel(
                    factory = TasksViewModel.Factory(container.repository),
                )
                val diaryViewModel: DiaryViewModel = viewModel(
                    factory = DiaryViewModel.Factory(container.diaryFileStore),
                )
                val transactionsViewModel: TransactionsViewModel = viewModel(
                    factory = TransactionsViewModel.Factory(container.repository),
                )
                val notesViewModel: NotesViewModel = viewModel(
                    factory = NotesViewModel.Factory(container.repository),
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        container.googleDriveSyncManager,
                        container.secureConfigStore,
                        container.repository,
                    ),
                )
                val passwordsViewModel: PasswordsViewModel = viewModel(
                    factory = PasswordsViewModel.Factory(container.repository),
                )

                val showBottomBar = destination == null ||
                    destination.hasRoute<TasksRoute>() ||
                    destination.hasRoute<DiaryRoute>() ||
                    destination.hasRoute<TransactionsRoute>() ||
                    destination.hasRoute<NotesRoute>() ||
                    destination.hasRoute<SettingsRoute>() ||
                    destination.hasRoute<PasswordsRoute>()

                val activeTab = when {
                    destination == null || destination.hasRoute<TasksRoute>() -> MainTab.TASKS
                    destination.hasRoute<DiaryRoute>() -> MainTab.DIARY
                    destination.hasRoute<TransactionsRoute>() -> MainTab.CASH
                    destination.hasRoute<NotesRoute>() -> MainTab.NOTES
                    destination.hasRoute<SettingsRoute>() -> MainTab.SETTINGS
                    else -> null
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBottomBar) {
                            RemainderrBottomBar(
                                currentTab = activeTab,
                                onNavigateTab = { tab ->
                                    val targetRoute: Any = when (tab) {
                                        MainTab.TASKS -> TasksRoute
                                        MainTab.DIARY -> DiaryRoute
                                        MainTab.CASH -> TransactionsRoute
                                        MainTab.NOTES -> NotesRoute
                                        MainTab.SETTINGS -> SettingsRoute
                                    }
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp),
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = TasksRoute,
                        ) {
                            composable<TasksRoute> {
                                TasksScreen(
                                    viewModel = tasksViewModel,
                                    onAddTaskClick = { navController.navigate(AddTaskRoute) },
                                )
                            }
                            composable<AddTaskRoute> {
                                AddTaskScreen(
                                    onBack = { navController.popBackStack() },
                                    onSave = { title, desc, date, startTime, endTime ->
                                        tasksViewModel.saveTask(title, desc, date, startTime, endTime)
                                    },
                                )
                            }

                            composable<DiaryRoute> {
                                DiaryScreen(
                                    viewModel = diaryViewModel,
                                    onAddEntryClick = { navController.navigate(AddDiaryEntryRoute) },
                                    onOpenEntry = { folderPath ->
                                        navController.navigate(EditDiaryEntryRoute(folderPath))
                                    },
                                )
                            }
                            composable<AddDiaryEntryRoute> {
                                AddDiaryEntryScreen(
                                    viewModel = diaryViewModel,
                                    onBack = { navController.popBackStack() },
                                    onSaved = { navController.popBackStack() },
                                )
                            }
                            composable<EditDiaryEntryRoute> { entry ->
                                val route = entry.toRoute<EditDiaryEntryRoute>()
                                EditDiaryEntryScreen(
                                    folderPath = route.folderPath,
                                    viewModel = diaryViewModel,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<TransactionsRoute> {
                                TransactionsScreen(
                                    viewModel = transactionsViewModel,
                                    onAddTransactionClick = { navController.navigate(AddTransactionRoute) },
                                )
                            }
                            composable<AddTransactionRoute> {
                                AddTransactionScreen(
                                    onBack = { navController.popBackStack() },
                                    onSave = { isExpense, amount, category, description ->
                                        transactionsViewModel.addTransaction(isExpense, amount, category, description)
                                    },
                                )
                            }

                            composable<NotesRoute> {
                                NotesScreen(
                                    viewModel = notesViewModel,
                                    onAddNoteClick = { navController.navigate(AddNoteRoute(noteId = null)) },
                                    onEditNoteClick = { noteId ->
                                        navController.navigate(AddNoteRoute(noteId = noteId))
                                    },
                                )
                            }
                            composable<AddNoteRoute> { entry ->
                                val route = entry.toRoute<AddNoteRoute>()
                                AddNoteScreen(
                                    noteId = route.noteId,
                                    viewModel = notesViewModel,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onOpenPasswords = { navController.navigate(PasswordsRoute) },
                                )
                            }

                            composable<PasswordsRoute> {
                                PasswordsScreen(viewModel = passwordsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
