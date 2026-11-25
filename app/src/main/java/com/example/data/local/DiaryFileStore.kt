package com.example.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

@Serializable
data class DiaryEntryJson(
    val id: Long,
    val title: String,
    val text: String,
    val date: String,
    val images: List<String> = emptyList(),
)

data class DiaryEntryModel(
    val folderName: String,
    val folderPath: String,
    val id: Long,
    val title: String,
    val text: String,
    val dateIso: String,
    val imagePaths: List<String>,
)

class DiaryFileStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun getAppDir(): File {
        val dir = File(context.filesDir, "App")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun loadEntries(): List<DiaryEntryModel> = withContext(Dispatchers.IO) {
        val appDir = getAppDir()
        val folders = appDir.listFiles { file -> file.isDirectory } ?: return@withContext emptyList()
        folders.mapNotNull { entryDir ->
            val dataFile = File(entryDir, "data.json")
            if (!dataFile.exists()) return@mapNotNull null
            runCatching {
                val parsed = json.decodeFromString<DiaryEntryJson>(dataFile.readText())
                val imgsDir = File(entryDir, "imgs")
                val absoluteImagePaths = parsed.images.mapNotNull { fileName ->
                    val imgFile = File(imgsDir, fileName)
                    if (imgFile.exists()) imgFile.absolutePath else null
                }
                DiaryEntryModel(
                    folderName = entryDir.name,
                    folderPath = entryDir.absolutePath,
                    id = parsed.id,
                    title = parsed.title,
                    text = parsed.text,
                    dateIso = parsed.date,
                    imagePaths = absoluteImagePaths,
                )
            }.getOrNull()
        }.sortedByDescending { it.id }
    }

    suspend fun loadEntry(folderPath: String): DiaryEntryModel? = withContext(Dispatchers.IO) {
        val entryDir = File(folderPath)
        val dataFile = File(entryDir, "data.json")
        if (!entryDir.exists() || !dataFile.exists()) return@withContext null
        runCatching {
            val parsed = json.decodeFromString<DiaryEntryJson>(dataFile.readText())
            val imgsDir = File(entryDir, "imgs")
            val absoluteImagePaths = parsed.images.map { fileName ->
                File(imgsDir, fileName).absolutePath
            }
            DiaryEntryModel(
                folderName = entryDir.name,
                folderPath = entryDir.absolutePath,
                id = parsed.id,
                title = parsed.title,
                text = parsed.text,
                dateIso = parsed.date,
                imagePaths = absoluteImagePaths,
            )
        }.getOrNull()
    }

    suspend fun createEntry(
        title: String,
        text: String,
        dateIso: String,
        pickedImageUris: List<Uri>,
    ): DiaryEntryModel = withContext(Dispatchers.IO) {
        val timestampString = Instant.now().toString().replace(":", "-").replace(".", "-")
        val entryDir = File(getAppDir(), timestampString).apply { mkdirs() }
        val imgsDir = File(entryDir, "imgs").apply { mkdirs() }

        val savedFilenames = mutableListOf<String>()
        pickedImageUris.forEachIndexed { index, uri ->
            val filename = "img_${index}_${System.currentTimeMillis()}.jpg"
            val destFile = File(imgsDir, filename)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
                savedFilenames.add(filename)
            }
        }

        val entryId = System.currentTimeMillis()
        val data = DiaryEntryJson(
            id = entryId,
            title = title,
            text = text,
            date = dateIso,
            images = savedFilenames,
        )
        File(entryDir, "data.json").writeText(json.encodeToString(data))

        DiaryEntryModel(
            folderName = entryDir.name,
            folderPath = entryDir.absolutePath,
            id = entryId,
            title = title,
            text = text,
            dateIso = dateIso,
            imagePaths = savedFilenames.map { File(imgsDir, it).absolutePath },
        )
    }

    suspend fun updateEntry(
        folderPath: String,
        existingId: Long,
        title: String,
        text: String,
        dateIso: String,
        currentSources: List<String>, // either existing file absolutePath or new content:// URI string
    ) = withContext(Dispatchers.IO) {
        val entryDir = File(folderPath)
        if (!entryDir.exists()) entryDir.mkdirs()
        val imgsDir = File(entryDir, "imgs").apply { mkdirs() }

        val savedFilenames = mutableListOf<String>()
        currentSources.forEachIndexed { index, source ->
            if (source.startsWith(entryDir.absolutePath)) {
                val filename = File(source).name
                savedFilenames.add(filename)
            } else {
                val uri = Uri.parse(source)
                val filename = "img_${index}_${System.currentTimeMillis()}.jpg"
                val destFile = File(imgsDir, filename)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    savedFilenames.add(filename)
                }
            }
        }

        // Remove unreferenced images in imgsDir
        imgsDir.listFiles()?.forEach { file ->
            if (file.name !in savedFilenames) {
                file.delete()
            }
        }

        val data = DiaryEntryJson(
            id = existingId,
            title = title,
            text = text,
            date = dateIso,
            images = savedFilenames,
        )
        File(entryDir, "data.json").writeText(json.encodeToString(data))
    }

    suspend fun deleteEntry(folderPath: String) = withContext(Dispatchers.IO) {
        File(folderPath).deleteRecursively()
    }
}
