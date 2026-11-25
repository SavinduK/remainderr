package com.example.data.local

import android.content.Context
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClinicalHistoryFile(
    val fileName: String,
    val displayName: String,
    val formattedDate: String,
    val lastModified: Long,
)

class ClinicalHistoryStore(private val context: Context) {

    fun getHistoriesDir(): File {
        val dir = File(context.filesDir, "histories")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getTemplateFile(): File = File(context.filesDir, "template.json")

    suspend fun getTemplateJson(): String = withContext(Dispatchers.IO) {
        val custom = getTemplateFile()
        if (custom.exists()) {
            custom.readText()
        } else {
            context.resources.openRawResource(R.raw.default_clinical_template)
                .bufferedReader()
                .use { it.readText() }
        }
    }

    suspend fun saveTemplateJson(template: String) = withContext(Dispatchers.IO) {
        getTemplateFile().writeText(template)
    }

    suspend fun listHistoryFiles(): List<ClinicalHistoryFile> = withContext(Dispatchers.IO) {
        val dir = getHistoriesDir()
        val dateFmt = SimpleDateFormat("MMMM d", Locale.US)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return@withContext emptyList()
        files.map { file ->
            val mod = file.lastModified()
            ClinicalHistoryFile(
                fileName = file.name,
                displayName = formatDisplayName(file.name),
                formattedDate = if (mod > 0L) dateFmt.format(Date(mod)) else "",
                lastModified = mod,
            )
        }.sortedByDescending { it.lastModified }
    }

    suspend fun readHistoryFile(fileName: String): String = withContext(Dispatchers.IO) {
        File(getHistoriesDir(), fileName).readText()
    }

    suspend fun writeHistoryFile(fileName: String, jsonContent: String): File = withContext(Dispatchers.IO) {
        val file = File(getHistoriesDir(), fileName)
        file.writeText(jsonContent)
        file
    }

    suspend fun deleteHistoryFile(fileName: String) = withContext(Dispatchers.IO) {
        File(getHistoriesDir(), fileName).delete()
    }

    fun getHistoryFile(fileName: String): File = File(getHistoriesDir(), fileName)

    fun formatDisplayName(fileName: String): String {
        return fileName
            .removeSuffix(".json")
            .replace(Regex("_\\d+$"), "")
    }
}
