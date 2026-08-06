package com.example.data.api

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class CloudinaryConfig(
    val cloudName: String,
    val uploadPreset: String,
    val apiKey: String,
    val apiSecret: String
)

class CloudinaryUploader(private val context: Context? = null) {

    companion object {
        private const val TAG = "CloudinaryUploader"
        private const val PREFS_NAME = "musica_cristiana_cloudinary_prefs"
        private const val KEY_CLOUD_NAME = "cloudinary_cloud_name"
        private const val KEY_UPLOAD_PRESET = "cloudinary_upload_preset"
        private const val KEY_API_KEY = "cloudinary_api_key"
        private const val KEY_API_SECRET = "cloudinary_api_secret"

        // Default Cloudinary configuration (Set to user's real Cloudinary account)
        const val DEFAULT_CLOUD_NAME = "lucnzuxs"
        const val DEFAULT_UPLOAD_PRESET = "musica_cristiana"
        const val DEFAULT_API_KEY = ""
        const val DEFAULT_API_SECRET = ""
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getConfig(): CloudinaryConfig {
        if (context == null) {
            return CloudinaryConfig(DEFAULT_CLOUD_NAME, DEFAULT_UPLOAD_PRESET, DEFAULT_API_KEY, DEFAULT_API_SECRET)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_CLOUD_NAME, null)
        val cloudName = if (savedName.isNullOrBlank() || savedName == "dne01qj9q") DEFAULT_CLOUD_NAME else savedName
        val savedPreset = prefs.getString(KEY_UPLOAD_PRESET, null)
        val uploadPreset = if (savedPreset.isNullOrBlank() || savedPreset == "ml_default") DEFAULT_UPLOAD_PRESET else savedPreset

        return CloudinaryConfig(
            cloudName = cloudName,
            uploadPreset = uploadPreset,
            apiKey = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY)?.ifBlank { DEFAULT_API_KEY } ?: DEFAULT_API_KEY,
            apiSecret = prefs.getString(KEY_API_SECRET, DEFAULT_API_SECRET)?.ifBlank { DEFAULT_API_SECRET } ?: DEFAULT_API_SECRET
        )
    }

    fun saveConfig(cloudName: String, uploadPreset: String, apiKey: String, apiSecret: String) {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.apply {
            putString(KEY_CLOUD_NAME, cloudName.trim())
            putString(KEY_UPLOAD_PRESET, uploadPreset.trim())
            putString(KEY_API_KEY, apiKey.trim())
            putString(KEY_API_SECRET, apiSecret.trim())
            apply()
        }
        Log.i(TAG, "Saved new Cloudinary configuration: cloudName=${cloudName.trim()}, preset=${uploadPreset.trim()}")
    }

