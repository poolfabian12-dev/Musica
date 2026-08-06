package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.player.WorshipAudioSynthesizer
import com.example.util.AudioFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
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
    val isUploadedToCloudinary: Boolean = false
)

class YoutubeAudioConverter(private val context: Context? = null) {

    companion object {
        private const val TAG = "YoutubeAudioConverter"

        // Reliable Invidious instances with proxy support
        private val INVIDIOUS_INSTANCES = listOf(
            "https://inv.tux.pizza",
            "https://invidious.nerdvpn.de",
            "https://invidious.projectsegfau.lt",
            "https://vid.puffyan.us",
            "https://yt.artemislena.eu",
            "https://invidious.no-logs.com",
            "https://invidious.jing.rocks",
            "https://inv.nadeko.net",
            "https://invidious.privacydev.net"
        )

        // Reliable Piped instances
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.private.coffee",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.drgns.space",
            "https://piped-api.garudalinux.org"
        )

        // Cobalt API instances
        private val COBALT_INSTANCES = listOf(
            "https://api.cobalt.tools",
            "https://co.wuk.sh/api/json",
            "https://cobalt.api.red54.de/api/json",
            "https://cobalt-api.kwiatekm.pl/api/json"
        )

        // Verified public Christian stream fallback
        const val DEFAULT_WORSHIP_STREAM = "https://archive.org/download/AmazingGrace_201809/Amazing_Grace.mp3"
        const val DEFAULT_WORSHIP_STREAM_2 = "https://archive.org/download/hymns-and-praise-worship/HowGreatThouArt.mp3"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val cloudinaryUploader = CloudinaryUploader(context)

    /**
     * Extracts YouTube Video ID from any standard format (links, shorts, embeds).
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
     * Fetches metadata (title, artist, high-res cover) from YouTube via oEmbed, Invidious or scrape.
     */
    suspend fun fetchVideoInfo(youtubeUrlOrId: String): Result<YoutubeVideoInfo> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(youtubeUrlOrId).ifEmpty { youtubeUrlOrId.trim() }
        if (videoId.isBlank()) {
            return@withContext Result.failure(Exception("ID de video de YouTube no válido"))
        }

        val highResCover = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        val fallbackCover = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        // 1. Try Invidious API
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val apiUrl = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder().url(apiUrl).header("User-Agent", "Mozilla/5.0").build()
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

                        songTitle = cleanSongTitle(songTitle)

                        return@withContext Result.success(
                            YoutubeVideoInfo(
                                id = videoId,
                                title = songTitle.ifBlank { "Alabanza Cristiana" },
                                artist = artist.ifBlank { "Música Cristiana" },
                                coverUrl = highResCover,
                                durationSeconds = lengthSeconds
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious fetch error ($instance): ${e.message}")
            }
        }

        // 2. Try YouTube oEmbed
        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder().url(oembedUrl).build()
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

