package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AudioFileManager {
    private const val TAG = "AudioFileManager"

    /**
     * Downloads an MP3 song directly to the user's phone "Downloads" folder.
     */
    suspend fun downloadSongToDeviceDownloads(
        context: Context,
        song: SongEntity,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = song.title.replace(Regex("[^a-zA-Z0-9._ -]"), "_").trim().ifEmpty { "cancion" }
            val cleanArtist = song.artist.replace(Regex("[^a-zA-Z0-9._ -]"), "_").trim().ifEmpty { "musica" }
            val fileName = "$cleanTitle - $cleanArtist.mp3"

            val audioSource = when {
                song.isDownloaded && song.localFilePath.isNotBlank() && File(song.localFilePath).exists() -> song.localFilePath
                song.audioUrl.startsWith("/") && File(song.audioUrl).exists() -> song.audioUrl
                song.audioUrl.startsWith("file://") -> song.audioUrl.substring(7)
                else -> song.audioUrl
            }

            // Case 1: Audio is already a local file on the device -> Copy to Downloads directory
            if (File(audioSource).exists()) {
                val sourceFile = File(audioSource)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            FileInputStream(sourceFile).use { input ->
                                input.copyTo(out)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "✅ MP3 descargado en tu carpeta de Descargas: $fileName", Toast.LENGTH_LONG).show()
                            onComplete(true, "Guardado en Descargas: $fileName")
                        }
                        return@withContext
                    }
                }

                // Fallback for direct Downloads directory
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "✅ MP3 descargado en Descargas: ${destFile.name}", Toast.LENGTH_LONG).show()
                    onComplete(true, "Guardado en Descargas: ${destFile.absolutePath}")
                }
                return@withContext
            }

            // Case 2: Audio is an online URL -> Use DownloadManager or download directly
            if (audioSource.startsWith("http://") || audioSource.startsWith("https://")) {
                val request = DownloadManager.Request(Uri.parse(audioSource))
                    .setTitle("${song.title} - ${song.artist}")
                    .setDescription("Descargando alabanza en MP3...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)

                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (downloadManager != null) {
                    downloadManager.enqueue(request)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "📥 Descarga iniciada: $fileName", Toast.LENGTH_SHORT).show()
                        onComplete(true, "Descarga iniciada en segundo plano")
                    }
                    return@withContext
                }

                // Direct stream download fallback
                val url = URL(audioSource)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val destFile = File(downloadsDir, fileName)
                    connection.inputStream.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "✅ MP3 descargado: $fileName", Toast.LENGTH_LONG).show()
                        onComplete(true, "Guardado en Descargas: ${destFile.name}")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "⚠️ Error en la descarga (${connection.responseCode})", Toast.LENGTH_SHORT).show()
                        onComplete(false, "Error HTTP ${connection.responseCode}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading song to device: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al descargar MP3: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(false, e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Saves audio bytes permanently to the internal app storage (`files/audio/`).
     * Returns the absolute path of the local file.
     */
    fun saveAudioToInternalStorage(context: Context, audioBytes: ByteArray, fileNameHint: String): String {
        val audioDir = File(context.filesDir, "audio")
        if (!audioDir.exists()) audioDir.mkdirs()
        val cleanName = fileNameHint.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val localFile = File(audioDir, "audio_${System.currentTimeMillis()}_$cleanName")
        FileOutputStream(localFile).use { it.write(audioBytes) }
        Log.i(TAG, "Saved audio locally: ${localFile.absolutePath} (${audioBytes.size / 1024} KB)")
        return localFile.absolutePath
    }

    /**
     * Copies an audio file from a Uri (e.g. file picked from device storage) to internal storage.
     */
    fun copyUriToInternalAudio(context: Context, uri: Uri, originalName: String): String? {
        return try {
            val audioDir = File(context.filesDir, "audio")
            if (!audioDir.exists()) audioDir.mkdirs()
            val cleanName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(audioDir, "local_${System.currentTimeMillis()}_$cleanName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to internal audio: ${e.message}", e)
            null
        }
    }

    /**
     * Shares the MP3 file to other apps (WhatsApp, Telegram, Bluetooth, etc.).
     */
    fun shareSongMp3(context: Context, song: SongEntity) {
        try {
            val audioPath = when {
                song.isDownloaded && song.localFilePath.isNotBlank() -> song.localFilePath
                song.audioUrl.startsWith("/") -> song.audioUrl
                else -> null
            }

            if (audioPath != null && File(audioPath).exists()) {
                val file = File(audioPath)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/mpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
                    putExtra(Intent.EXTRA_TEXT, "Escucha '${song.title}' de ${song.artist}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir MP3 con..."))
            } else {
                // Share the online link
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
                    putExtra(Intent.EXTRA_TEXT, "Escucha '${song.title}' de ${song.artist}: ${song.audioUrl}")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir canción..."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing song: ${e.message}", e)
            Toast.makeText(context, "No se pudo compartir el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