    /**
     * Upload an Audio file (MP3, WAV, AAC, M4A) or Image (JPG, PNG) from a local Uri.
     * Always saves a local copy in internal storage so offline playback works.
     */
    suspend fun uploadFromUri(
        uri: Uri,
        fileName: String,
        isAudio: Boolean,
        onProgressUpdate: (String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        if (context == null) {
            return@withContext Result.failure(Exception("Contexto requerido para leer el archivo"))
        }

        try {
            onProgressUpdate("Leyendo archivo del dispositivo...")
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo seleccionado en el celular."))

            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return@withContext Result.failure(Exception("El archivo seleccionado está vacío (0 bytes)."))
            }

            // Always save a permanent local backup in app internal storage
            val targetDir = File(context.filesDir, if (isAudio) "audio" else "covers")
            if (!targetDir.exists()) targetDir.mkdirs()
            val cleanName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val localSavedFile = File(targetDir, "saved_${System.currentTimeMillis()}_$cleanName")
            FileOutputStream(localSavedFile).use { it.write(bytes) }

            val resourceType = if (isAudio) "video" else "image"
            onProgressUpdate("Subiendo $cleanName a Cloudinary (${bytes.size / 1024} KB)...")

            val uploadResult = uploadBytes(
                fileBytes = bytes,
                fileName = cleanName,
                resourceType = resourceType
            )

            if (uploadResult.isSuccess) {
                val cloudUrl = uploadResult.getOrThrow()
                onProgressUpdate("¡Subida a Cloudinary exitosa!")
                Result.success(cloudUrl)
            } else {
                val err = uploadResult.exceptionOrNull()?.message ?: "Error desconocido"
                Log.w(TAG, "Cloudinary upload failed: $err")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadFromUri: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads raw ByteArray to Cloudinary with candidate preset fallbacks.
     */
    suspend fun uploadBytes(
        fileBytes: ByteArray,
        fileName: String,
        resourceType: String = "auto"
    ): Result<String> = withContext(Dispatchers.IO) {
        val config = getConfig()
        val cloudName = config.cloudName.trim().ifBlank { DEFAULT_CLOUD_NAME }
        val uploadPreset = config.uploadPreset.trim().ifBlank { DEFAULT_UPLOAD_PRESET }
        val apiKey = config.apiKey.trim()
        val apiSecret = config.apiSecret.trim()

        val isAudio = fileName.endsWith(".mp3", true) || fileName.endsWith(".wav", true) ||
                fileName.endsWith(".m4a", true) || fileName.endsWith(".aac", true) ||
                fileName.endsWith(".ogg", true) || resourceType == "video" || resourceType == "audio"

        val actualResourceType = if (isAudio) "video" else if (resourceType == "image") "image" else "auto"
        val mediaType = if (isAudio) "audio/mpeg".toMediaTypeOrNull() else "image/jpeg".toMediaTypeOrNull()

        // Build candidate presets list
        val presetCandidates = linkedSetOf<String>()
        if (uploadPreset.isNotBlank()) {
            presetCandidates.add(uploadPreset) // e.g. "música cristiana"
            val accentedUnderscore = uploadPreset.replace(" ", "_")
            presetCandidates.add(accentedUnderscore) // "música_cristiana"

            val unaccented = uploadPreset
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
                .replace("Á", "a").replace("É", "e").replace("Í", "i")
                .replace("Ó", "o").replace("Ú", "u").replace("Ñ", "n")
            
            presetCandidates.add(unaccented) // "musica cristiana"
            presetCandidates.add(unaccented.replace(" ", "_")) // "musica_cristiana"
            presetCandidates.add(unaccented.replace(" ", "-")) // "musica-cristiana"
            presetCandidates.add(unaccented.replace(" ", "")) // "musicacristiana"
        }
        presetCandidates.add("música cristiana")
        presetCandidates.add("musica_cristiana")
        presetCandidates.add("musica cristiana")
        presetCandidates.add("ml_default")

        var lastErrorMessage = ""

        // Try candidate endpoints (video/image and auto)
        val endpointsToTry = if (actualResourceType != "auto") {
            listOf(actualResourceType, "auto")
        } else {
            listOf("auto", "video", "image")
        }

        for (resType in endpointsToTry) {
            for (preset in presetCandidates) {
                try {
                    val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/$resType/upload"
                    val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("upload_preset", preset)
                        .addFormDataPart("file", fileName, fileBytes.toRequestBody(mediaType))

                    val request = Request.Builder().url(endpoint).post(bodyBuilder.build()).build()
                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        Log.d(TAG, "Cloudinary upload ($endpoint, preset=$preset) -> HTTP ${response.code}")
                        if (response.isSuccessful) {
                            val json = JSONObject(responseBody)
                            val secureUrl = json.optString("secure_url", json.optString("url", ""))
                            if (secureUrl.isNotBlank()) {
                                Log.i(TAG, "Successfully uploaded to Cloudinary: $secureUrl")
                                return@withContext Result.success(secureUrl)
                            }
                        } else {
                            val errJson = try { JSONObject(responseBody).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
                            lastErrorMessage = if (!errJson.isNullOrBlank()) {
                                "Error Cloudinary ($preset): $errJson"
                            } else {
                                "Error HTTP ${response.code}: $responseBody"
                            }
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMessage = "Error de red: ${e.message}"
                    Log.w(TAG, "Upload attempt failed: ${e.message}")
                }
            }
        }

        // 2. Try Signed Upload with API Key & Secret if configured
        if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            try {
                val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/$actualResourceType/upload"
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val stringToSign = "timestamp=$timestamp$apiSecret"
                val signature = sha1(stringToSign)

                val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("api_key", apiKey)
                    .addFormDataPart("timestamp", timestamp)
                    .addFormDataPart("signature", signature)
                    .addFormDataPart("file", fileName, fileBytes.toRequestBody(mediaType))

                val request = Request.Builder().url(endpoint).post(bodyBuilder.build()).build()
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val json = JSONObject(responseBody)
                        val secureUrl = json.optString("secure_url", json.optString("url", ""))
                        if (secureUrl.isNotBlank()) {
                            return@withContext Result.success(secureUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Signed upload failed: ${e.message}")
            }
        }

        val finalError = if (lastErrorMessage.isNotBlank()) {
            lastErrorMessage
        } else {
            "No se pudo subir a Cloudinary. Verifica que el Cloud Name '$cloudName' y el Preset '$uploadPreset' (No firmado / Unsigned) estén activos."
        }

        Result.failure(Exception(finalError))
    }

    suspend fun uploadRemoteUrl(
        remoteUrl: String,
        resourceType: String = "video"
    ): Result<String> = withContext(Dispatchers.IO) {
        val config = getConfig()
        val cloudName = config.cloudName.trim().ifBlank { DEFAULT_CLOUD_NAME }
        val uploadPreset = config.uploadPreset.trim().ifBlank { DEFAULT_UPLOAD_PRESET }
        val actualResourceType = if (resourceType == "image") "image" else "video"
        val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/$actualResourceType/upload"

        try {
            val body = okhttp3.FormBody.Builder()
                .add("file", remoteUrl)
                .add("upload_preset", uploadPreset)
                .build()

            val request = Request.Builder().url(endpoint).post(body).build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val secureUrl = json.optString("secure_url", json.optString("url", ""))
                    if (secureUrl.isNotBlank()) {
                        return@withContext Result.success(secureUrl)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Remote URL upload exception: ${e.message}")
        }

        if (remoteUrl.startsWith("https://")) {
            Result.success(remoteUrl)
        } else {
            Result.failure(Exception("No se pudo subir la URL remota a Cloudinary"))
        }
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