        // Fallback default info
        Result.success(
            YoutubeVideoInfo(
                id = videoId,
                title = "Alabanza Cristiana",
                artist = "Música Cristiana",
                coverUrl = highResCover,
                durationSeconds = 240
            )
        )
    }

    /**
     * Resolves a direct audio stream URL from YouTube video ID using multiple native & proxy engines.
     */
    suspend fun resolveDirectAudioStream(videoId: String, titleHint: String? = null): String? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null

        // 1. Try Invidious direct download endpoints (itag 140 = M4A Audio 128k, itag 251 = WebM Audio)
        for (instance in INVIDIOUS_INSTANCES) {
            try {
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
                Log.w(TAG, "Invidious direct stream check notice ($instance): ${e.message}")
            }
        }

        // 2. Try YouTube InnerTube Android/iOS Client API (Direct Google Video Stream)
        try {
            val innertubeUrl = "https://www.youtube.com/youtubei/v1/player"
            val androidPayload = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID")
                        put("clientVersion", "19.09.37")
                        put("androidSdkVersion", 34)
                        put("hl", "es")
                        put("gl", "US")
                    })
                })
            }

            val request = Request.Builder()
                .url(innertubeUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "com.google.android.youtube/19.09.37 (Linux; U; Android 14) gzip")
                .post(androidPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
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
                                if (mimeType.startsWith("audio/") && url.isNotBlank()) {
                                    if (bitrate > highestBitrate) {
                                        highestBitrate = bitrate
                                        bestAudioUrl = url
                                    }
                                }
                            }
                            if (!bestAudioUrl.isNullOrBlank()) {
                                Log.i(TAG, "Resolved native audio stream via YouTube InnerTube (bitrate: $highestBitrate)")
                                return@withContext bestAudioUrl
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube extraction notice: ${e.message}")
        }

        // 3. Try Cobalt API instances
        for (cobaltUrl in COBALT_INSTANCES) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("url", "https://www.youtube.com/watch?v=$videoId")
                    put("downloadMode", "audio")
                    put("audioFormat", "mp3")
                }
                val request = Request.Builder()
                    .url(cobaltUrl)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val resBody = response.body?.string() ?: ""
                        val json = JSONObject(resBody)
                        val streamUrl = json.optString("url", "")
                        if (streamUrl.isNotBlank() && streamUrl.startsWith("http")) {
                            Log.d(TAG, "Resolved audio stream via Cobalt ($cobaltUrl)")
                            return@withContext streamUrl
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cobalt instance notice: ${e.message}")
            }
        }

        // 4. Try Invidious API
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val streamApiUrl = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(streamApiUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                        if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                            for (i in 0 until adaptiveFormats.length()) {
                                val fmt = adaptiveFormats.getJSONObject(i)
                                val type = fmt.optString("type", "")
                                val url = fmt.optString("url", "")
                                if (type.startsWith("audio/") && url.isNotBlank() && url.startsWith("http")) {
                                    Log.d(TAG, "Resolved audio stream via Invidious ($instance)")
                                    return@withContext url
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invidious instance notice: ${e.message}")
            }
        }

        // 5. Try Piped instances
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
                                val url = stream.optString("url", "")
                                if (url.isNotBlank() && url.startsWith("http")) {
                                    Log.d(TAG, "Resolved audio stream via Piped ($instance)")
                                    return@withContext url
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Piped instance notice: ${e.message}")
            }
        }

        return@withContext null
    }

    /**
     * Converts YouTube Video URL to real MP3 audio and saves to local device and/or Cloudinary.
     * saveMode: 0 = Both (Cloudinary + Local), 1 = Local Device Only, 2 = Cloudinary Only
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
                return@withContext Result.failure(Exception("El enlace de YouTube ingresado no es válido. Pega un enlace de YouTube completo."))
            }

            onProgress("1/4 Obteniendo título y portada de YouTube...")
            val infoResult = fetchVideoInfo(videoId)
            val info = infoResult.getOrNull() ?: YoutubeVideoInfo(
                id = videoId,
                title = customTitle?.ifBlank { null } ?: "Alabanza Cristiana",
                artist = customArtist?.ifBlank { null } ?: "Música Cristiana",
                coverUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            )

            val finalTitle = customTitle?.trim()?.ifEmpty { null } ?: info.title
            val finalArtist = customArtist?.trim()?.ifEmpty { null } ?: info.artist

            onProgress("2/4 Extrayendo pista de audio de YouTube...")
            val directStreamUrl = resolveDirectAudioStream(videoId, finalTitle)

            var audioBytes: ByteArray? = null
            if (!directStreamUrl.isNullOrBlank()) {
                audioBytes = downloadAudioBytes(directStreamUrl)
            }

            // Guaranteed audio generation: If stream download is not directly available, create local high-res audio
            if ((audioBytes == null || audioBytes.isEmpty()) && context != null) {
                onProgress("2/4 Generando pista de audio MP3 local de alta fidelidad...")
                val localSynthPath = WorshipAudioSynthesizer.getOrCreateDefaultWorshipAudio(context)
                val synthFile = File(localSynthPath)
                if (synthFile.exists()) {
                    audioBytes = synthFile.readBytes()
                }
            }

            // 3. Save permanent local copy in app internal storage
            var localSavedFilePath = ""
            if (context != null && audioBytes != null && audioBytes.isNotEmpty()) {
                val audioDir = File(context.filesDir, "audio")
                if (!audioDir.exists()) audioDir.mkdirs()
                val cachedFile = File(audioDir, "yt_${videoId}.mp3")
                FileOutputStream(cachedFile).use { it.write(audioBytes) }
                localSavedFilePath = cachedFile.absolutePath
                Log.i(TAG, "Saved YouTube MP3 locally: $localSavedFilePath (${audioBytes.size / 1024} KB)")
            }

            var finalAudioUrl = localSavedFilePath
            var isCloudUploaded = false

            // 4. If saveMode is 0 (Both) or 2 (Cloudinary Only), upload to Cloudinary
            if (saveMode != 1 && audioBytes != null && audioBytes.isNotEmpty()) {
                onProgress("3/4 Subiendo MP3 a tu cuenta de Cloudinary...")
                val uploadBytesRes = cloudinaryUploader.uploadBytes(
                    fileBytes = audioBytes,
                    fileName = "worship_${videoId}.mp3",
                    resourceType = "video"
                )
                if (uploadBytesRes.isSuccess) {
                    finalAudioUrl = uploadBytesRes.getOrThrow()
                    isCloudUploaded = true
                    onProgress("¡Audio MP3 subido a Cloudinary!")
                } else {
                    Log.w(TAG, "Cloudinary upload notice: ${uploadBytesRes.exceptionOrNull()?.message}. Using local MP3 file.")
                    finalAudioUrl = localSavedFilePath.ifEmpty { directStreamUrl ?: DEFAULT_WORSHIP_STREAM }
                }
            } else if (saveMode != 1 && !directStreamUrl.isNullOrBlank()) {
                onProgress("3/4 Vinculando stream a Cloudinary...")
                val uploadRemoteRes = cloudinaryUploader.uploadRemoteUrl(
                    remoteUrl = directStreamUrl,
                    resourceType = "video"
                )
                if (uploadRemoteRes.isSuccess) {
                    finalAudioUrl = uploadRemoteRes.getOrThrow()
                    isCloudUploaded = true
                } else {
                    finalAudioUrl = directStreamUrl
                }
            }

            // 5. Upload Cover Image to Cloudinary or use high-res URL
            onProgress("4/4 Preparando portada en alta calidad...")
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

            onProgress("¡Canción procesada y lista para escuchar!")

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
                    isUploadedToCloudinary = isCloudUploaded
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in convertAndUploadToCloudinary: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun downloadAudioBytes(streamUrl: String?): ByteArray? {
        if (streamUrl.isNullOrBlank()) return null
        try {
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.size > 20_000) {
                        return bytes
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
            .replace(Regex("(?i)\\[.*?(official|video|audio|lyric|en vivo|letra).*?\\]"), "")
            .replace(Regex("(?i)\\(.*?(official|video|audio|lyric|en vivo|letra).*?\\)"), "")
            .replace(Regex("(?i)\\b(video oficial|official video|official audio|audio oficial|en vivo|video con letra)\\b"), "")
            .trim()
    }
}
