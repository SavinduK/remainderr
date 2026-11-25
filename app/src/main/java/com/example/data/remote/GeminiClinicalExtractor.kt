package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.local.ClinicalHistoryStore
import com.example.data.local.SecureConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class GeminiClinicalExtractor(
    private val context: Context,
    private val secureConfigStore: SecureConfigStore,
    private val historyStore: ClinicalHistoryStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun extractAndSave(imageUris: List<Uri>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = secureConfigStore.getApiKey()
            val model = secureConfigStore.getModel().ifBlank { SecureConfigStore.DEFAULT_MODEL }

            require(apiKey.isNotBlank()) {
                "Please configure Gemini API Key in Settings."
            }
            require(imageUris.isNotEmpty()) {
                "Please select at least one image first."
            }

            val templateStr = historyStore.getTemplateJson()
            val prompt = """
                You are a medical assistant extracting clinical history from handwritten notes across ${imageUris.size} page(s). 
                Analyze all provided images in sequence and fill in the following JSON template with the consolidated extracted information. 
                Return strictly valid JSON without any markdown code blocks. 
                Template: $templateStr
            """.trimIndent()

            val partsArray = buildJsonArray {
                add(buildJsonObject { put("text", prompt) })
                imageUris.forEach { uri ->
                    val b64 = encodeUriToJpegBase64(uri)
                    add(
                        buildJsonObject {
                            put(
                                "inline_data",
                                buildJsonObject {
                                    put("mime_type", "image/jpeg")
                                    put("data", b64)
                                },
                            )
                        },
                    )
                }
            }

            val requestBody = buildJsonObject {
                put(
                    "contents",
                    buildJsonArray {
                        add(buildJsonObject { put("parts", partsArray) })
                    },
                )
            }.toString()

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 90_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            conn.outputStream.use { out ->
                out.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()

            val root = json.parseToJsonElement(responseText).jsonObject
            root["error"]?.let { errObj ->
                val msg = errObj.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                    ?: "Gemini API returned an error."
                throw IllegalStateException(msg)
            }

            val rawOutput = root["candidates"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.contentOrNull
                ?: throw IllegalStateException("Empty response from Gemini API.")

            val cleanedJson = rawOutput
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val parsedElement = json.parseToJsonElement(cleanedJson)
            val prettyJson = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), parsedElement)

            val patientName = (parsedElement as? JsonObject)
                ?.get("clinical_history_template")?.let { it as? JsonObject }
                ?.get("1_patient_identification")?.let { it as? JsonObject }
                ?.get("name")?.let { it as? JsonPrimitive }
                ?.contentOrNull
                ?.takeIf { it.isNotBlank() && it != "null" }
                ?: "Unknown"

            val safeName = patientName.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "${safeName}_${System.currentTimeMillis()}.json"
            historyStore.writeHistoryFile(fileName, prettyJson)
            fileName
        }
    }

    private fun encodeUriToJpegBase64(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
                ?: throw IllegalArgumentException("Unable to decode selected image.")
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            bitmap.recycle()
            out.toByteArray()
        } ?: throw IllegalArgumentException("Unable to open selected image.")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
