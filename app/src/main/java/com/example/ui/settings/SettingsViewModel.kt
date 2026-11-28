package com.example.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SecureConfigStore
import com.example.data.remote.GoogleDriveSyncManager
import com.example.data.repository.RemainderrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DriveAction {
    CONNECT,
    SYNC,
    UPLOAD,
    RESTORE,
}

data class SettingsUiState(
    val connectedEmail: String = "",
    val folderName: String = GoogleDriveSyncManager.SYNC_FOLDER_NAME,
    val lastSyncedAt: Long = 0L,
    val isSyncing: Boolean = false,
    val pendingAction: DriveAction? = null,
    val pendingResolution: IntentSender? = null,
    val restoreConfirmVisible: Boolean = false,
    val alertTitle: String? = null,
    val alertMessage: String? = null,
) {
    val isConnected: Boolean
        get() = connectedEmail.isNotBlank()
}

class SettingsViewModel(
    private val googleDriveSyncManager: GoogleDriveSyncManager,
    private val secureConfigStore: SecureConfigStore,
    private val repository: RemainderrRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshSyncMetadata()
    }

    private fun refreshSyncMetadata() {
        _uiState.update {
            it.copy(
                connectedEmail = secureConfigStore.getDriveEmail(),
                lastSyncedAt = secureConfigStore.getLastSyncedAt(),
            )
        }
    }

    fun startDriveAction(activity: Activity, action: DriveAction) {
        if (_uiState.value.isSyncing) return
        _uiState.update {
            it.copy(
                isSyncing = true,
                pendingAction = action,
                restoreConfirmVisible = false,
            )
        }
        viewModelScope.launch {
            val authResult = runCatching { googleDriveSyncManager.requestAuthorization(activity) }
            authResult.onSuccess { result ->
                if (result.hasResolution() && result.pendingIntent != null) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            pendingResolution = result.pendingIntent!!.intentSender,
                        )
                    }
                } else {
                    val token = result.accessToken
                    if (!token.isNullOrBlank()) {
                        executeDriveAction(token, action)
                    } else {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                pendingAction = null,
                                alertTitle = "Google Drive",
                                alertMessage = "Unable to obtain Google Drive access token.",
                            )
                        }
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        pendingAction = null,
                        alertTitle = "Authorization Error",
                        alertMessage = err.message ?: "Google Drive authorization failed.",
                    )
                }
            }
        }
    }

    fun consumeResolution() {
        _uiState.update { it.copy(pendingResolution = null) }
    }

    fun handleAuthorizationIntentResult(activity: Activity, resultCode: Int, data: Intent?) {
        val action = _uiState.value.pendingAction ?: DriveAction.SYNC
        if (resultCode != Activity.RESULT_OK || data == null) {
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    pendingAction = null,
                    alertTitle = "Google Drive",
                    alertMessage = "Google Drive sign-in was canceled.",
                )
            }
            return
        }

        _uiState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val result = runCatching {
                googleDriveSyncManager.getAuthorizationResultFromIntent(activity, data)
            }
            result.onSuccess { authResult ->
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    executeDriveAction(token, action)
                } else {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            pendingAction = null,
                            alertTitle = "Google Drive",
                            alertMessage = "Authorization succeeded, but no access token was returned.",
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        pendingAction = null,
                        alertTitle = "Authorization Error",
                        alertMessage = err.message ?: "Could not complete Google Drive sign-in.",
                    )
                }
            }
        }
    }

    private suspend fun executeDriveAction(accessToken: String, action: DriveAction) {
        when (action) {
            DriveAction.CONNECT -> {
                val res = googleDriveSyncManager.refreshAccountInfo(accessToken)
                res.onSuccess { (email, _) ->
                    _uiState.update {
                        it.copy(
                            connectedEmail = email.ifBlank { secureConfigStore.getDriveEmail() },
                            lastSyncedAt = secureConfigStore.getLastSyncedAt(),
                            isSyncing = false,
                            pendingAction = null,
                            alertTitle = "Google Drive Connected",
                            alertMessage = "Connected${if (email.isNotBlank()) " as $email" else ""}. Sync folder \"${GoogleDriveSyncManager.SYNC_FOLDER_NAME}\" is ready.",
                        )
                    }
                }.onFailure { err ->
                    onDriveFailure(err)
                }
            }

            DriveAction.SYNC -> {
                val res = googleDriveSyncManager.syncAll(accessToken)
                res.onSuccess { summary ->
                    repository.refreshWidget()
                    _uiState.update {
                        it.copy(
                            connectedEmail = summary.email,
                            lastSyncedAt = summary.syncedAt,
                            isSyncing = false,
                            pendingAction = null,
                            alertTitle = "Sync Complete",
                            alertMessage = "Synced to \"${GoogleDriveSyncManager.SYNC_FOLDER_NAME}\": ${summary.notesCount} notes, ${summary.tasksCount} tasks, ${summary.transactionsCount} transactions, ${summary.passwordsCount} passwords.",
                        )
                    }
                }.onFailure { err ->
                    onDriveFailure(err)
                }
            }

            DriveAction.UPLOAD -> {
                val res = googleDriveSyncManager.uploadAll(accessToken)
                res.onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            connectedEmail = summary.email,
                            lastSyncedAt = summary.syncedAt,
                            isSyncing = false,
                            pendingAction = null,
                            alertTitle = "Upload Complete",
                            alertMessage = "Uploaded to \"${GoogleDriveSyncManager.SYNC_FOLDER_NAME}\": ${summary.notesCount} notes, ${summary.tasksCount} tasks, ${summary.transactionsCount} transactions, ${summary.passwordsCount} passwords.",
                        )
                    }
                }.onFailure { err ->
                    onDriveFailure(err)
                }
            }

            DriveAction.RESTORE -> {
                val res = googleDriveSyncManager.restoreAll(accessToken)
                res.onSuccess { summary ->
                    repository.refreshWidget()
                    _uiState.update {
                        it.copy(
                            connectedEmail = summary.email,
                            lastSyncedAt = summary.syncedAt,
                            isSyncing = false,
                            pendingAction = null,
                            alertTitle = "Restore Complete",
                            alertMessage = "Restored from \"${GoogleDriveSyncManager.SYNC_FOLDER_NAME}\": ${summary.notesCount} notes, ${summary.tasksCount} tasks, ${summary.transactionsCount} transactions, ${summary.passwordsCount} passwords.",
                        )
                    }
                }.onFailure { err ->
                    onDriveFailure(err)
                }
            }
        }
    }

    private fun onDriveFailure(err: Throwable) {
        _uiState.update {
            it.copy(
                isSyncing = false,
                pendingAction = null,
                alertTitle = "Sync Failed",
                alertMessage = err.message ?: "Unable to sync with Google Drive.",
            )
        }
    }

    fun showRestoreConfirm() {
        _uiState.update { it.copy(restoreConfirmVisible = true) }
    }

    fun dismissRestoreConfirm() {
        _uiState.update { it.copy(restoreConfirmVisible = false) }
    }

    fun disconnectDrive() {
        googleDriveSyncManager.disconnect()
        _uiState.update {
            it.copy(
                connectedEmail = "",
                lastSyncedAt = 0L,
                pendingAction = null,
                alertTitle = "Disconnected",
                alertMessage = "Google Drive sync account disconnected from this device.",
            )
        }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertTitle = null, alertMessage = null) }
    }

    class Factory(
        private val googleDriveSyncManager: GoogleDriveSyncManager,
        private val secureConfigStore: SecureConfigStore,
        private val repository: RemainderrRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(googleDriveSyncManager, secureConfigStore, repository) as T
        }
    }
}
