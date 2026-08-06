package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.YoutubeAudioConverter
import com.example.data.local.NotificationEntity
import com.example.data.local.SongEntity
import com.example.data.local.SuggestionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.MusicRepository
import com.example.player.AudioPlayerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class YoutubeConversionState {
    object Idle : YoutubeConversionState()
    data class Processing(val progressPercent: Int, val statusText: String = "Procesando...") : YoutubeConversionState()
    data class Success(
        val mp3Url: String,
        val coverUrl: String,
        val title: String,
        val artist: String,
        val durationSeconds: Int = 210,
        val localFilePath: String = "",
        val isStoredLocally: Boolean = true,
        val isUploadedToCloudinary: Boolean = false
    ) : YoutubeConversionState()
    data class Error(val message: String) : YoutubeConversionState()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository(application)
    val playerManager = AudioPlayerManager(application)
    val youtubeConverter = YoutubeAudioConverter(application)

    // Auth & User Role State
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
    val needsReverification: StateFlow<Boolean> = repository.needsReverification
    val autoLoginCount: StateFlow<Int> = repository.autoLoginCount
    val savedUserForReverification: StateFlow<UserEntity?> = repository.savedUserForReverification
    val firebaseSyncStatus: StateFlow<String> = repository.firebaseSyncStatus

    // Database Flows
    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadedSongs: StateFlow<List<SongEntity>> = repository.downloadedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val suggestions: StateFlow<List<SuggestionEntity>> = repository.allSuggestions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Favorite Song IDs
    val favoriteSongIds: StateFlow<List<String>> = currentUser.flatMapLatest { user ->
        repository.getFavoriteSongIds(user?.uid ?: "guest")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // User Playlists
    val userPlaylists = currentUser.flatMapLatest { user ->
        repository.getUserPlaylists(user?.uid ?: "guest")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredSongs: StateFlow<List<SongEntity>> = combine(allSongs, searchQuery) { songs, query ->
        if (query.isBlank()) songs
        else songs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
            song.artist.contains(query, ignoreCase = true) ||
            song.ministry.contains(query, ignoreCase = true) ||
            song.genre.contains(query, ignoreCase = true) ||
            song.lyrics.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected Category/Genre Filter
    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    // Selected Artist Filter
    private val _selectedArtist = MutableStateFlow<String?>(null)
    val selectedArtist: StateFlow<String?> = _selectedArtist.asStateFlow()

    // YouTube Conversion State
    private val _youtubeState = MutableStateFlow<YoutubeConversionState>(YoutubeConversionState.Idle)
    val youtubeState: StateFlow<YoutubeConversionState> = _youtubeState.asStateFlow()

    // Status Message Toast/Snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultData()
        }
        viewModelScope.launch {
            playerManager.playbackErrorMessage.collect { err ->
                if (!err.isNullOrBlank()) {
                    _userMessage.value = err
                    playerManager.clearPlaybackError()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGenre(genre: String?) {
        _selectedGenre.value = genre
    }

    fun setSelectedArtist(artist: String?) {
        _selectedArtist.value = artist
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // Player Trigger
    fun playSong(song: SongEntity) {
        val list = filteredSongs.value.ifEmpty { allSongs.value }
        val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playerManager.playSongList(list, index)
        viewModelScope.launch {
            repository.incrementPlay(song.id)
        }
    }

    fun toggleFavorite(song: SongEntity) {
        val user = currentUser.value
        if (user?.role == "guest") {
            _userMessage.value = "Modo Invitado: Inicia sesión o regístrate para guardar canciones en tus favoritos."
            return
        }
        viewModelScope.launch {
            val isFav = favoriteSongIds.value.contains(song.id)
            repository.toggleFavorite(song.id, user?.uid ?: "guest", isFav)
            _userMessage.value = if (!isFav) "Añadida a favoritos" else "Eliminada de favoritos"
        }
    }

    fun downloadSong(song: SongEntity) {
        val user = currentUser.value
        if (user?.role == "guest") {
            _userMessage.value = "Modo Invitado: Inicia sesión o regístrate para descargar canciones."
            return
        }
        viewModelScope.launch {
            _userMessage.value = "Descargando '${song.title}'..."
            val success = repository.downloadSongLocally(song)
            if (success) {
                _userMessage.value = "¡'${song.title}' descargada exitosamente!"
            } else {
                _userMessage.value = "Error al descargar la canción"
            }
        }
    }

    fun removeDownload(songId: String) {
        viewModelScope.launch {
            repository.removeLocalDownload(songId)
            _userMessage.value = "Descarga eliminada"
        }
    }

    // User Suggest Song Form
    fun submitSuggestion(title: String, artist: String, ministry: String, reason: String) {
        if (title.isBlank() || artist.isBlank()) {
            _userMessage.value = "Por favor ingresa título y artista."
            return
        }
        viewModelScope.launch {
            repository.submitSuggestion(title, artist, ministry, reason)
            _userMessage.value = "¡Sugerencia enviada al administrador (poolfabian12@gmail.com)!"
        }
    }

    // Admin Operations
    fun saveSong(song: SongEntity) {
        viewModelScope.launch {
            repository.saveSong(song)
            _userMessage.value = "Canción '${song.title}' guardada correctamente."
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            repository.deleteSong(songId)
            _userMessage.value = "Canción eliminada del catálogo."
        }
    }

    fun updateSuggestionStatus(suggestionId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateSuggestionStatus(suggestionId, newStatus)
            _userMessage.value = "Sugerencia marcada como: $newStatus"
        }
    }

    fun convertYoutubeUrl(url: String, saveMode: Int = 0) {
        if (url.isBlank() || (!url.contains("youtube.com") && !url.contains("youtu.be"))) {
            _userMessage.value = "Por favor ingresa una URL válida de YouTube."
            return
        }

        viewModelScope.launch {
            _youtubeState.value = YoutubeConversionState.Processing(15, "Obteniendo datos del video...")
            val result = youtubeConverter.convertAndUploadToCloudinary(
                youtubeUrl = url,
                saveMode = saveMode,
                onProgress = { progressText ->
                    val percent = when {
                        progressText.startsWith("1") -> 25
                        progressText.startsWith("2") -> 50
                        progressText.startsWith("3") -> 75
                        progressText.startsWith("4") -> 90
                        else -> 80
                    }
                    _youtubeState.value = YoutubeConversionState.Processing(percent, progressText)
                }
            )

            if (result.isSuccess) {
                val res = result.getOrThrow()
                _youtubeState.value = YoutubeConversionState.Success(
                    mp3Url = res.cloudinaryAudioUrl,
                    coverUrl = res.cloudinaryCoverUrl,
                    title = res.title,
                    artist = res.artist,
                    durationSeconds = res.durationSeconds,
                    localFilePath = res.localFilePath,
                    isStoredLocally = res.isStoredLocally,
                    isUploadedToCloudinary = res.isUploadedToCloudinary
                )
                val msg = when {
                    res.isUploadedToCloudinary && res.isStoredLocally -> "¡Canción convertida a MP3, guardada en tu celular y subida a Cloudinary!"
                    res.isStoredLocally -> "¡Canción convertida a MP3 y guardada en el almacenamiento de tu celular!"
                    else -> "¡Canción convertida con éxito!"
                }
                _userMessage.value = msg
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error desconocido en la conversión"
                _youtubeState.value = YoutubeConversionState.Error(err)
                _userMessage.value = "Error al convertir: $err"
            }
        }
    }

    fun downloadSongMp3(song: SongEntity) {
        viewModelScope.launch {
            com.example.util.AudioFileManager.downloadSongToDeviceDownloads(getApplication(), song) { success, msg ->
                _userMessage.value = msg
            }
        }
    }

    fun shareSongMp3(song: SongEntity) {
        com.example.util.AudioFileManager.shareSongMp3(getApplication(), song)
    }

    fun resetYoutubeState() {
        _youtubeState.value = YoutubeConversionState.Idle
    }

    // Playlists
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val user = currentUser.value
        if (user?.role == "guest") {
            _userMessage.value = "Modo Invitado: Inicia sesión o regístrate para crear listas de reproducción."
            return
        }
        viewModelScope.launch {
            repository.createPlaylist(name, user?.uid ?: "guest")
            _userMessage.value = "Lista '$name' creada"
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            _userMessage.value = "Añadida a la lista de reproducción"
        }
    }

    // Authentication & Session
    fun login(email: String, name: String, role: String, password: String = "") {
        viewModelScope.launch {
            repository.loginUser(email, name, role, password)
            _userMessage.value = if (role == "admin") "Bienvenido Administrador" else "Bienvenido $name"
        }
    }

    fun verifyIdentity(password: String): Boolean {
        val success = repository.verifyIdentityAndRenew(password)
        if (success) {
            _userMessage.value = "Identidad confirmada. ¡Bienvenido de vuelta!"
        } else {
            _userMessage.value = "Contraseña incorrecta. Inténtalo de nuevo."
        }
        return success
    }

    fun switchUser() {
        repository.dismissReverificationAndSwitchUser()
        _userMessage.value = "Sesión cerrada"
    }

    fun logout() {
        repository.logoutUser()
        _userMessage.value = "Sesión cerrada"
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
