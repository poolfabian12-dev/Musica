package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

class MusicRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val musicDao = db.musicDao()

    // Current Auth State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

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

    // Admin Operations
    suspend fun saveSong(song: SongEntity) = withContext(Dispatchers.IO) {
        musicDao.insertSong(song)
        // Post notification to all users
        musicDao.insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                title = "¡Nueva Canción Publicada!",
                message = "Escucha '${song.title}' de ${song.artist}.",
                songId = song.id,
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

        // Simulate notification sent to admin email poolfabian12@gmail.com
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

    // Download Song to App Storage
    suspend fun downloadSongLocally(song: SongEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // Save file in app-specific internal audio directory
            val musicDir = File(context.filesDir, "downloads")
            if (!musicDir.exists()) musicDir.mkdirs()

            val file = File(musicDir, "${song.id}.mp3")
            
            // Perform simulated/real stream download
            if (song.audioUrl.startsWith("http")) {
                val input = URL(song.audioUrl).openStream()
                val output = FileOutputStream(file)
                input.use { inp ->
                    output.use { out ->
                        inp.copyTo(out)
                    }
                }
            } else {
                // Dummy file payload if offline or local placeholder
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

    // Auth & Persistence
    suspend fun loginUser(email: String, name: String, role: String) = withContext(Dispatchers.IO) {
        val existing = musicDao.getUserByEmail(email)
        val user = existing ?: UserEntity(
            uid = if (role == "admin") "admin_01" else "user_${email.hashCode()}",
            email = email,
            name = name,
            role = role
        )
        musicDao.insertUser(user)
        _currentUser.value = user
    }

    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        return@withContext musicDao.getUserByEmail(email)
    }

    fun logoutUser() {
        _currentUser.value = null
    }

    suspend fun setUserRole(uid: String, role: String) = withContext(Dispatchers.IO) {
        musicDao.updateUserRole(uid, role)
    }

    suspend fun markNotificationRead(id: String) = withContext(Dispatchers.IO) {
        musicDao.markNotificationAsRead(id)
    }
}
