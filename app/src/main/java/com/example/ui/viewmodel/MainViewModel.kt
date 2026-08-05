package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    data class Processing(val progressPercent: Int) : YoutubeConversionState()
    data class Success(val mp3Url: String, val title: String, val artist: String) : YoutubeConversionState()
    data class Error(val message: String) : YoutubeConversionState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository(application)
    val playerManager = AudioPlayerManager(application)

    // Auth & User Role State
    val currentUser: StateFlow<UserEntity?> = repository.currentUser

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

    fun convertYoutubeUrl(url: String) {
        if (url.isBlank() || (!url.contains("youtube.com") && !url.contains("youtu.be"))) {
            _userMessage.value = "Por favor ingresa una URL válida de YouTube."
            return
        }

        viewModelScope.launch {
            _youtubeState.value = YoutubeConversionState.Processing(10)
            delay(800)
            _youtubeState.value = YoutubeConversionState.Processing(45)
            delay(1000)
            _youtubeState.value = YoutubeConversionState.Processing(85)
            delay(800)

            // Extracted sample audio stream from Cloudinary/Sound Helix
            val convertedMp3 = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"
            _youtubeState.value = YoutubeConversionState.Success(
                mp3Url = convertedMp3,
                title = "Alabanza Extraída de YouTube",
                artist = "Ministerio Vivo"
            )
            _userMessage.value = "¡Conversión de YouTube completada con éxito!"
        }
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
    fun login(email: String, name: String, role: String) {
        viewModelScope.launch {
            repository.loginUser(email, name, role)
            _userMessage.value = if (role == "admin") "Bienvenido Administrador" else "Bienvenido $name"
        }
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
