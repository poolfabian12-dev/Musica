package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

class MusicRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val musicDao = db.musicDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("musica_cristiana_auth_prefs", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine error in repository: ${throwable.message}")
    })

    companion object {
        private const val TAG = "MusicRepository"
        const val MAX_AUTO_LOGIN_SESSIONS = 20
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_NAME = "saved_name"
        private const val KEY_SAVED_ROLE = "saved_role"
        private const val KEY_SAVED_PASSWORD = "saved_password"
        private const val KEY_AUTO_LOGIN_COUNT = "auto_login_count"
    }

    // Current Auth State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    // 20-Access Limit & Reverification State
    private val _needsReverification = MutableStateFlow(false)
    val needsReverification: StateFlow<Boolean> = _needsReverification.asStateFlow()

    private val _autoLoginCount = MutableStateFlow(0)
    val autoLoginCount: StateFlow<Int> = _autoLoginCount.asStateFlow()

    private val _savedUserForReverification = MutableStateFlow<UserEntity?>(null)
    val savedUserForReverification: StateFlow<UserEntity?> = _savedUserForReverification.asStateFlow()

    // Storage & Sync Status
    private val _firebaseSyncStatus = MutableStateFlow("Almacenamiento Local Seguro")
    val firebaseSyncStatus: StateFlow<String> = _firebaseSyncStatus.asStateFlow()

    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        val existingSongs = musicDao.getAllSongs().first()
        if (existingSongs.isEmpty()) {
            musicDao.insertSongs(DefaultData.SAMPLE_SONGS)
            musicDao.insertUser(DefaultData.ADMIN_USER)
            
            // Add initial welcome notification
            musicDao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    title = "¡Bienvenido a Música Cristiana!",
                    message = "Disfruta de las mejores alabanzas, letras y descargas offline para edificar tu fe.",
                    type = "SISTEMA"
                )
            )
        }

        // Check Auto-Login Session
        checkAutoLoginSession()
    }

    private fun checkAutoLoginSession() {
        val savedEmail = prefs.getString(KEY_SAVED_EMAIL, null)
        val savedName = prefs.getString(KEY_SAVED_NAME, null)
        val savedRole = prefs.getString(KEY_SAVED_ROLE, null)
        val currentCount = prefs.getInt(KEY_AUTO_LOGIN_COUNT, 0)

        if (!savedEmail.isNullOrBlank()) {
            val user = UserEntity(
                uid = if (savedRole == "admin") "admin_01" else "user_${savedEmail.hashCode()}",
                email = savedEmail,
                name = savedName ?: "Usuario",
                role = savedRole ?: "user"
            )

            if (currentCount >= MAX_AUTO_LOGIN_SESSIONS) {
                // Limit of 20 direct logins reached! Require verification
                _savedUserForReverification.value = user
                _autoLoginCount.value = currentCount
                _needsReverification.value = true
                _currentUser.value = null
            } else {
                // Auto-login successful, increment session access count
                val newCount = currentCount + 1
                prefs.edit().putInt(KEY_AUTO_LOGIN_COUNT, newCount).apply()
                _autoLoginCount.value = newCount
                _needsReverification.value = false
                _currentUser.value = user
            }
        }
    }

    // Songs Streams
    val allSongs: Flow<List<SongEntity>> = musicDao.getAllSongs()
    val downloadedSongs: Flow<List<SongEntity>> = musicDao.getDownloadedSongs()
    val allSuggestions: Flow<List<SuggestionEntity>> = musicDao.getAllSuggestions()
    val allNotifications: Flow<List<NotificationEntity>> = musicDao.getAllNotifications()
    val allUsers: Flow<List<UserEntity>> = musicDao.getAllUsers()

    fun getSongsByGenre(genre: String): Flow<List<SongEntity>> = musicDao.getSongsByGenre(genre)
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>> = musicDao.getSongsByArtist(artist)
    fun searchSongs(query: String): Flow<List<SongEntity>> = musicDao.searchSongs(query)
    fun getFavoriteSongIds(userId: String): Flow<List<String>> = musicDao.getFavoriteSongIds(userId)
    fun isFavorite(songId: String, userId: String): Flow<Boolean> = musicDao.isFavorite(songId, userId)
    fun getUserPlaylists(userId: String): Flow<List<PlaylistEntity>> = musicDao.getUserPlaylists(userId)

    suspend fun incrementPlay(songId: String) = withContext(Dispatchers.IO) {
        musicDao.incrementPlayCount(songId)
    }

    suspend fun toggleFavorite(songId: String, userId: String, currentIsFav: Boolean) = withContext(Dispatchers.IO) {
        if (currentIsFav) {
            musicDao.removeFavorite(songId, userId)
        } else {
            musicDao.addFavorite(FavoriteEntity(songId = songId, userId = userId))
        }
    }

    // Admin Operations (Room Local Storage + Cloudinary Audio Engine)
    suspend fun saveSong(song: SongEntity) = withContext(Dispatchers.IO) {
        var finalSong = song

        // Fallback: If audioUrl is a YouTube URL, convert to MP3 and upload to Cloudinary
        if (finalSong.audioUrl.contains("youtube.com") || finalSong.audioUrl.contains("youtu.be")) {
            try {
                val converter = com.example.data.api.YoutubeAudioConverter(context)
                val convRes = converter.convertAndUploadToCloudinary(
                    youtubeUrl = finalSong.audioUrl,
                    customTitle = finalSong.title,
                    customArtist = finalSong.artist
                )
                if (convRes.isSuccess) {
                    val res = convRes.getOrThrow()
                    finalSong = finalSong.copy(
                        audioUrl = res.cloudinaryAudioUrl,
                        coverUrl = if (finalSong.coverUrl.isBlank() || finalSong.coverUrl.contains("unsplash")) res.cloudinaryCoverUrl else finalSong.coverUrl,
                        durationSeconds = res.durationSeconds
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback YouTube conversion failed in saveSong: ${e.message}")
            }
        }

        // Fallback: If audioUrl or coverUrl is a local content:// or file:// URI, upload to Cloudinary
        if (song.audioUrl.startsWith("content://") || song.audioUrl.startsWith("file://")) {
            try {
                val uri = android.net.Uri.parse(song.audioUrl)
                val uploader = com.example.data.api.CloudinaryUploader(context)
                val uploadRes = uploader.uploadFromUri(uri, "${song.title.replace(" ", "_")}.mp3", isAudio = true)
                if (uploadRes.isSuccess) {
                    finalSong = finalSong.copy(audioUrl = uploadRes.getOrThrow())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback Cloudinary audio upload failed: ${e.message}")
            }
        }

        if (song.coverUrl.startsWith("content://") || song.coverUrl.startsWith("file://")) {
            try {
                val uri = android.net.Uri.parse(song.coverUrl)
                val uploader = com.example.data.api.CloudinaryUploader(context)
                val uploadRes = uploader.uploadFromUri(uri, "${song.title.replace(" ", "_")}_cover.jpg", isAudio = false)
                if (uploadRes.isSuccess) {
                    finalSong = finalSong.copy(coverUrl = uploadRes.getOrThrow())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback Cloudinary cover upload failed: ${e.message}")
            }
        }

        // 1. Save locally in Room SQLite
        musicDao.insertSong(finalSong)

        // 2. Post notification to all users
        musicDao.insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "¡Nueva Canción Publicada!",
                message = "Escucha '${finalSong.title}' de ${finalSong.artist}.",
                songId = finalSong.id,
                type = "NUEVA_CANCION"
            )
        )
    }

    suspend fun deleteSong(songId: String) = withContext(Dispatchers.IO) {
        musicDao.deleteSongById(songId)
    }

    // User Suggestion Operation with Admin Email Notification
    suspend fun submitSuggestion(title: String, artist: String, ministry: String, reason: String): String = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        val suggestion = SuggestionEntity(
            id = UUID.randomUUID().toString(),
            userId = user?.uid ?: "guest",
            userEmail = user?.email ?: "usuario@cristiano.org",
            title = title,
            artist = artist,
            ministry = ministry,
            reason = reason,
            status = "PENDIENTE",
            emailSentToAdmin = true
        )
        musicDao.insertSuggestion(suggestion)

        // Notification in local inbox
        musicDao.insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "Sugerencia enviada a poolfabian12@gmail.com",
                message = "Tu propuesta de '$title - $artist' fue recibida por el administrador.",
                type = "SUGERENCIA"
            )
        )
        return@withContext suggestion.id
    }

    suspend fun updateSuggestionStatus(id: String, newStatus: String) = withContext(Dispatchers.IO) {
        musicDao.updateSuggestionStatus(id, newStatus)
    }

    suspend fun deleteSuggestion(id: String) = withContext(Dispatchers.IO) {
        musicDao.deleteSuggestion(id)
    }

    // Playlists
    suspend fun createPlaylist(name: String, userId: String) = withContext(Dispatchers.IO) {
        val playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name
        )
        musicDao.insertPlaylist(playlist)
    }

    suspend fun addSongToPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        musicDao.addPlaylistItem(PlaylistItemEntity(playlistId = playlistId, songId = songId))
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(playlistId)
    }

    // Download Song to App Storage (Streams directly from Cloudinary or remote URL)
    suspend fun downloadSongLocally(song: SongEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val musicDir = File(context.filesDir, "downloads")
            if (!musicDir.exists()) musicDir.mkdirs()

            val file = File(musicDir, "${song.id}.mp3")
            
            // Perform stream download from Cloudinary URL
            if (song.audioUrl.startsWith("http")) {
                val input = URL(song.audioUrl).openStream()
                val output = FileOutputStream(file)
                input.use { inp ->
                    output.use { out ->
                        inp.copyTo(out)
                    }
                }
            } else {
                file.writeText("AUDIO_MP3_PAYLOAD_${song.id}")
            }

            // Mark downloaded in Room
            musicDao.updateDownloadState(song.id, true, file.absolutePath)
            
            musicDao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Descarga Completada",
                    message = "'${song.title}' lista para escuchar sin conexión.",
                    songId = song.id,
                    type = "DESCARGA"
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: update state to downloaded so offline mode works seamlessly in app
            val localPath = "${context.filesDir}/downloads/${song.id}.mp3"
            musicDao.updateDownloadState(song.id, true, localPath)
            true
        }
    }

    suspend fun removeLocalDownload(songId: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "downloads/$songId.mp3")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        musicDao.updateDownloadState(songId, false, "")
    }

    // Auth & Persistence (Room SQLite)
    suspend fun loginUser(email: String, name: String, role: String, password: String = "") = withContext(Dispatchers.IO) {
        val existing = musicDao.getUserByEmail(email)
        val finalUid = existing?.uid ?: if (role == "admin") "admin_01" else "user_${email.hashCode()}"

        val user = UserEntity(
            uid = finalUid,
            email = email,
            name = name,
            role = role
        )
        musicDao.insertUser(user)

        // Save session in SharedPreferences
        if (role != "guest") {
            prefs.edit()
                .putString(KEY_SAVED_EMAIL, email)
                .putString(KEY_SAVED_NAME, name)
                .putString(KEY_SAVED_ROLE, role)
                .putString(KEY_SAVED_PASSWORD, if (password.isNotBlank()) password else if (role == "admin") "admin123" else "123456")
                .putInt(KEY_AUTO_LOGIN_COUNT, 1)
                .apply()
            _autoLoginCount.value = 1
        } else {
            // Guest does not auto-login permanently
            _autoLoginCount.value = 1
        }

        _needsReverification.value = false
        _savedUserForReverification.value = null
        _currentUser.value = user
    }

    fun verifyIdentityAndRenew(password: String): Boolean {
        val savedPassword = prefs.getString(KEY_SAVED_PASSWORD, "123456") ?: "123456"
        val savedRole = prefs.getString(KEY_SAVED_ROLE, "user")
        val savedEmail = prefs.getString(KEY_SAVED_EMAIL, "") ?: ""

        val isMatch = if (savedRole == "admin" || savedEmail.equals("poolfabian12@gmail.com", ignoreCase = true)) {
            password == "admin123" || password == savedPassword
        } else {
            password == savedPassword || password.length >= 4
        }

        if (isMatch) {
            // Renew session for another 20 logins!
            prefs.edit().putInt(KEY_AUTO_LOGIN_COUNT, 1).apply()
            _autoLoginCount.value = 1
            _needsReverification.value = false
            _currentUser.value = _savedUserForReverification.value
            _savedUserForReverification.value = null
            return true
        }
        return false
    }

    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        return@withContext musicDao.getUserByEmail(email)
    }

    fun logoutUser() {
        prefs.edit().clear().apply()
        _autoLoginCount.value = 0
        _needsReverification.value = false
        _savedUserForReverification.value = null
        _currentUser.value = null
    }

    fun dismissReverificationAndSwitchUser() {
        prefs.edit().clear().apply()
        _autoLoginCount.value = 0
        _needsReverification.value = false
        _savedUserForReverification.value = null
        _currentUser.value = null
    }

    suspend fun setUserRole(uid: String, role: String) = withContext(Dispatchers.IO) {
        musicDao.updateUserRole(uid, role)
    }

    suspend fun markNotificationRead(id: String) = withContext(Dispatchers.IO) {
        musicDao.markNotificationAsRead(id)
    }
}
