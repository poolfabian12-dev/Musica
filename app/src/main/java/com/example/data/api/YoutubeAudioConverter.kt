package com.example.data.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class YoutubeVideoInfo(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val durationSeconds: Int = 240,
    val directAudioStreamUrl: String? = null
)

data class YoutubeConversionResult(
    val title: String,
    val artist: String,
    val cloudinaryAudioUrl: String,
    val cloudinaryCoverUrl: String,
    val durationSeconds: Int,
    val videoId: String,
    val localFilePath: String = "",
    val isStoredLocally: Boolean = true,
    val isUploadedToCloudinary: Boolean = false,
    val fileSizeKb: Long = 0L
)

class YoutubeAudioConverter(private val context: Context? = null) {

    companion object {
        private const val TAG = "YoutubeAudioConverter"

        // Active Invidious instances cluster
        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.tux.pizza",
            "https://invidious.nerdvpn.de",
            "https://invidious.projectsegfau.lt",
            "https://vid.puffyan.us",
            "https://invidious.no-logs.com",
            "https://invidious.jing.rocks",
            "https://inv.nadeko.net",
            "https://invidious.privacydev.net",
            "https://yewtu.be",
            "https://invidious.private.coffee",
            "https://inv.bp.projectsegfau.lt",
            "https://invidious.einfachzocken.eu"
        )

