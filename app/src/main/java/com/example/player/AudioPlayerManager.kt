package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.CountDownTimer
import com.example.data.local.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Player State Flows
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

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

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val urlToPlay = if (song.isDownloaded && song.localFilePath.isNotEmpty()) {
                song.localFilePath
            } else {
                song.audioUrl
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(urlToPlay)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _duration.value = mp.duration.toLong().coerceAtLeast(1L)
                }
                setOnCompletionListener {
                    handleSongCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback simulation playback for preview
            _isPlaying.value = true
            _duration.value = (song.durationSeconds * 1000).toLong()
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
            if (mp.isPlaying || mp.currentPosition > 0) {
                mp.seekTo(positionMs.toInt())
                _currentPosition.value = positionMs
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
                    if (mp.isPlaying) {
                        _currentPosition.value = mp.currentPosition.toLong()
                        _duration.value = mp.duration.toLong().coerceAtLeast(1L)
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
