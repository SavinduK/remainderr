package com.example

import android.app.Application
import com.example.data.backup.BackupRestoreManager
import com.example.data.local.AppDatabase
import com.example.data.local.ClinicalHistoryStore
import com.example.data.local.DiaryFileStore
import com.example.data.local.SecureConfigStore
import com.example.data.remote.GeminiClinicalExtractor
import com.example.data.repository.RemainderrRepository

class AppContainer(application: Application) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(application) }
    val repository: RemainderrRepository by lazy { RemainderrRepository(application, database) }
    val secureConfigStore: SecureConfigStore by lazy { SecureConfigStore(application) }
    val diaryFileStore: DiaryFileStore by lazy { DiaryFileStore(application) }
    val clinicalHistoryStore: ClinicalHistoryStore by lazy { ClinicalHistoryStore(application) }
    val geminiExtractor: GeminiClinicalExtractor by lazy {
        GeminiClinicalExtractor(application, secureConfigStore, clinicalHistoryStore)
    }
    val backupRestoreManager: BackupRestoreManager by lazy {
        BackupRestoreManager(application, database, diaryFileStore, clinicalHistoryStore)
    }
}

class RemainderrApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