        // Active Piped instances cluster
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.private.coffee",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.drgns.space",
            "https://piped-api.garudalinux.org",
            "https://pipedapi.leptons.xyz",
            "https://pipedapi.smnz.de",
            "https://pipedapi.colins.link"
        )

        // Cobalt API instances
        private val COBALT_INSTANCES = listOf(
            "https://api.cobalt.tools",
            "https://co.wuk.sh",
            "https://cobalt-api.kwiatekm.pl",
            "https://cobalt.api.red54.de",
            "https://api.wuk.sh",
            "https://cobaltapi.canine.tools",
            "https://cobalt.tools"
        )

        // Public reliable Christian stream fallbacks (used only for initial catalog samples if needed)
        const val DEFAULT_WORSHIP_STREAM = "https://archive.org/download/AmazingGrace_201809/Amazing_Grace.mp3"
        const val DEFAULT_WORSHIP_STREAM_2 = "https://archive.org/download/hymns-and-praise-worship/HowGreatThouArt.mp3"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val cloudinaryUploader = CloudinaryUploader(context)

    /**
     * Extracts YouTube Video ID from any standard format (watch, shorts, embed, youtu.be, live, music).
     */
    fun extractVideoId(url: String): String {
        val cleanUrl = url.trim()
        return when {
            cleanUrl.contains("v=") -> cleanUrl.substringAfter("v=").substringBefore("&").substringBefore("?").substringBefore("#")
            cleanUrl.contains("youtu.be/") -> cleanUrl.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").substringBefore("#")
            cleanUrl.contains("shorts/") -> cleanUrl.substringAfter("shorts/").substringBefore("?").substringBefore("&").substringBefore("#")
            cleanUrl.contains("embed/") -> cleanUrl.substringAfter("embed/").substringBefore("?").substringBefore("&").substringBefore("#")
            cleanUrl.contains("live/") -> cleanUrl.substringAfter("live/").substringBefore("?").substringBefore("&").substringBefore("#")
            cleanUrl.length in 10..12 && !cleanUrl.contains("/") -> cleanUrl
            else -> ""
        }
    }

    /**
     * Fetches metadata (title, artist, high-res cover) from YouTube via oEmbed, Invidious or InnerTube.
     */
    suspend fun fetchVideoInfo(youtubeUrlOrId: String): Result<YoutubeVideoInfo> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(youtubeUrlOrId).ifEmpty { youtubeUrlOrId.trim() }
        if (videoId.isBlank()) {
            return@withContext Result.failure(Exception("ID de video de YouTube no válido"))
        }

        val highResCover = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        val fallbackCover = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        // 1. Try YouTube oEmbed API (Fastest & Most Reliable for metadata)
        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val title = json.optString("title", "Alabanza Cristiana")
                    val author = json.optString("author_name", "Música Cristiana")

                    var songTitle = title
                    var artist = author
                    if (title.contains(" - ")) {
                        val parts = title.split(" - ", limit = 2)
                        artist = parts[0].trim()
                        songTitle = parts[1].trim()
                    } else if (title.contains(" – ")) {
                        val parts = title.split(" – ", limit = 2)
                        artist = parts[0].trim()
                        songTitle = parts[1].trim()
                    }

                    return@withContext Result.success(
                        YoutubeVideoInfo(
                            id = videoId,
                            title = cleanSongTitle(songTitle).ifBlank { "Alabanza Cristiana" },
                            artist = artist.ifBlank { "Música Cristiana" },
                            coverUrl = highResCover,
                            durationSeconds = 240
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "oEmbed fetch notice: ${e.message}")
        }

        // 2. Try Invidious API for metadata + exact duration
        for (instance in INVIDIOUS_INSTANCES.take(4)) {
            try {
                val apiUrl = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val fullTitle = json.optString("title", "")
                        val author = json.optString("author", "Música Cristiana")
                        val lengthSeconds = json.optInt("lengthSeconds", 240)

                        var songTitle = fullTitle
                        var artist = author
                        if (fullTitle.contains(" - ")) {
                            val parts = fullTitle.split(" - ", limit = 2)
                            artist = parts[0].trim()
                            songTitle = parts[1].trim()
                        } else if (fullTitle.contains(" – ")) {
                            val parts = fullTitle.split(" – ", limit = 2)
                            artist = parts[0].trim()
                            songTitle = parts[1].trim()
                        }

                        return@withContext Result.success(
                            YoutubeVideoInfo(
                                id = videoId,
                                title = cleanSongTitle(songTitle).ifBlank { "Alabanza Cristiana" },
                                artist = artist.ifBlank { "Música Cristiana" },
                                coverUrl = highResCover,
                                durationSeconds = if (lengthSeconds > 0) lengthSeconds else 240
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious fetch error ($instance): ${e.message}")
            }
        }

        // Fallback default info
        Result.success(
            YoutubeVideoInfo(
                id = videoId,
                title = "Alabanza Cristiana",
                artist = "Música Cristiana",
                coverUrl = fallbackCover,
                durationSeconds = 240
            )
        )
    }

    /**
     * Resolves a direct audio stream URL or extracts real audio bytes from YouTube using a multi-layer engine:
     * 1. Cobalt MP3 API instances
     * 2. Invidious Audio Streams (with absolute URL resolution)
     * 3. Vevioz & Public MP3 Conversion APIs
     * 4. Piped API Streams
     * 5. YouTube InnerTube (iOS / TV Embedded clients)
     */
    suspend fun resolveDirectAudioStream(videoId: String, titleHint: String? = null): String? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null

        val youtubeWatchUrl = "https://www.youtube.com/watch?v=$videoId"

        // 1. Try Cobalt API instances (Direct MP3 converter)
        for (cobaltBase in COBALT_INSTANCES) {
            try {
                val endpoints = listOf("$cobaltBase/", "$cobaltBase/api/json")
                for (cobaltUrl in endpoints) {
                    val jsonPayload = JSONObject().apply {
                        put("url", youtubeWatchUrl)
                        put("downloadMode", "audio")
                        put("audioFormat", "mp3")
                    }
                    val request = Request.Builder()
                        .url(cobaltUrl)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .post(jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val resBody = response.body?.string() ?: ""
                            val json = JSONObject(resBody)
                            val streamUrl = json.optString("url", "")
                            if (streamUrl.isNotBlank() && streamUrl.startsWith("http")) {
                                Log.i(TAG, "Resolved Cobalt audio stream ($cobaltUrl): $streamUrl")
                                return@withContext streamUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cobalt instance notice ($cobaltBase): ${e.message}")
            }
        }

        // 2. Try Invidious direct stream and format endpoints (itag 140 = M4A Audio, itag 251 = WebM Audio)
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                // Method A: Check Invidious API videos endpoint
                val streamApiUrl = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(streamApiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                        if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                            var bestAudioUrl: String? = null
                            var highestBitrate = 0

                            for (i in 0 until adaptiveFormats.length()) {
                                val fmt = adaptiveFormats.getJSONObject(i)
                                val type = fmt.optString("type", "")
                                val bitrate = fmt.optInt("bitrate", 0)
                                var rawUrl = fmt.optString("url", "")

                                if (type.startsWith("audio/") && rawUrl.isNotBlank()) {
                                    if (rawUrl.startsWith("/")) {
                                        rawUrl = "$instance$rawUrl"
                                    }
                                    if (rawUrl.startsWith("http") && bitrate >= highestBitrate) {
                                        highestBitrate = bitrate
                                        bestAudioUrl = rawUrl
                                    }
                                }
                            }

                            if (!bestAudioUrl.isNullOrBlank()) {
                                Log.i(TAG, "Resolved Invidious audio format ($instance): $bestAudioUrl")
                                return@withContext bestAudioUrl
                            }
                        }
                    }
                }

                // Method B: Direct latest_version endpoint
                val directProxyUrl = "$instance/latest_version?id=$videoId&itag=140"
                val testReq = Request.Builder()
                    .url(directProxyUrl)
                    .head()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                client.newCall(testReq).execute().use { resp ->
                    if (resp.isSuccessful || resp.code == 302 || resp.code == 200) {
                        Log.i(TAG, "Resolved Invidious direct stream: $directProxyUrl")
                        return@withContext directProxyUrl
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious stream check notice ($instance): ${e.message}")
            }
        }

        // 3. Try Vevioz & Public JSON MP3 APIs
        try {
            val veviozUrl = "https://api.vevioz.com/@api/json/mp3/$videoId"
            val req = Request.Builder()
                .url(veviozUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val dlUrl = json.optJSONObject("mp3")?.optString("url")
                        ?: json.optString("url")
                        ?: json.optJSONObject("data")?.optString("url")
                    if (!dlUrl.isNullOrBlank() && dlUrl.startsWith("http")) {
                        Log.i(TAG, "Resolved Vevioz audio stream: $dlUrl")
                        return@withContext dlUrl
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vevioz notice: ${e.message}")
        }

        // 4. Try Piped API instances
        for (instance in PIPED_INSTANCES) {
            try {
                val streamApiUrl = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(streamApiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            for (i in 0 until audioStreams.length()) {
                                val stream = audioStreams.getJSONObject(i)
                                var url = stream.optString("url", "")
                                if (url.isNotBlank()) {
                                    if (url.startsWith("/")) {
                                        url = "$instance$url"
                                    }
                                    if (url.startsWith("http")) {
                                        Log.i(TAG, "Resolved Piped audio stream ($instance): $url")
                                        return@withContext url
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped instance notice: ${e.message}")
            }
        }

        // 5. Try YouTube InnerTube with iOS & TV Clients (Direct deciphered Google Video streams)
        val clientsToTry = listOf(
            JSONObject().apply {
                put("clientName", "IOS")
                put("clientVersion", "19.29.1")
                put("deviceModel", "iPhone16,2")
                put("userAgent", "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; es_US)")
                put("hl", "es")
                put("gl", "US")
            },
            JSONObject().apply {
                put("clientName", "TVHTML5_SIMPLY_EMBEDDED_PLAYER")
                put("clientVersion", "2.0")
                put("hl", "es")
                put("gl", "US")
            }
        )

        for (clientObj in clientsToTry) {
            try {
                val innertubeUrl = "https://www.youtube.com/youtubei/v1/player"
                val payload = JSONObject().apply {
                    put("videoId", videoId)
                    put("context", JSONObject().apply {
                        put("client", clientObj)
                        put("thirdParty", JSONObject().apply {
                            put("embedUrl", "https://www.youtube.com")
                        })
                    })
                }

                val request = Request.Builder()
                    .url(innertubeUrl)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val streamingData = json.optJSONObject("streamingData")
                        if (streamingData != null) {
                            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                            if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                                var bestAudioUrl: String? = null
                                var highestBitrate = 0
                                for (i in 0 until adaptiveFormats.length()) {
                                    val fmt = adaptiveFormats.getJSONObject(i)
                                    val mimeType = fmt.optString("mimeType", "")
                                    val bitrate = fmt.optInt("bitrate", 0)
                                    val url = fmt.optString("url", "")
                                    if (mimeType.startsWith("audio/") && url.isNotBlank() && url.startsWith("http")) {
                                        if (bitrate > highestBitrate) {
                                            highestBitrate = bitrate
                                            bestAudioUrl = url
                                        }
                                    }
                                }
                                if (!bestAudioUrl.isNullOrBlank()) {
                                    Log.i(TAG, "Resolved native InnerTube audio stream (bitrate: $highestBitrate)")
                                    return@withContext bestAudioUrl
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "InnerTube client notice: ${e.message}")
            }
        }

        return@withContext null
    }

    /**
     * Converts a YouTube Video URL to a real, playable MP3/M4A audio file and stores it in app internal storage
     * and optionally uploads to Cloudinary.
     *
     * saveMode: 0 = Both (Cloudinary + Local Storage), 1 = Local Device Storage Only, 2 = Cloudinary Only
     */
    suspend fun convertAndUploadToCloudinary(
        youtubeUrl: String,
        customTitle: String? = null,
        customArtist: String? = null,
        saveMode: Int = 0,
        onProgress: (String) -> Unit = {}
    ): Result<YoutubeConversionResult> = withContext(Dispatchers.IO) {
        try {
            val videoId = extractVideoId(youtubeUrl)
            if (videoId.isBlank()) {
                return@withContext Result.failure(
                    Exception("El enlace ingresado no es válido. Por favor ingresa un enlace completo de YouTube (ej. https://www.youtube.com/watch?v=...)")
                )
            }

            onProgress("1/4 Identificando título y portada en YouTube...")
            val infoResult = fetchVideoInfo(videoId)
            val info = infoResult.getOrNull() ?: YoutubeVideoInfo(
                id = videoId,
                title = customTitle?.ifBlank { null } ?: "Alabanza Cristiana",
                artist = customArtist?.ifBlank { null } ?: "Música Cristiana",
                coverUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
            )

            val finalTitle = customTitle?.trim()?.ifEmpty { null } ?: info.title
            val finalArtist = customArtist?.trim()?.ifEmpty { null } ?: info.artist

            onProgress("2/4 Conectando con los servidores de audio de YouTube...")
            var directStreamUrl = resolveDirectAudioStream(videoId, finalTitle)

            var audioBytes: ByteArray? = null
            if (!directStreamUrl.isNullOrBlank()) {
                onProgress("2/4 Descargando la pista original de audio...")
                audioBytes = downloadAudioBytes(directStreamUrl)
            }

            // If first attempt failed to download audio bytes, try secondary stream extraction
            if (audioBytes == null || audioBytes.isEmpty()) {
                Log.w(TAG, "Primary stream download failed, attempting secondary resolvers for videoId: $videoId")
                for (instance in INVIDIOUS_INSTANCES.take(4)) {
                    val fallbackStream = "$instance/latest_version?id=$videoId&itag=140"
                    audioBytes = downloadAudioBytes(fallbackStream)
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        directStreamUrl = fallbackStream
                        break
                    }
                }
            }

            // CRITICAL CHECK: Real audio bytes MUST be present (> 30KB).
            // If conversion fails, NEVER return generic/fallback audio! Fail explicitly and clearly.
            if (audioBytes == null || audioBytes.size < 30_000) {
                return@withContext Result.failure(
                    Exception(
                        "No se pudo extraer la pista de audio de este video de YouTube (ID: $videoId).\n\n" +
                        "Motivo técnico: YouTube aplicó restricciones de derechos de autor/DRM en este contenido o los servidores de extracción están saturados temporalmente.\n\n" +
                        "👉 Para garantizar que tu alabanza suene siempre con su música original, por favor selecciona la opción 'Elegir MP3 de mi Celular' o ingresa un enlace directo MP3."
                    )
                )
            }

            // 3. Save permanent real audio file in app internal storage
            var localSavedFilePath = ""
            var fileSizeKb = audioBytes.size / 1024L
            if (context != null) {
                val audioDir = File(context.filesDir, "audio")
                if (!audioDir.exists()) audioDir.mkdirs()
                val cachedFile = File(audioDir, "yt_${videoId}.mp3")
                FileOutputStream(cachedFile).use { it.write(audioBytes) }
                localSavedFilePath = cachedFile.absolutePath
                Log.i(TAG, "Saved YouTube MP3 locally: $localSavedFilePath ($fileSizeKb KB)")
            }

            var finalAudioUrl = localSavedFilePath
            var isCloudUploaded = false

            // 4. Upload MP3 to Cloudinary if requested and configured
            if (saveMode != 1) {
                onProgress("3/4 Subiendo MP3 ($fileSizeKb KB) a tu nube de Cloudinary...")
                val uploadBytesRes = cloudinaryUploader.uploadBytes(
                    fileBytes = audioBytes,
                    fileName = "worship_${videoId}.mp3",
                    resourceType = "video"
                )
                if (uploadBytesRes.isSuccess) {
                    finalAudioUrl = uploadBytesRes.getOrThrow()
                    isCloudUploaded = true
                    onProgress("¡Audio MP3 subido exitosamente a Cloudinary!")
                } else {
                    Log.w(TAG, "Cloudinary upload notice: ${uploadBytesRes.exceptionOrNull()?.message}. Using local storage copy.")
                    finalAudioUrl = localSavedFilePath
                }
            }

            // 5. Upload Cover Image to Cloudinary or use high-res URL
            onProgress("4/4 Preparando portada en alta resolución...")
            var cloudinaryCoverUrl = info.coverUrl
            if (saveMode != 1) {
                try {
                    val coverUploadRes = cloudinaryUploader.uploadRemoteUrl(
                        remoteUrl = info.coverUrl,
                        resourceType = "image"
                    )
                    if (coverUploadRes.isSuccess) {
                        cloudinaryCoverUrl = coverUploadRes.getOrThrow()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cover upload notice: ${e.message}")
                }
            }

            onProgress("¡Canción procesada con éxito y lista para reproducir!")

            Result.success(
                YoutubeConversionResult(
                    title = finalTitle,
                    artist = finalArtist,
                    cloudinaryAudioUrl = finalAudioUrl.ifEmpty { localSavedFilePath },
                    cloudinaryCoverUrl = cloudinaryCoverUrl,
                    durationSeconds = info.durationSeconds,
                    videoId = videoId,
                    localFilePath = localSavedFilePath,
                    isStoredLocally = localSavedFilePath.isNotBlank(),
                    isUploadedToCloudinary = isCloudUploaded,
                    fileSizeKb = fileSizeKb
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in convertAndUploadToCloudinary: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads raw audio bytes from a stream URL with streaming buffers and standard browser headers.
     */
    private fun downloadAudioBytes(streamUrl: String?): ByteArray? {
        if (streamUrl.isNullOrBlank()) return null
        try {
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Range", "bytes=0-")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 206) {
                    val body = response.body ?: return null
                    val inputStream: InputStream = body.byteStream()
                    val outputStream = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    val resultBytes = outputStream.toByteArray()
                    if (resultBytes.size > 30_000) {
                        Log.i(TAG, "Successfully downloaded audio bytes: ${resultBytes.size / 1024} KB from $streamUrl")
                        return resultBytes
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error downloading audio stream: ${e.message}")
        }
        return null
    }

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\[.*?(official|video|audio|lyric|en vivo|letra|videoclip|hd|4k|audio oficial|video oficial).*?\\]"), "")
            .replace(Regex("(?i)\\b(video oficial|official video|official audio|audio oficial|en vivo|video con letra|videoclip oficial|letra oficial|hd|4k)\\b"), "")
            .replace(Regex("(?i)\\b(video oficial|official video|official audio|audio oficial|en vivo|video con letra|videoclip oficial|letra oficial|hd|4k)\\b"), "")
            .trim()
    }
}
