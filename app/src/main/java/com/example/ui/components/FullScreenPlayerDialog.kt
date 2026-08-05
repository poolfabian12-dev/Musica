package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.SongEntity
import com.example.player.EqualizerPreset
import com.example.player.RepeatMode

@Composable
fun FullScreenPlayerDialog(
    show: Boolean,
    song: SongEntity?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isFavorite: Boolean,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    sleepTimerMinutes: Int,
    equalizerPresets: List<EqualizerPreset>,
    selectedPreset: EqualizerPreset,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit
) {
    if (!show || song == null) return

    var selectedTab by remember { mutableStateOf(0) } // 0 = Reproductor, 1 = Letra
    var showTimerDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_screen_player"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cerrar",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Tab Selector: Reproductor | Letra
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Audio")
                        }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Letra")
                        }
                    }

                    IconButton(onClick = { showTimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Temporizador",
                            tint = if (sleepTimerMinutes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Audio Player Screen
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Large Artwork Card
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                        ) {
                            AsyncImage(
                                model = song.coverUrl.ifEmpty { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500" },
                                contentDescription = "Portada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Title & Artist
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${song.artist} • ${song.ministry}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.testTag("full_player_fav")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorito",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Seek Slider & Time Labels
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val posFloat = currentPositionMs.toFloat()
                            val durFloat = durationMs.coerceAtLeast(1L).toFloat()
                            var sliderValue by remember(posFloat) { mutableStateOf(posFloat) }

                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = { onSeek(sliderValue.toLong()) },
                                valueRange = 0f..durFloat,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatMs(currentPositionMs),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = formatMs(durationMs),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleShuffle) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Aleatorio",
                                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = onPrevious,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Anterior",
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Big Play/Pause FAB
                            FloatingActionButton(
                                onClick = onPlayPause,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("full_player_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            IconButton(
                                onClick = onNext,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Siguiente",
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(onClick = onToggleRepeat) {
                                Icon(
                                    imageVector = when (repeatMode) {
                                        RepeatMode.ONE -> Icons.Default.RepeatOne
                                        else -> Icons.Default.Repeat
                                    },
                                    contentDescription = "Repetir",
                                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Secondary Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDownload) {
                                Icon(
                                    imageVector = if (song.isDownloaded) Icons.Default.DownloadDone else Icons.Outlined.FileDownload,
                                    contentDescription = "Descargar MP3"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (song.isDownloaded) "Descargado" else "Descargar MP3")
                            }

                            TextButton(onClick = { showEqDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = "Ecualizador"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ecualizador")
                            }
                        }
                    }
                } else {
                    // Lyrics Screen
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Letra de Alabanza",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${song.title} - ${song.artist}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            Text(
                                text = song.lyrics.ifEmpty { "La letra no está disponible para esta canción todavía." },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 28.sp
                                ),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }

    // Timer Selector Dialog
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Temporizador de Apagado") },
            text = {
                Column {
                    listOf(0, 15, 30, 45, 60).forEach { mins ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = sleepTimerMinutes == mins,
                                onClick = {
                                    onSetSleepTimer(mins)
                                    showTimerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (mins == 0) "Desactivado" else "$mins Minutos")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Equalizer Selector Dialog
    if (showEqDialog) {
        AlertDialog(
            onDismissRequest = { showEqDialog = false },
            title = { Text("Ajustes de Sonido / Ecualizador") },
            text = {
                Column {
                    equalizerPresets.forEach { preset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedPreset.name == preset.name,
                                onClick = {
                                    onSelectPreset(preset)
                                    showEqDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(preset.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
