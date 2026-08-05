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

        // Default Cloudinary configuration
        const val DEFAULT_CLOUD_NAME = "dne01qj9q"
        const val DEFAULT_UPLOAD_PRESET = "ml_default"
        const val DEFAULT_API_KEY = "388539997418443"
        const val DEFAULT_API_SECRET = "s4lRnhfb0CetDZ11bKx72cGoqJM"
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
        return CloudinaryConfig(
            cloudName = prefs.getString(KEY_CLOUD_NAME, DEFAULT_CLOUD_NAME)?.ifBlank { DEFAULT_CLOUD_NAME } ?: DEFAULT_CLOUD_NAME,
            uploadPreset = prefs.getString(KEY_UPLOAD_PRESET, DEFAULT_UPLOAD_PRESET)?.ifBlank { DEFAULT_UPLOAD_PRESET } ?: DEFAULT_UPLOAD_PRESET,
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
    }

    /**
     * Upload an Audio file (MP3, WAV, AAC) or Image (JPG, PNG) from a local Uri.
     * Always saves a local copy in internal storage so playback never fails.
     */
    suspend fun uploadFromUri(
        uri: Uri,
        fileName: String,
        isAudio: Boolean,
        onProgressUpdate: (String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        if (context == null) {
            return@withContext Result.failure(Exception("Context is required to read URI"))
        }

        try {
            onProgressUpdate("Leyendo archivo del dispositivo...")
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo seleccionado"))

            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return@withContext Result.failure(Exception("El archivo seleccionado está vacío"))
            }

            // Always save a permanent local copy in app internal storage
            val targetDir = File(context.filesDir, if (isAudio) "audio" else "covers")
            if (!targetDir.exists()) targetDir.mkdirs()
            val cleanName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val localSavedFile = File(targetDir, "saved_${System.currentTimeMillis()}_$cleanName")
            FileOutputStream(localSavedFile).use { it.write(bytes) }

            val resourceType = if (isAudio) "video" else "image"
            onProgressUpdate("Subiendo $fileName a Cloudinary (${bytes.size / 1024} KB)...")

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
                // If Cloudinary fails, return the saved local file path so audio plays flawlessly on device
                Log.w(TAG, "Cloudinary upload failed, using local storage: ${uploadResult.exceptionOrNull()?.message}")
                onProgressUpdate("Audio guardado en almacenamiento del celular.")
                Result.success(localSavedFile.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadFromUri: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads raw ByteArray to Cloudinary.
     */
    suspend fun uploadBytes(
        fileBytes: ByteArray,
        fileName: String,
        resourceType: String = "auto"
    ): Result<String> = withContext(Dispatchers.IO) {
        val config = getConfig()
        val cloudName = config.cloudName
        val uploadPreset = config.uploadPreset
        val apiKey = config.apiKey
        val apiSecret = config.apiSecret

        val actualResourceType = when {
            resourceType == "image" || fileName.endsWith(".jpg", true) || fileName.endsWith(".png", true) || fileName.endsWith(".jpeg", true) -> "image"
            fileName.endsWith(".mp3", true) || fileName.endsWith(".wav", true) || fileName.endsWith(".m4a", true) ||
            fileName.endsWith(".aac", true) || resourceType == "video" || resourceType == "audio" -> "video"
            else -> "auto"
        }

        val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/$actualResourceType/upload"
        val mediaType = if (actualResourceType == "video") "audio/mpeg".toMediaTypeOrNull() else "image/jpeg".toMediaTypeOrNull()

        // 1. Try Signed Upload with API Key & Secret if configured
        if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            try {
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
                    Log.d(TAG, "Cloudinary signed response ($endpoint) code: ${response.code}")
                    if (response.isSuccessful) {
                        val json = JSONObject(responseBody)
                        val secureUrl = json.optString("secure_url", json.optString("url", ""))
                        if (secureUrl.isNotBlank()) {
                            return@withContext Result.success(secureUrl)
                        }
                    } else {
                        Log.w(TAG, "Cloudinary signed upload error: ${response.code} $responseBody")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Signed upload exception: ${e.message}")
            }
        }

        // 2. Try Unsigned Upload with Preset
        if (uploadPreset.isNotBlank()) {
            try {
                val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", uploadPreset)
                    .addFormDataPart("file", fileName, fileBytes.toRequestBody(mediaType))

                val request = Request.Builder().url(endpoint).post(bodyBuilder.build()).build()
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Cloudinary unsigned response code: ${response.code}")
                    if (response.isSuccessful) {
                        val json = JSONObject(responseBody)
                        val secureUrl = json.optString("secure_url", json.optString("url", ""))
                        if (secureUrl.isNotBlank()) {
                            return@withContext Result.success(secureUrl)
                        }
                    } else {
                        Log.w(TAG, "Cloudinary unsigned upload error: ${response.code} $responseBody")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unsigned upload exception: ${e.message}")
            }
        }

        Result.failure(Exception("No se pudo subir a Cloudinary. Verifica tu Cloud Name y Upload Preset o credenciales API."))
    }

    /**
     * Uploads a remote media URL directly into Cloudinary
     */
    suspend fun uploadRemoteUrl(
        remoteUrl: String,
        resourceType: String = "video"
    ): Result<String> = withContext(Dispatchers.IO) {
        val config = getConfig()
        val cloudName = config.cloudName
        val uploadPreset = config.uploadPreset
        val actualResourceType = if (resourceType == "image") "image" else "video"
        val endpoint = "https://api.cloudinary.com/v1_1/$cloudName/$actualResourceType/upload"

        // Try Unsigned remote URL ingestion
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

        // Return the remote URL if already a secure CDN
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
