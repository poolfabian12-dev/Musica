package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import com.example.data.api.YoutubeAudioConverter
import com.example.data.local.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream

enum class RepeatMode {
    OFF, ONE, ALL
}

data class EqualizerPreset(
    val name: String,
    val bassBoost: Int,
    val vocalBoost: Int,
    val trebleBoost: Int
)

class AudioPlayerManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayerManager"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val youtubeConverter = YoutubeAudioConverter(context)

    // Player State Flows
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(1L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<SongEntity>>(emptyList())
    val queue: StateFlow<List<SongEntity>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow(0) // 0 = OFF
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes.asStateFlow()

    private var sleepCountDown: CountDownTimer? = null

    // Equalizer State
    val equalizerPresets = listOf(
        EqualizerPreset("Plano / Normal", 0, 0, 0),
        EqualizerPreset("Alabanza Vivo", 4, 2, 3),
        EqualizerPreset("Adoración Suave", 2, 5, 2),
        EqualizerPreset("Roca Firme", 6, 1, 5),
        EqualizerPreset("Vocal Claro", 1, 6, 2)
    )

    private val _selectedPreset = MutableStateFlow(equalizerPresets[0])
    val selectedPreset: StateFlow<EqualizerPreset> = _selectedPreset.asStateFlow()

    // Playback Error Notification State
    private val _playbackErrorMessage = MutableStateFlow<String?>(null)
    val playbackErrorMessage: StateFlow<String?> = _playbackErrorMessage.asStateFlow()

    fun clearPlaybackError() {
        _playbackErrorMessage.value = null
    }

    init {
        startProgressTracker()
    }

    fun playSongList(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _queue.value = if (_isShuffle.value) songs.shuffled() else songs
        _currentIndex.value = startIndex.coerceIn(0, _queue.value.lastIndex)
        playCurrentQueueItem()
    }

    private fun playCurrentQueueItem() {
        val song = _queue.value.getOrNull(_currentIndex.value) ?: return
        _currentSong.value = song
        _isBuffering.value = true
        _playbackErrorMessage.value = null

        scope.launch {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null

                var rawUrl = when {
                    song.isDownloaded && song.localFilePath.isNotEmpty() -> song.localFilePath
                    song.localFilePath.isNotEmpty() && File(song.localFilePath).exists() -> song.localFilePath
                    else -> song.audioUrl
                }

                if (rawUrl.isBlank()) {
                    _isBuffering.value = false
                    _isPlaying.value = false
                    _playbackErrorMessage.value = "La canción '${song.title}' no tiene un enlace de audio válido."
                    return@launch
                }

                // If URL is a YouTube watch URL or not a direct audio file, resolve real audio stream
                if (rawUrl.contains("youtube.com") || rawUrl.contains("youtu.be")) {
                    val videoId = youtubeConverter.extractVideoId(rawUrl)
                    val stream = withContext(Dispatchers.IO) {
                        youtubeConverter.resolveDirectAudioStream(videoId, song.title)
                    }
                    if (!stream.isNullOrBlank()) {
                        rawUrl = stream
                    } else if (song.localFilePath.isNotEmpty() && File(song.localFilePath).exists()) {
                        rawUrl = song.localFilePath
                    } else {
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _playbackErrorMessage.value = "No se pudo conectar con la pista de audio original de YouTube para '${song.title}'."
                        return@launch
                    }
                }

                initMediaPlayer(rawUrl, song)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing song: ${e.message}", e)
                _isBuffering.value = false
                _isPlaying.value = false
                _playbackErrorMessage.value = "Error al iniciar reproducción de '${song.title}': ${e.localizedMessage ?: "Archivo no reproducible"}"
            }
        }
    }

    private fun initMediaPlayer(urlToPlay: String, song: SongEntity, attempt: Int = 0) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            val mp = MediaPlayer()
            mediaPlayer = mp

            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
            )

            // Setup Data Source according to URL type (Local File vs Content URI vs HTTPS Stream)
            when {
                urlToPlay.startsWith("content://") -> {
                    val uri = Uri.parse(urlToPlay)
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        mp.setDataSource(pfd.fileDescriptor)
                    } ?: mp.setDataSource(context, uri)
                }
                urlToPlay.startsWith("file://") || urlToPlay.startsWith("/") -> {
                    val filePath = if (urlToPlay.startsWith("file://")) urlToPlay.substring(7) else urlToPlay
                    val file = File(filePath)
                    if (file.exists() && file.length() > 0) {
                        FileInputStream(file).use { fis ->
                            mp.setDataSource(fis.fd)
                        }
                    } else {
                        throw IllegalStateException("Archivo de audio local no encontrado: $filePath")
                    }
                }
                else -> {
                    val headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                        "Referer" to "https://www.youtube.com/"
                    )
                    mp.setDataSource(context, Uri.parse(urlToPlay), headers)
                }
            }

            mp.setOnPreparedListener { player ->
                try {
                    player.start()
                    _isPlaying.value = true
                    _isBuffering.value = false
                    val d = player.duration.toLong()
                    _duration.value = if (d > 0) d else (song.durationSeconds * 1000L).coerceAtLeast(60000L)
                    Log.i(TAG, "Audio started playing successfully. Duration: ${_duration.value}ms")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting MediaPlayer onPrepared: ${e.message}")
                    _isBuffering.value = false
                    _isPlaying.value = false
                    _playbackErrorMessage.value = "Error al iniciar reproducción de '${song.title}'."
                }
            }

            mp.setOnCompletionListener {
                handleSongCompletion()
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra for url: $urlToPlay (attempt: $attempt)")
                scope.launch {
                    try {
                        mediaPlayer?.release()
                        mediaPlayer = null

                        // If local file path exists and wasn't tried yet, attempt local fallback
                        if (attempt == 0 && song.localFilePath.isNotEmpty() && File(song.localFilePath).exists() && urlToPlay != song.localFilePath) {
                            Log.i(TAG, "Retrying playback with local file: ${song.localFilePath}")
                            initMediaPlayer(song.localFilePath, song, attempt = 1)
                        } else {
                            _isBuffering.value = false
                            _isPlaying.value = false
                            _playbackErrorMessage.value = "No se pudo reproducir '${song.title}'. El enlace o archivo no está disponible (Error $what)."
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in playback error handler: ${e.message}")
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _playbackErrorMessage.value = "Error al reproducir '${song.title}'."
                    }
                }
                true
            }

            mp.prepareAsync()

        } catch (e: Exception) {
            Log.e(TAG, "initMediaPlayer failed for $urlToPlay (attempt $attempt): ${e.message}")
            scope.launch {
                if (attempt == 0 && song.localFilePath.isNotEmpty() && File(song.localFilePath).exists() && urlToPlay != song.localFilePath) {
                    initMediaPlayer(song.localFilePath, song, attempt = 1)
                } else {
                    _isBuffering.value = false
                    _isPlaying.value = false
                    _playbackErrorMessage.value = "No se pudo reproducir '${song.title}': ${e.localizedMessage ?: "Recurso no accesible"}"
                }
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.start()
                _isPlaying.value = true
            }
        } else {
            _currentSong.value?.let {
                playCurrentQueueItem()
            }
        }
    }

    fun playNext() {
        val list = _queue.value
        if (list.isEmpty()) return
        val nextIdx = (_currentIndex.value + 1) % list.size
        _currentIndex.value = nextIdx
        playCurrentQueueItem()
    }

    fun playPrevious() {
        val list = _queue.value
        if (list.isEmpty()) return
        val prevIdx = if (_currentIndex.value - 1 < 0) list.lastIndex else _currentIndex.value - 1
        _currentIndex.value = prevIdx
        playCurrentQueueItem()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(positionMs.toInt())
                _currentPosition.value = positionMs
            } catch (e: Exception) {
                Log.w(TAG, "Seek error: ${e.message}")
            }
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        sleepCountDown?.cancel()
        if (minutes > 0) {
            val millis = minutes * 60 * 1000L
            sleepCountDown = object : CountDownTimer(millis, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    mediaPlayer?.pause()
                    _isPlaying.value = false
                    _sleepTimerMinutes.value = 0
                }
            }.start()
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _selectedPreset.value = preset
    }

    private fun handleSongCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> playCurrentQueueItem()
            RepeatMode.ALL, RepeatMode.OFF -> playNext()
        }
    }

    private fun startProgressTracker() {
        scope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            _currentPosition.value = mp.currentPosition.toLong()
                            val dur = mp.duration.toLong()
                            if (dur > 0) _duration.value = dur
                        }
                    } catch (e: Exception) {
                        // Ignored if released
                    }
                }
                delay(500)
            }
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        sleepCountDown?.cancel()
        scope.cancel()
    }
}
