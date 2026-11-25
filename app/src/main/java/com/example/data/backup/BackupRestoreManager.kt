package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.ClinicalChecklistDefaults
import com.example.data.local.ClinicalHistoryStore
import com.example.data.local.CreditCardEntity
import com.example.data.local.DiaryFileStore
import com.example.data.local.NoteEntity
import com.example.data.local.PasswordEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class BackupRestoreManager(
    private val context: Context,
    private val db: AppDatabase,
    private val diaryStore: DiaryFileStore,
    private val historyStore: ClinicalHistoryStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private fun getMimeType(filename: String): String {
        return when (filename.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/json"
        }
    }

    suspend fun backupAll(treeUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalStateException("Invalid backup directory.")

            val notes = db.noteDao().getAllOnce()
            val categories = db.categoryDao().getAllOnce().map { it.name }
            val transactions = db.transactionDao().getAllOnce()
            val cards = db.creditCardDao().getAllOnce()
            val tasks = db.taskDao().getAllOnce()
            val passwords = db.passwordDao().getAllOnce()

            val diaryDir = diaryStore.getAppDir()
            val hasDiary = (diaryDir.listFiles()?.isNotEmpty() == true)
            val historyDir = historyStore.getHistoriesDir()
            val hasHistory = (historyDir.listFiles()?.isNotEmpty() == true)

            val hasData = notes.isNotEmpty() || categories.isNotEmpty() || transactions.isNotEmpty() ||
                cards.isNotEmpty() || tasks.isNotEmpty() || passwords.isNotEmpty() ||
                hasDiary || hasHistory

            if (!hasData) {
                throw IllegalStateException("No data found to backup.")
            }

            rootDoc.listFiles().forEach { it.delete() }

            val appDataDir = rootDoc.createDirectory("AppData")
                ?: throw IllegalStateException("Could not create AppData directory.")

            writeTextFile(appDataDir, "notes.json", json.encodeToString(notes))
            writeTextFile(
                appDataDir,
                "categories.json",
                json.encodeToString(categories.ifEmpty { ClinicalChecklistDefaults.defaultNoteCategories }),
            )
            writeTextFile(appDataDir, "transactions.json", json.encodeToString(transactions))
            writeTextFile(appDataDir, "cards.json", json.encodeToString(cards))
            writeTextFile(appDataDir, "todo.json", json.encodeToString(tasks))
            writeTextFile(appDataDir, "passwords.json", json.encodeToString(passwords))

            if (diaryDir.exists()) {
                val diaryBackup = rootDoc.createDirectory("Diary")
                if (diaryBackup != null) copyDirectoryToDoc(diaryDir, diaryBackup)
            }

            if (historyDir.exists()) {
                val historyBackup = rootDoc.createDirectory("History")
                if (historyBackup != null) copyDirectoryToDoc(historyDir, historyBackup)
            }

            "Backup completed successfully!"
        }
    }

    suspend fun restoreAll(treeUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalStateException("Invalid restore directory.")

            val appDir = diaryStore.getAppDir()
            appDir.listFiles()?.forEach { it.deleteRecursively() }

            val historyDir = historyStore.getHistoriesDir()
            historyDir.listFiles()?.forEach { it.deleteRecursively() }

            for (topItem in rootDoc.listFiles()) {
                if (!topItem.isDirectory) continue
                when (topItem.name) {
                    "AppData" -> {
                        for (fileDoc in topItem.listFiles()) {
                            if (!fileDoc.isFile) continue
                            val text = readTextFile(fileDoc) ?: continue
                            when (fileDoc.name) {
                                "notes.json" -> {
                                    val list = runCatching { json.decodeFromString<List<NoteEntity>>(text) }.getOrNull()
                                    if (list != null) {
                                        db.noteDao().deleteAll()
                                        db.noteDao().insertAll(list)
                                    }
                                }
                                "categories.json" -> {
                                    val list = runCatching { json.decodeFromString<List<String>>(text) }.getOrNull()
                                    if (list != null) {
                                        db.categoryDao().deleteAll()
                                        db.categoryDao().insertAll(
                                            list.mapIndexed { i, name -> CategoryEntity(name, i) },
                                        )
                                    }
                                }
                                "transactions.json" -> {
                                    val list = runCatching { json.decodeFromString<List<TransactionEntity>>(text) }.getOrNull()
                                    if (list != null) {
                                        db.transactionDao().deleteAll()
                                        db.transactionDao().insertAll(list)
                                    }
                                }
                                "cards.json" -> {
                                    val list = runCatching { json.decodeFromString<List<CreditCardEntity>>(text) }.getOrNull()
                                    if (list != null) {
                                        db.creditCardDao().deleteAll()
                                        db.creditCardDao().insertAll(list)
                                    }
                                }
                                "todo.json" -> {
                                    val list = runCatching { json.decodeFromString<List<TaskEntity>>(text) }.getOrNull()
                                    if (list != null) {
                                        db.taskDao().deleteAll()
                                        db.taskDao().insertAll(list)
                                    }
                                }
                                "passwords.json" -> {
                                    val list = runCatching { json.decodeFromString<List<PasswordEntity>>(text) }.getOrNull()
                                    if (list != null) {
                                        db.passwordDao().deleteAll()
                                        db.passwordDao().insertAll(list)
                                    }
                                }
                            }
                        }
                    }
                    "Diary" -> copyDocToDirectory(topItem, appDir)
                    "History" -> copyDocToDirectory(topItem, historyDir)
                }
            }

            "Data restored successfully!"
        }
    }

    suspend fun backupNotesOnly(treeUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val notes = db.noteDao().getAllOnce()
            val categories = db.categoryDao().getAllOnce().map { it.name }
            if (notes.isEmpty() && categories.isEmpty()) {
                throw IllegalStateException("No data found to backup.")
            }
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalStateException("Invalid directory.")
            val existing = rootDoc.findFile("Notes")
            val targetDir = if (existing != null && existing.isDirectory) {
                existing.listFiles().forEach { it.delete() }
                existing
            } else {
                rootDoc.createDirectory("Notes") ?: throw IllegalStateException("Could not create Notes directory.")
            }
            writeTextFile(targetDir, "notes.json", json.encodeToString(notes))
            writeTextFile(
                targetDir,
                "categories.json",
                json.encodeToString(categories.ifEmpty { ClinicalChecklistDefaults.defaultNoteCategories }),
            )
            "Notes backup completed!"
        }
    }

    suspend fun restoreNotesOnly(treeUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalStateException("Invalid directory.")
            var restored = false

            suspend fun processDoc(fileDoc: DocumentFile) {
                val text = readTextFile(fileDoc) ?: return
                when (fileDoc.name) {
                    "notes.json" -> {
                        val list = runCatching { json.decodeFromString<List<NoteEntity>>(text) }.getOrNull()
                        if (list != null) {
                            db.noteDao().deleteAll()
                            db.noteDao().insertAll(list)
                            restored = true
                        }
                    }
                    "categories.json" -> {
                        val list = runCatching { json.decodeFromString<List<String>>(text) }.getOrNull()
                        if (list != null) {
                            db.categoryDao().deleteAll()
                            db.categoryDao().insertAll(list.mapIndexed { i, n -> CategoryEntity(n, i) })
                            restored = true
                        }
                    }
                }
            }

            for (item in rootDoc.listFiles()) {
                if (item.isDirectory && item.name == "Notes") {
                    item.listFiles().filter { it.isFile }.forEach { processDoc(it) }
                } else if (item.isFile) {
                    processDoc(item)
                }
            }

            if (!restored) throw IllegalStateException("No valid notes backup files found.")
            "Notes restored successfully!"
        }
    }

    suspend fun backupDiaryOnly(treeUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val diaryDir = diaryStore.getAppDir()
            if (!diaryDir.exists() || diaryDir.listFiles()?.isEmpty() != false) {
                throw IllegalStateException("No data found to backup.")
            }
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalStateException("Invalid directory.")
            rootDoc.listFiles().forEach { it.delete() }
            copyDirectoryToDoc(diaryDir, rootDoc)
            "Diary backup completed!"
        }
    }

    suspend fun restoreDiaryOnly(treeUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IllegalStateException("Invalid directory.")
            val diaryDir = diaryStore.getAppDir()
            diaryDir.listFiles()?.forEach { it.deleteRecursively() }
            for (sub in rootDoc.listFiles()) {
                if (sub.isDirectory && sub.name != "Notes") {
                    val destSub = File(diaryDir, sub.name ?: continue).apply { mkdirs() }
                    copyDocToDirectory(sub, destSub)
                }
            }
            "Diary restored successfully!"
        }
    }

    private fun writeTextFile(parent: DocumentFile, name: String, content: String) {
        val file = parent.createFile("application/json", name) ?: return
        context.contentResolver.openOutputStream(file.uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    private fun readTextFile(doc: DocumentFile): String? {
        return context.contentResolver.openInputStream(doc.uri)?.use { input ->
            input.bufferedReader().readText()
        }
    }

    private fun copyDirectoryToDoc(sourceDir: File, targetDoc: DocumentFile) {
        sourceDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val subDoc = targetDoc.createDirectory(file.name) ?: return@forEach
                copyDirectoryToDoc(file, subDoc)
            } else {
                val docFile = targetDoc.createFile(getMimeType(file.name), file.name) ?: return@forEach
                context.contentResolver.openOutputStream(docFile.uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
            }
        }
    }

    private fun copyDocToDirectory(sourceDoc: DocumentFile, targetDir: File) {
        sourceDoc.listFiles().forEach { item ->
            val name = item.name ?: return@forEach
            if (item.isDirectory) {
                val subDir = File(targetDir, name).apply { mkdirs() }
                copyDocToDirectory(item, subDir)
            } else if (item.isFile) {
                val destFile = File(targetDir, name)
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    destFile.outputStream().use { out -> input.copyTo(out) }
                }
            }
        }
    }
}
