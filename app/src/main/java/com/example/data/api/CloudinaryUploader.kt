package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class CloudinaryUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadAudioOrImage(
        fileBytes: ByteArray,
        fileName: String,
        resourceType: String = "auto", // "video" for mp3/audio, "image" for cover
        customCloudName: String? = null,
        customUploadPreset: String? = null,
        customApiKey: String? = null,
        customApiSecret: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cloudName = customCloudName?.ifEmpty { null } ?: "dne01qj9q"
            val uploadPreset = customUploadPreset?.ifEmpty { null } ?: "ml_default"
            val apiKey = customApiKey?.ifEmpty { null } ?: "388539997418443"
            val apiSecret = customApiSecret?.ifEmpty { null } ?: "s4lRnhfb0CetDZ11bKx72cGoqJM"

            val url = "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload"

            val mediaType = if (fileName.endsWith(".mp3", true) || resourceType == "video") {
                "audio/mpeg".toMediaTypeOrNull()
            } else {
                "image/jpeg".toMediaTypeOrNull()
            }

            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

            if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                // Sign parameters according to Cloudinary spec
                val stringToSign = "timestamp=$timestamp$apiSecret"
                val signature = sha1(stringToSign)

                builder.addFormDataPart("api_key", apiKey)
                builder.addFormDataPart("timestamp", timestamp)
                builder.addFormDataPart("signature", signature)
            } else {
                builder.addFormDataPart("upload_preset", uploadPreset)
            }

            builder.addFormDataPart(
                "file",
                fileName,
                fileBytes.toRequestBody(mediaType)
            )

            val requestBody = builder.build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val secureUrl = json.optString("secure_url", json.optString("url", ""))
                    if (secureUrl.isNotBlank()) {
                        Result.success(secureUrl)
                    } else {
                        Result.failure(Exception("Cloudinary did not return a valid secure_url"))
                    }
                } else {
                    Result.failure(Exception("Cloudinary upload failed: ${response.code} $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
