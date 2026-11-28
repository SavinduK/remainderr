package com.example.data.remote

import android.app.Activity
import android.content.Context
import com.example.data.local.CreditCardEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PasswordEntity
import com.example.data.local.SecureConfigStore
import com.example.data.local.TaskEntity
import com.example.data.local.TransactionEntity
import com.example.data.repository.RemainderrRepository
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable
data class NotesSyncPayload(
    val notes: List<NoteEntity> = emptyList(),
    val categories: List<String> = emptyList(),
)

@Serializable
data class TransactionsSyncPayload(
    val transactions: List<TransactionEntity> = emptyList(),
    val creditCards: List<CreditCardEntity> = emptyList(),
)

data class DriveSyncSummary(
    val email: String,
    val folderId: String,
    val notesCount: Int,
    val tasksCount: Int,
    val transactionsCount: Int,
    val passwordsCount: Int,
    val syncedAt: Long,
)

class GoogleDriveSyncManager(
    private val context: Context,
    private val repository: RemainderrRepository,
    private val secureConfigStore: SecureConfigStore,
) {
    companion object {
        const val SYNC_FOLDER_NAME = "Remainderr Sync"
        private const val FILE_NOTES = "notes.json"
        private const val FILE_TASKS = "tasks.json"
        private const val FILE_TRANSACTIONS = "transactions.json"
        private const val FILE_PASSWORDS = "passwords.json"

        val REQUIRED_SCOPES: List<Scope> = listOf(
            Scope("https://www.googleapis.com/auth/drive.file"),
            Scope("https://www.googleapis.com/auth/userinfo.email"),
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun requestAuthorization(activity: Activity): AuthorizationResult =
        suspendCancellableCoroutine { cont ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(REQUIRED_SCOPES)
                .build()
            Identity.getAuthorizationClient(activity)
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result)
                }
                .addOnFailureListener { error ->
                    if (cont.isActive) cont.resumeWithException(error)
                }
        }

    fun getAuthorizationResultFromIntent(activity: Activity, data: android.content.Intent?): AuthorizationResult {
        return Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
    }

    suspend fun refreshAccountInfo(accessToken: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val email = fetchUserEmail(accessToken)
            if (email.isNotBlank()) secureConfigStore.setDriveEmail(email)
            val folderId = ensureSyncFolder(accessToken)
            email to folderId
        }
    }

    suspend fun syncAll(accessToken: String): Result<DriveSyncSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val email = fetchUserEmail(accessToken).also {
                if (it.isNotBlank()) secureConfigStore.setDriveEmail(it)
            }
            val folderId = ensureSyncFolder(accessToken)

            // 1. Notes + Categories
            val localNotes = repository.getNotesOnce()
            val localCategories = repository.getCategoriesOnce().ifEmpty { RemainderrRepository.defaultNoteCategories }
            val remoteNotesPayload = readFileText(accessToken, folderId, FILE_NOTES)?.let { decodeNotesPayload(it) }
            val mergedNotes = mergeById(localNotes, remoteNotesPayload?.notes.orEmpty()) { a, b ->
                val aTime = runCatching { Instant.parse(a.createdAt).toEpochMilli() }.getOrDefault(0L)
                val bTime = runCatching { Instant.parse(b.createdAt).toEpochMilli() }.getOrDefault(0L)
                if (bTime > aTime) b else a
            }
            val mergedCategories = (localCategories + remoteNotesPayload?.categories.orEmpty()).distinct()
                .ifEmpty { RemainderrRepository.defaultNoteCategories }
            repository.replaceNotesAndCategories(mergedNotes, mergedCategories)
            upsertJsonFile(
                accessToken,
                folderId,
                FILE_NOTES,
                json.encodeToString(NotesSyncPayload(notes = mergedNotes, categories = mergedCategories)),
            )

            // 2. Tasks
            val localTasks = repository.getTasksOnce()
            val remoteTasks = readFileText(accessToken, folderId, FILE_TASKS)?.let { decodeTasks(it) }.orEmpty()
            val mergedTasks = mergeById(localTasks, remoteTasks) { a, b -> b }
            repository.replaceTasks(mergedTasks)
            upsertJsonFile(accessToken, folderId, FILE_TASKS, json.encodeToString(mergedTasks))

            // 3. Transactions (+ Credit Cards)
            val localTxs = repository.getTransactionsOnce()
            val localCards = repository.getCreditCardsOnce()
            val remoteTxPayload = readFileText(accessToken, folderId, FILE_TRANSACTIONS)?.let { decodeTransactionsPayload(it) }
            val mergedTxs = mergeById(localTxs, remoteTxPayload?.transactions.orEmpty()) { a, b -> b }
            val mergedCards = mergeById(localCards, remoteTxPayload?.creditCards.orEmpty()) { a, b -> b }
            repository.replaceTransactionsAndCards(mergedTxs, mergedCards)
            upsertJsonFile(
                accessToken,
                folderId,
                FILE_TRANSACTIONS,
                json.encodeToString(TransactionsSyncPayload(transactions = mergedTxs, creditCards = mergedCards)),
            )

            // 4. Passwords
            val localPasswords = repository.getPasswordsOnce()
            val remotePasswords = readFileText(accessToken, folderId, FILE_PASSWORDS)?.let { decodePasswords(it) }.orEmpty()
            val mergedPasswords = mergeById(localPasswords, remotePasswords) { a, b -> b }
            repository.replacePasswords(mergedPasswords)
            upsertJsonFile(accessToken, folderId, FILE_PASSWORDS, json.encodeToString(mergedPasswords))

            val now = System.currentTimeMillis()
            secureConfigStore.setLastSyncedAt(now)

            DriveSyncSummary(
                email = email.ifBlank { secureConfigStore.getDriveEmail() },
                folderId = folderId,
                notesCount = mergedNotes.size,
                tasksCount = mergedTasks.size,
                transactionsCount = mergedTxs.size,
                passwordsCount = mergedPasswords.size,
                syncedAt = now,
            )
        }
    }

    suspend fun uploadAll(accessToken: String): Result<DriveSyncSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val email = fetchUserEmail(accessToken).also {
                if (it.isNotBlank()) secureConfigStore.setDriveEmail(it)
            }
            val folderId = ensureSyncFolder(accessToken)

            val notes = repository.getNotesOnce()
            val categories = repository.getCategoriesOnce().ifEmpty { RemainderrRepository.defaultNoteCategories }
            val tasks = repository.getTasksOnce()
            val txs = repository.getTransactionsOnce()
            val cards = repository.getCreditCardsOnce()
            val passwords = repository.getPasswordsOnce()

            upsertJsonFile(
                accessToken,
                folderId,
                FILE_NOTES,
                json.encodeToString(NotesSyncPayload(notes = notes, categories = categories)),
            )
            upsertJsonFile(accessToken, folderId, FILE_TASKS, json.encodeToString(tasks))
            upsertJsonFile(
                accessToken,
                folderId,
                FILE_TRANSACTIONS,
                json.encodeToString(TransactionsSyncPayload(transactions = txs, creditCards = cards)),
            )
            upsertJsonFile(accessToken, folderId, FILE_PASSWORDS, json.encodeToString(passwords))

            val now = System.currentTimeMillis()
            secureConfigStore.setLastSyncedAt(now)

            DriveSyncSummary(
                email = email.ifBlank { secureConfigStore.getDriveEmail() },
                folderId = folderId,
                notesCount = notes.size,
                tasksCount = tasks.size,
                transactionsCount = txs.size,
                passwordsCount = passwords.size,
                syncedAt = now,
            )
        }
    }

    suspend fun restoreAll(accessToken: String): Result<DriveSyncSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val email = fetchUserEmail(accessToken).also {
                if (it.isNotBlank()) secureConfigStore.setDriveEmail(it)
            }
            val folderId = ensureSyncFolder(accessToken)

            val notesText = readFileText(accessToken, folderId, FILE_NOTES)
            val tasksText = readFileText(accessToken, folderId, FILE_TASKS)
            val txsText = readFileText(accessToken, folderId, FILE_TRANSACTIONS)
            val passwordsText = readFileText(accessToken, folderId, FILE_PASSWORDS)

            if (notesText == null && tasksText == null && txsText == null && passwordsText == null) {
                throw IllegalStateException("No synced files found inside \"$SYNC_FOLDER_NAME\" on Google Drive.")
            }

            var notesCount = repository.getNotesOnce().size
            var tasksCount = repository.getTasksOnce().size
            var txsCount = repository.getTransactionsOnce().size
            var passwordsCount = repository.getPasswordsOnce().size

            notesText?.let {
                val payload = decodeNotesPayload(it)
                repository.replaceNotesAndCategories(payload.notes, payload.categories)
                notesCount = payload.notes.size
            }
            tasksText?.let {
                val tasks = decodeTasks(it)
                repository.replaceTasks(tasks)
                tasksCount = tasks.size
            }
            txsText?.let {
                val payload = decodeTransactionsPayload(it)
                repository.replaceTransactionsAndCards(payload.transactions, payload.creditCards)
                txsCount = payload.transactions.size
            }
            passwordsText?.let {
                val passwords = decodePasswords(it)
                repository.replacePasswords(passwords)
                passwordsCount = passwords.size
            }

            val now = System.currentTimeMillis()
            secureConfigStore.setLastSyncedAt(now)

            DriveSyncSummary(
                email = email.ifBlank { secureConfigStore.getDriveEmail() },
                folderId = folderId,
                notesCount = notesCount,
                tasksCount = tasksCount,
                transactionsCount = txsCount,
                passwordsCount = passwordsCount,
                syncedAt = now,
            )
        }
    }

    fun disconnect() {
        secureConfigStore.clearDriveSyncMetadata()
    }

    private fun decodeNotesPayload(text: String): NotesSyncPayload {
        return runCatching { json.decodeFromString<NotesSyncPayload>(text) }.getOrElse {
            val notes = runCatching { json.decodeFromString<List<NoteEntity>>(text) }.getOrDefault(emptyList())
            NotesSyncPayload(notes = notes, categories = RemainderrRepository.defaultNoteCategories)
        }
    }

    private fun decodeTasks(text: String): List<TaskEntity> {
        return runCatching { json.decodeFromString<List<TaskEntity>>(text) }.getOrDefault(emptyList())
    }

    private fun decodeTransactionsPayload(text: String): TransactionsSyncPayload {
        return runCatching { json.decodeFromString<TransactionsSyncPayload>(text) }.getOrElse {
            val txs = runCatching { json.decodeFromString<List<TransactionEntity>>(text) }.getOrDefault(emptyList())
            TransactionsSyncPayload(transactions = txs, creditCards = emptyList())
        }
    }

    private fun decodePasswords(text: String): List<PasswordEntity> {
        return runCatching { json.decodeFromString<List<PasswordEntity>>(text) }.getOrDefault(emptyList())
    }

    private fun <T : Any> mergeById(
        local: List<T>,
        remote: List<T>,
        resolveConflict: (T, T) -> T,
    ): List<T> {
        val map = linkedMapOf<String, T>()
        local.forEach { map[itemIdOf(it)] = it }
        remote.forEach { r ->
            val key = itemIdOf(r)
            val existing = map[key]
            map[key] = if (existing == null) r else resolveConflict(existing, r)
        }
        return map.values.toList()
    }

    private fun itemIdOf(item: Any): String = when (item) {
        is NoteEntity -> item.id
        is TaskEntity -> item.id
        is TransactionEntity -> item.id
        is CreditCardEntity -> item.id
        is PasswordEntity -> item.id
        else -> item.hashCode().toString()
    }

    private fun fetchUserEmail(accessToken: String): String {
        val url = URL("https://www.googleapis.com/oauth2/v2/userinfo")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) return ""
        return runCatching {
            json.parseToJsonElement(body).jsonObject["email"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }.getOrDefault("")
    }

    private fun ensureSyncFolder(accessToken: String): String {
        val cached = secureConfigStore.getDriveFolderId()
        if (cached.isNotBlank() && isFolderActive(accessToken, cached)) {
            return cached
        }

        val query = "name = '$SYNC_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val searchUrl = URL(
            "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)&spaces=drive",
        )
        val searchResp = executeGet(searchUrl, accessToken)
        val existingId = runCatching {
            json.parseToJsonElement(searchResp).jsonObject["files"]
                ?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
        }.getOrNull()

        if (!existingId.isNullOrBlank()) {
            secureConfigStore.setDriveFolderId(existingId)
            return existingId
        }

        val createUrl = URL("https://www.googleapis.com/drive/v3/files?fields=id")
        val metadataJson = buildJsonObject {
            put("name", SYNC_FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }.toString()
        val createResp = executeJsonRequest(createUrl, "POST", accessToken, metadataJson)
        val createdId = json.parseToJsonElement(createResp).jsonObject["id"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("Could not create Google Drive folder \"$SYNC_FOLDER_NAME\".")
        secureConfigStore.setDriveFolderId(createdId)
        return createdId
    }

    private fun isFolderActive(accessToken: String, folderId: String): Boolean {
        return runCatching {
            val url = URL("https://www.googleapis.com/drive/v3/files/$folderId?fields=id,trashed")
            val body = executeGet(url, accessToken)
            val obj = json.parseToJsonElement(body).jsonObject
            val trashed = obj["trashed"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            !trashed && obj["id"] != null
        }.getOrDefault(false)
    }

    private fun findFileId(accessToken: String, folderId: String, fileName: String): String? {
        val query = "'$folderId' in parents and name = '$fileName' and trashed = false"
        val url = URL(
            "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)",
        )
        val resp = executeGet(url, accessToken)
        return runCatching {
            json.parseToJsonElement(resp).jsonObject["files"]
                ?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    private fun readFileText(accessToken: String, folderId: String, fileName: String): String? {
        val fileId = findFileId(accessToken, folderId, fileName) ?: return null
        val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
        return executeGet(url, accessToken)
    }

    private fun upsertJsonFile(accessToken: String, folderId: String, fileName: String, content: String) {
        val existingId = findFileId(accessToken, folderId, fileName)
        if (existingId != null) {
            val url = URL("https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media")
            executeJsonRequest(url, "PATCH", accessToken, content)
        } else {
            val boundary = "remainderr_${UUID.randomUUID()}"
            val metadata = buildJsonObject {
                put("name", fileName)
                put("parents", kotlinx.serialization.json.buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive(folderId))
                })
                put("mimeType", "application/json")
            }.toString()

            val body = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(content).append("\r\n")
                append("--").append(boundary).append("--")
            }

            val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) {
                throw IllegalStateException("Google Drive upload failed ($code): ${extractDriveError(resp)}")
            }
        }
    }

    private fun executeGet(url: URL, accessToken: String): String {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        val code = conn.responseCode
        val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) {
            throw IllegalStateException("Google Drive request failed ($code): ${extractDriveError(resp)}")
        }
        return resp
    }

    private fun executeJsonRequest(url: URL, method: String, accessToken: String, payload: String): String {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) {
            throw IllegalStateException("Google Drive request failed ($code): ${extractDriveError(resp)}")
        }
        return resp
    }

    private fun extractDriveError(raw: String): String {
        return runCatching {
            json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: raw.ifBlank { "Unknown Drive API error" }
    }
}
