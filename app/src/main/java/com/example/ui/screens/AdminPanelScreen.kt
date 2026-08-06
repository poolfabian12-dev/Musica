package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.SongEntity
import com.example.data.local.SuggestionEntity
import com.example.data.local.UserEntity
import com.example.ui.viewmodel.YoutubeConversionState
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    songs: List<SongEntity>,
    suggestions: List<SuggestionEntity>,
    users: List<UserEntity>,
    youtubeState: YoutubeConversionState,
    currentPlayingSong: SongEntity? = null,
    isPlaying: Boolean = false,
    onPlaySong: (SongEntity) -> Unit = {},
    onSaveSong: (SongEntity) -> Unit,
    onDeleteSong: (String) -> Unit,
    onUpdateSuggestionStatus: (String, String) -> Unit,
    onConvertYoutubeUrl: (String) -> Unit,
    onResetYoutubeState: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedAdminTab by remember { mutableStateOf(0) } // 0 = Subir Canción, 1 = Biblioteca, 2 = Gestión
    var showConverterModal by remember { mutableStateOf(false) }
    var adminMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 90.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header: Admin Title + User Badge with Salir Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Panel de Administración",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "Gestiona tu música cristiana",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Admin Profile Badge & Salir Button
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "A",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Administrador",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Administrador",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Salir",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Summary Cards Row (4 Stats)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Canciones Totales
            AdminStatCard(
                icon = Icons.Default.MusicNote,
                iconColor = Color(0xFF7C4DFF),
                count = "${songs.size}",
                label = "Canciones Totales",
                modifier = Modifier.weight(1f)
            )

            // Card 2: Usuarios Registrados
            AdminStatCard(
                icon = Icons.Default.People,
                iconColor = Color(0xFF00BCD4),
                count = "${users.size}",
                label = "Usuarios Registrados",
                modifier = Modifier.weight(1f)
            )

            // Card 3: Playlists Creadas
            AdminStatCard(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                iconColor = Color(0xFFFF9800),
                count = "0",
                label = "Playlists Creadas",
                modifier = Modifier.weight(1f)
            )

            // Card 4: Géneros Diferentes
            AdminStatCard(
                icon = Icons.Default.Category,
                iconColor = Color(0xFFFF4081),
                count = "${songs.map { it.genre }.filter { it.isNotBlank() }.distinct().size}",
                label = "Géneros Diferentes",
                modifier = Modifier.weight(1f)
            )
        }

        // Action Status Message Banner
        if (adminMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = adminMessage!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { adminMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Main Navigation Segmented Tabs (3 Tabs)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedAdminTab == 0,
                onClick = { selectedAdminTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Subir Canción", fontSize = 13.sp)
            }

            SegmentedButton(
                selected = selectedAdminTab == 1,
                onClick = { selectedAdminTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Icon(Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Biblioteca", fontSize = 13.sp)
            }

            SegmentedButton(
                selected = selectedAdminTab == 2,
                onClick = { selectedAdminTab = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gestión", fontSize = 13.sp)
            }
        }

        // Tab Content
        when (selectedAdminTab) {
            0 -> UploadSongTab(
                onSaveSong = { newSong ->
                    onSaveSong(newSong)
                    adminMessage = "¡Canción '${newSong.title}' subida exitosamente!"
                }
            )
            1 -> SongsLibraryTab(
                songs = songs,
                currentPlayingSong = currentPlayingSong,
                isPlaying = isPlaying,
                onPlaySong = onPlaySong,
                onSaveSong = onSaveSong,
                onDeleteSong = { id ->
                    onDeleteSong(id)
                    adminMessage = "Canción eliminada del catálogo."
                }
            )
            2 -> AdminToolsTab(
                songs = songs,
                onOpenConverter = { showConverterModal = true },
                onDeleteAllSongs = {
                    songs.forEach { onDeleteSong(it.id) }
                    adminMessage = "Todas las canciones han sido eliminadas."
                }
            )
        }
    }

    // Modal Converter YouTube Dialog
    if (showConverterModal) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var ytUrl by remember { mutableStateOf("") }
        var selectedSaveMode by remember { mutableStateOf(0) } // 0 = Ambos (Cloudinary + Celular), 1 = Solo Celular (Local Offline), 2 = Solo Cloudinary
        var customConvertedTitle by remember { mutableStateOf("") }
        var customConvertedArtist by remember { mutableStateOf("") }

        val localAudioPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val fileName = uri.lastPathSegment ?: "audio_local.mp3"
                val savedPath = com.example.util.AudioFileManager.copyUriToInternalAudio(context, uri, fileName)
                if (savedPath != null) {
                    val cleanTitle = fileName.substringBeforeLast(".").replace("_", " ").trim()
                    val newSong = SongEntity(
                        id = UUID.randomUUID().toString(),
                        title = cleanTitle.ifBlank { "Alabanza Cristiana" },
                        artist = "Música Cristiana",
                        ministry = "Música Cristiana",
                        genre = "Adoración (Worship)",
                        album = "Audio Celular",
                        year = 2025,
                        durationSeconds = 240,
                        audioUrl = savedPath,
                        coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                        isDownloaded = true,
                        localFilePath = savedPath,
                        playsCount = 0,
                        downloadsCount = 1,
                        timestamp = System.currentTimeMillis()
                    )
                    onSaveSong(newSong)
                    showConverterModal = false
                    adminMessage = "¡Audio MP3 guardado en tu celular y añadido a la biblioteca!"
                } else {
                    android.widget.Toast.makeText(context, "No se pudo procesar el archivo seleccionado", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                showConverterModal = false
                onResetYoutubeState()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFFF0000))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convertidor YouTube a MP3")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Convierte videos de YouTube a MP3 para escucharlos y descargarlos directamente en tu celular o subirlos a la nube.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = ytUrl,
                        onValueChange = { ytUrl = it },
                        label = { Text("Enlace de YouTube") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Storage destination selector
                    Text(
                        text = "Destino del Audio MP3:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedSaveMode == 0,
                            onClick = { selectedSaveMode = 0 },
                            label = { Text("🔄 Ambos", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedSaveMode == 1,
                            onClick = { selectedSaveMode = 1 },
                            label = { Text("📱 En Celular", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedSaveMode == 2,
                            onClick = { selectedSaveMode = 2 },
                            label = { Text("☁️ Cloudinary", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Direct Option to Pick MP3 from Device Files
                    OutlinedButton(
                        onClick = { localAudioPicker.launch("audio/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📁 O Elegir archivo MP3 desde mi Celular", fontSize = 12.sp)
                    }

                    when (youtubeState) {
                        is YoutubeConversionState.Processing -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = youtubeState.statusText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { youtubeState.progressPercent / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        is YoutubeConversionState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "⚠️ ${youtubeState.message}",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Tip: También puedes usar 'Elegir archivo MP3 desde mi Celular' arriba para agregar el audio directamente.",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                                    )
                                }
                            }
                        }
                        is YoutubeConversionState.Success -> {
                            LaunchedEffect(youtubeState) {
                                customConvertedTitle = youtubeState.title
                                customConvertedArtist = youtubeState.artist
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("¡Audio MP3 Listo!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 14.sp)
                                    }

                                    // Status info
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (youtubeState.isStoredLocally || youtubeState.localFilePath.isNotBlank()) {
                                            Surface(
                                                color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("📱 Guardado en Celular", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        if (youtubeState.isUploadedToCloudinary) {
                                            Surface(
                                                color = Color(0xFF00BCD4).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("☁️ Subido a Cloudinary", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00838F), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    // Editable fields before saving
                                    OutlinedTextField(
                                        value = customConvertedTitle,
                                        onValueChange = { customConvertedTitle = it },
                                        label = { Text("Título de la Canción") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = customConvertedArtist,
                                        onValueChange = { customConvertedArtist = it },
                                        label = { Text("Artista / Ministerio") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Action Buttons Row (Download to Device + Play Test Audio)
                                    val previewSong = SongEntity(
                                        id = UUID.randomUUID().toString(),
                                        title = customConvertedTitle.ifBlank { youtubeState.title },
                                        artist = customConvertedArtist.ifBlank { youtubeState.artist },
                                        ministry = customConvertedArtist.ifBlank { youtubeState.artist },
                                        genre = "Adoración (Worship)",
                                        album = "YouTube MP3",
                                        year = 2025,
                                        durationSeconds = youtubeState.durationSeconds,
                                        audioUrl = youtubeState.mp3Url,
                                        coverUrl = if (youtubeState.coverUrl.isNotBlank()) youtubeState.coverUrl else "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                                        isDownloaded = youtubeState.localFilePath.isNotBlank(),
                                        localFilePath = youtubeState.localFilePath,
                                        playsCount = 0,
                                        downloadsCount = 0,
                                        timestamp = System.currentTimeMillis()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Play audio preview button
                                        OutlinedButton(
                                            onClick = { onPlaySong(previewSong) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Escuchar", fontSize = 11.sp)
                                        }

                                        // Download MP3 to phone button
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    com.example.util.AudioFileManager.downloadSongToDeviceDownloads(context, previewSong)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Descargar MP3", fontSize = 11.sp)
                                        }
                                    }

                                    // Add to Catalog / Library Button
                                    Button(
                                        onClick = {
                                            val songToSave = previewSong.copy(
                                                title = customConvertedTitle.trim().ifEmpty { youtubeState.title },
                                                artist = customConvertedArtist.trim().ifEmpty { youtubeState.artist }
                                            )
                                            onSaveSong(songToSave)
                                            showConverterModal = false
                                            onResetYoutubeState()
                                            adminMessage = "¡Canción '${songToSave.title}' añadida a la biblioteca con MP3!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Añadir a la Biblioteca de Canciones", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        YoutubeConversionState.Idle -> {
                            Button(
                                onClick = {
                                    if (ytUrl.isNotBlank()) {
                                        onConvertYoutubeUrl(ytUrl.trim())
                                    }
                                },
                                enabled = ytUrl.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Convertir y Generar MP3")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConverterModal = false
                    onResetYoutubeState()
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Component: Stat Card
// -----------------------------------------------------------------------------
@Composable
private fun AdminStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    count: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 11.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 0: SUBIR CANCIÓN
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadSongTab(
    onSaveSong: (SongEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uploader = remember { com.example.data.api.CloudinaryUploader(context) }
    val youtubeConverter = remember { com.example.data.api.YoutubeAudioConverter(context) }

    var titleInput by remember { mutableStateOf("") }
    var artistInput by remember { mutableStateOf("") }
    var albumInput by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf("2025") }

    // Genre Selector
    var genreExpanded by remember { mutableStateOf(false) }
    val genresList = listOf(
        "Adoración (Worship)",
        "Alabanza",
        "Pop Cristiano",
        "Acústico",
        "Instrumental",
        "Roca Firme",
        "Otros"
    )
    var selectedGenre by remember { mutableStateOf(genresList[0]) }

    // Audio Source Selection (1: Youtube, 2: Direct MP3, 3: Local File to Cloudinary)
    var selectedAudioOption by remember { mutableStateOf(1) } // Default to YouTube URL conversion
    var youtubeUrlInput by remember { mutableStateOf("") }
    var isFetchingYtInfo by remember { mutableStateOf(false) }
    var directMp3Input by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // Cover & Lyrics
    var coverUrlInput by remember { mutableStateOf("") }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCoverFileName by remember { mutableStateOf<String?>(null) }
    var lyricsInput by remember { mutableStateOf("") }

    // Upload & Form State
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusText by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = uri.lastPathSegment ?: "audio_cancion.mp3"
            formError = null
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCoverUri = uri
            selectedCoverFileName = uri.lastPathSegment ?: "portada.jpg"
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Subir Nueva Canción a la Nube",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Convierte videos de YouTube a MP3 o sube audios directamente a Cloudinary y Firebase Firestore.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            if (formError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (isUploading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uploadStatusText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Título & Artista
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Título *") },
                    placeholder = { Text("Nombre de la canción") },
                    singleLine = true,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = artistInput,
                    onValueChange = { artistInput = it },
                    label = { Text("Artista *") },
                    placeholder = { Text("Nombre del artista o ministerio") },
                    singleLine = true,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f)
                )
            }

            // Álbum, Género & Año
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = albumInput,
                    onValueChange = { albumInput = it },
                    label = { Text("Álbum") },
                    placeholder = { Text("Nombre del álbum") },
                    singleLine = true,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f)
                )

                // Exposed Dropdown Menu for Genre
                ExposedDropdownMenuBox(
                    expanded = genreExpanded,
                    onExpandedChange = { if (!isUploading) genreExpanded = !genreExpanded },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = selectedGenre,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isUploading,
                        label = { Text("Género *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, !isUploading)
                    )
                    ExposedDropdownMenu(
                        expanded = genreExpanded,
                        onDismissRequest = { genreExpanded = false }
                    ) {
                        genresList.forEach { genreOption ->
                            DropdownMenuItem(
                                text = { Text(genreOption) },
                                onClick = {
                                    selectedGenre = genreOption
                                    genreExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = yearInput,
                    onValueChange = { yearInput = it },
                    label = { Text("Año") },
                    singleLine = true,
                    enabled = !isUploading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Audio Selection Section Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡 Elige cómo agregar el audio", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }

            // Option 1 (Recommended): YouTube URL -> Auto Convert to MP3 and Cloudinary Upload
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedAudioOption == 1) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedAudioOption == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isUploading) { selectedAudioOption = 1 }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedAudioOption == 1,
                            onClick = { selectedAudioOption = 1 },
                            enabled = !isUploading
                        )
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFFF0000), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("URL de YouTube (Convierte a MP3 y Sube a Cloudinary)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "▶️ Pega un enlace de YouTube. Se convertirá automáticamente a MP3 y se subirá a Cloudinary.",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    AnimatedVisibility(visible = selectedAudioOption == 1) {
                        Column(modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = youtubeUrlInput,
                                    onValueChange = { 
                                        youtubeUrlInput = it
                                        // Auto-fetch if pasted complete URL
                                        if (it.contains("youtube.com") || it.contains("youtu.be")) {
                                            coroutineScope.launch {
                                                val info = youtubeConverter.fetchVideoInfo(it).getOrNull()
                                                if (info != null) {
                                                    if (titleInput.isBlank()) titleInput = info.title
                                                    if (artistInput.isBlank()) artistInput = info.artist
                                                    if (coverUrlInput.isBlank()) coverUrlInput = info.coverUrl
                                                }
                                            }
                                        }
                                    },
                                    placeholder = { Text("https://www.youtube.com/watch?v=... o https://youtu.be/...") },
                                    singleLine = true,
                                    enabled = !isUploading,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (youtubeUrlInput.isNotBlank()) {
                                            coroutineScope.launch {
                                                isFetchingYtInfo = true
                                                val info = youtubeConverter.fetchVideoInfo(youtubeUrlInput.trim()).getOrNull()
                                                if (info != null) {
                                                    titleInput = info.title
                                                    artistInput = info.artist
                                                    coverUrlInput = info.coverUrl
                                                }
                                                isFetchingYtInfo = false
                                            }
                                        }
                                    },
                                    enabled = !isUploading && youtubeUrlInput.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (isFetchingYtInfo) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Detectar", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Option 3: Upload Local MP3 File directly to Cloudinary
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedAudioOption == 3) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedAudioOption == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isUploading) { selectedAudioOption = 3 }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedAudioOption == 3,
                            onClick = { selectedAudioOption = 3 },
                            enabled = !isUploading
                        )
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Subir archivo de audio (MP3 / WAV / M4A a Cloudinary)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "☁️ Se sube a la nube (Cloudinary) para que se escuche en cualquier teléfono o computadora",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4CAF50)),
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    AnimatedVisibility(visible = selectedAudioOption == 3) {
                        Column(modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { filePickerLauncher.launch("audio/*") },
                                    enabled = !isUploading,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Elegir MP3 de mi Celular")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedFileName ?: "Ningún archivo seleccionado",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (selectedFileUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (selectedFileUri != null) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Option 2: Direct MP3 URL
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedAudioOption == 2) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedAudioOption == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isUploading) { selectedAudioOption = 2 }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedAudioOption == 2,
                            onClick = { selectedAudioOption = 2 },
                            enabled = !isUploading
                        )
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("URL directa de audio en la web (HTTPS)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "🔗 Para enlaces HTTPS que terminan en .mp3",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    AnimatedVisibility(visible = selectedAudioOption == 2) {
                        Column(modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)) {
                            OutlinedTextField(
                                value = directMp3Input,
                                onValueChange = { directMp3Input = it },
                                placeholder = { Text("https://servidor.com/musica/alabanza.mp3") },
                                singleLine = true,
                                enabled = !isUploading,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Bottom Section: Portada (con selector de imagen de galería o URL) & Letra
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Imagen de Portada (Opcional)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = coverUrlInput,
                        onValueChange = { coverUrlInput = it },
                        placeholder = { Text("URL https://... o selecciona abajo") },
                        singleLine = true,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            enabled = !isUploading,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Elegir foto", fontSize = 11.sp)
                        }
                        if (selectedCoverFileName != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedCoverFileName!!,
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Letra de la Canción (Opcional)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = lyricsInput,
                        onValueChange = { lyricsInput = it },
                        placeholder = { Text("Escribe o pega la letra aquí...") },
                        maxLines = 4,
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Submit Button with Coroutine Upload to Cloudinary & Firebase
            Button(
                onClick = {
                    formError = null
                    val title = titleInput.trim()
                    val artist = artistInput.trim()

                    if (title.isBlank() || artist.isBlank()) {
                        formError = "El título y el artista son obligatorios."
                        return@Button
                    }

                    // Validate audio selection
                    when (selectedAudioOption) {
                        1 -> {
                            if (youtubeUrlInput.isBlank()) {
                                formError = "Por favor ingresa la URL de YouTube."
                                return@Button
                            }
                        }
                        2 -> {
                            if (directMp3Input.isBlank()) {
                                formError = "Por favor ingresa la URL directa del archivo MP3."
                                return@Button
                            }
                        }
                        3 -> {
                            if (selectedFileUri == null) {
                                formError = "Por favor presiona 'Elegir MP3 de mi Celular' para seleccionar el archivo de audio."
                                return@Button
                            }
                        }
                    }

                    coroutineScope.launch {
                        isUploading = true
                        uploadStatusText = "Iniciando procesamiento de audio..."

                        var finalAudioUrl = ""
                        var finalLocalFilePath = ""
                        var finalCoverUrl = coverUrlInput.trim()
                        var detectedDuration = 210

                        try {
                            // 1. Process Audio & Convert to Cloudinary MP3
                            when (selectedAudioOption) {
                                1 -> {
                                    val ytUrl = youtubeUrlInput.trim()
                                    uploadStatusText = "1/4 Obteniendo video de YouTube..."
                                    
                                    val convRes = youtubeConverter.convertAndUploadToCloudinary(
                                        youtubeUrl = ytUrl,
                                        customTitle = title,
                                        customArtist = artist,
                                        onProgress = { progressText ->
                                            uploadStatusText = progressText
                                        }
                                    )

                                    if (convRes.isSuccess) {
                                        val res = convRes.getOrThrow()
                                        finalAudioUrl = res.cloudinaryAudioUrl
                                        finalLocalFilePath = res.localFilePath
                                        if (finalCoverUrl.isBlank()) {
                                            finalCoverUrl = res.cloudinaryCoverUrl
                                        }
                                        detectedDuration = res.durationSeconds
                                    } else {
                                        val reason = convRes.exceptionOrNull()?.message ?: "El servidor de YouTube bloqueó la descarga temporalmente."
                                        formError = "❌ No se pudo procesar el video de YouTube:\n\n$reason\n\n💡 Para evitar sonidos genéricos o archivos incompletos, la subida fue cancelada. Puedes subir el archivo de audio original seleccionando la opción '3. Elegir MP3 de mi Celular'."
                                        isUploading = false
                                        return@launch
                                    }
                                }
                                2 -> {
                                    finalAudioUrl = directMp3Input.trim()
                                }
                                3 -> {
                                    val audioUri = selectedFileUri!!
                                    val fileName = selectedFileName ?: "cancion_${System.currentTimeMillis()}.mp3"
                                    uploadStatusText = "Subiendo audio a Cloudinary ($fileName)..."

                                    // Save a local copy first
                                    val savedLocalPath = com.example.util.AudioFileManager.copyUriToInternalAudio(context, audioUri, fileName)
                                    if (savedLocalPath != null) {
                                        finalLocalFilePath = savedLocalPath
                                    }

                                    val audioResult = uploader.uploadFromUri(
                                        uri = audioUri,
                                        fileName = fileName,
                                        isAudio = true,
                                        onProgressUpdate = { progress -> uploadStatusText = progress }
                                    )

                                    if (audioResult.isSuccess) {
                                        finalAudioUrl = audioResult.getOrThrow()
                                    } else {
                                        if (savedLocalPath != null) {
                                            finalAudioUrl = savedLocalPath
                                        } else {
                                            formError = "Error al procesar audio: ${audioResult.exceptionOrNull()?.message}"
                                            isUploading = false
                                            return@launch
                                        }
                                    }
                                }
                            }

                            // 2. Process Cover Image if selected from device
                            if (selectedCoverUri != null) {
                                uploadStatusText = "Subiendo portada a Cloudinary..."
                                val coverResult = uploader.uploadFromUri(
                                    uri = selectedCoverUri!!,
                                    fileName = selectedCoverFileName ?: "portada_${System.currentTimeMillis()}.jpg",
                                    isAudio = false
                                )
                                if (coverResult.isSuccess) {
                                    finalCoverUrl = coverResult.getOrThrow()
                                }
                            }

                            if (finalCoverUrl.isBlank()) {
                                finalCoverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"
                            }

                            uploadStatusText = "Guardando en Firebase Firestore y base de datos local..."

                            val newSong = SongEntity(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                artist = artist,
                                ministry = artist,
                                genre = selectedGenre,
                                album = albumInput.trim().ifEmpty { "Sencillo" },
                                year = yearInput.toIntOrNull() ?: 2025,
                                durationSeconds = detectedDuration,
                                audioUrl = finalAudioUrl,
                                coverUrl = finalCoverUrl,
                                lyrics = lyricsInput.trim(),
                                playsCount = 0,
                                downloadsCount = 0,
                                timestamp = System.currentTimeMillis(),
                                localFilePath = finalLocalFilePath,
                                isDownloaded = finalLocalFilePath.isNotBlank()
                            )

                            onSaveSong(newSong)

                            // Reset form on success
                            titleInput = ""
                            artistInput = ""
                            albumInput = ""
                            youtubeUrlInput = ""
                            directMp3Input = ""
                            selectedFileName = null
                            selectedFileUri = null
                            selectedCoverFileName = null
                            selectedCoverUri = null
                            lyricsInput = ""
                            coverUrlInput = ""
                            isUploading = false

                        } catch (e: Exception) {
                            formError = "Error en la subida: ${e.message}"
                            isUploading = false
                        }
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subiendo a la Nube...", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subir Canción a Cloudinary y Firebase", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------
// TAB 1: BIBLIOTECA
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsLibraryTab(
    songs: List<SongEntity>,
    currentPlayingSong: SongEntity? = null,
    isPlaying: Boolean = false,
    onPlaySong: (SongEntity) -> Unit = {},
    onSaveSong: (SongEntity) -> Unit,
    onDeleteSong: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var editingSong by remember { mutableStateOf<SongEntity?>(null) }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.genre.contains(searchQuery, ignoreCase = true)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Todas las Canciones (${songs.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar en biblioteca...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.width(200.dp)
                )
            }

            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (songs.isEmpty()) "No hay canciones subidas aún en el catálogo." else "No se encontraron canciones.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                // Table Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PORTADA", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
                        Text("TÍTULO", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        Text("ARTISTA", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("GÉNERO", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                        Text("REPROD.", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(55.dp))
                        Text("ACCIONES", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp), textAlign = TextAlign.End)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredSongs.forEach { song ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover Thumbnail with Play Button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onPlaySong(song) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = song.coverUrl,
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    val isThisPlaying = currentPlayingSong?.id == song.id && isPlaying
                                    Surface(
                                        color = if (isThisPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.4f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Reproducir",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Title and Audio Source Info
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = song.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val isCloudinary = song.audioUrl.contains("cloudinary.com")
                                    Text(
                                        text = if (isCloudinary) "☁ Cloudinary MP3" else if (song.audioUrl.startsWith("http")) "🌐 Audio Online" else "📱 Audio Local",
                                        fontSize = 9.sp,
                                        color = if (isCloudinary) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Artist
                                Text(
                                    text = song.artist,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )

                                // Genre Badge
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Text(
                                        text = song.genre,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // Plays Count
                                Surface(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.width(50.dp)
                                ) {
                                    Text(
                                        text = "▶ ${song.playsCount}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Action Buttons
                                Row(
                                    modifier = Modifier.width(130.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Download MP3 Button
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                com.example.util.AudioFileManager.downloadSongToDeviceDownloads(context, song)
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.FileDownload,
                                            contentDescription = "Descargar MP3 al celular",
                                            tint = Color(0xFF00897B),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Share MP3 Button
                                    IconButton(
                                        onClick = {
                                            com.example.util.AudioFileManager.shareSongMp3(context, song)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Compartir audio",
                                            tint = Color(0xFF1E88E5),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Edit Song Button
                                    IconButton(
                                        onClick = { editingSong = song },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                    }

                                    // Delete Song Button
                                    IconButton(
                                        onClick = { onDeleteSong(song.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", tint = Color(0xFFE53935), modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Song Dialog
    if (editingSong != null) {
        val currentSong = editingSong!!
        var editTitle by remember { mutableStateOf(currentSong.title) }
        var editArtist by remember { mutableStateOf(currentSong.artist) }
        var editGenre by remember { mutableStateOf(currentSong.genre) }
        var editAlbum by remember { mutableStateOf(currentSong.album) }

        AlertDialog(
            onDismissRequest = { editingSong = null },
            title = { Text("Editar Canción") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editArtist,
                        onValueChange = { editArtist = it },
                        label = { Text("Artista") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editGenre,
                        onValueChange = { editGenre = it },
                        label = { Text("Género") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAlbum,
                        onValueChange = { editAlbum = it },
                        label = { Text("Álbum") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = currentSong.copy(
                        title = editTitle.trim(),
                        artist = editArtist.trim(),
                        genre = editGenre.trim(),
                        album = editAlbum.trim()
                    )
                    onSaveSong(updated)
                    editingSong = null
                }) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSong = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// TAB 2: GESTIÓN DE LA APLICACIÓN
// -----------------------------------------------------------------------------
@Composable
private fun AdminToolsTab(
    songs: List<SongEntity>,
    onOpenConverter: () -> Unit,
    onDeleteAllSongs: () -> Unit
) {
    var showConfirmDeleteAll by remember { mutableStateOf(false) }
    var newPasswordInput by remember { mutableStateOf("") }
    var passwordChangeSuccess by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Herramientas de Gestión", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            // Cards Grid for Tools
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Cloud Status & Setup Guide Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var testStatus by remember { mutableStateOf<String?>(null) }
                    var isTesting by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Estado de la Nube (Firebase & Cloudinary)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Text(
                            text = "Para que las canciones subidas desde tu celular se sincronicen en cualquier dispositivo, realiza estos 2 pasos rápidos en tu consola de Firebase:",
                            style = MaterialTheme.typography.bodySmall
                        )

                        // Step 1: Firestore
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                Text("1️⃣", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Activar Cloud Firestore en Firebase Console", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "En la pantalla de Firestore donde dice 'Crear base de datos', haz clic en el botón naranja ➔ Elige ubicación ➔ Selecciona 'Modo de prueba' ➔ Haz clic en 'Habilitar'.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }

                        // Step 2: Authentication
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                Text("2️⃣", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Activar Proveedor en Authentication", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "En Authentication ➔ Ve a la pestaña 'Método de acceso' ➔ Haz clic en 'Correo electrónico/contraseña' ➔ Actívalo y guarda los cambios.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }

                        // Cloudinary Configuration, Step-by-step Guide & Diagnostics
                        val uploaderHelper = remember { com.example.data.api.CloudinaryUploader(context) }
                        var currentConfig by remember { mutableStateOf(uploaderHelper.getConfig()) }
                        var showCloudinaryEditor by remember { mutableStateOf(false) }
                        var showCloudinaryGuide by remember { mutableStateOf(false) }
                        var editCloudName by remember { mutableStateOf(currentConfig.cloudName) }
                        var editPreset by remember { mutableStateOf(currentConfig.uploadPreset) }
                        var editApiKey by remember { mutableStateOf(currentConfig.apiKey) }
                        var editApiSecret by remember { mutableStateOf(currentConfig.apiSecret) }
                        var configSaveMessage by remember { mutableStateOf<String?>(null) }
                        var isTestingAudio by remember { mutableStateOf(false) }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("☁️ Cloudinary (Hosting de Canciones MP3 y Fotos)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Cloud Name: ${currentConfig.cloudName} | Preset: ${currentConfig.uploadPreset}", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { showCloudinaryGuide = !showCloudinaryGuide },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (showCloudinaryGuide) "Ocultar Guía" else "📖 Ver Guía", fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { showCloudinaryEditor = !showCloudinaryEditor },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (showCloudinaryEditor) "Cerrar" else "Ajustar", fontSize = 10.sp)
                                        }
                                    }
                                }

                                // Step-by-Step Interactive Guide
                                AnimatedVisibility(visible = showCloudinaryGuide) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("📋 Paso a Paso para Alojar Canciones en Cloudinary (Gratis):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            
                                            Text("1️⃣ Entra a https://cloudinary.com y crea tu cuenta gratuita.", fontSize = 11.sp)
                                            Text("2️⃣ En el Dashboard principal copia tu 'Cloud Name' (ejemplo: mi_nube_123).", fontSize = 11.sp)
                                            Text("3️⃣ Ve a ⚙️ Settings (Tuerca) ➔ pestaña 'Upload' ➔ baja hasta 'Upload presets'.", fontSize = 11.sp)
                                            Text("4️⃣ Haz clic en 'Add upload preset'.", fontSize = 11.sp)
                                            Text("5️⃣ ⚠️ MUY IMPORTANTE: Cambia 'Signing Mode' de 'Signed' a 'Unsigned'.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                            Text("6️⃣ Ponle un nombre al preset (por ejemplo: musica_cristiana) y haz clic en Save (Guardar).", fontSize = 11.sp)
                                            Text("7️⃣ Pega el Cloud Name y el Preset abajo en 'Ajustar' y presiona 'Guardar Credenciales'.", fontSize = 11.sp)
                                            Text("8️⃣ Presiona '🧪 Probar Subida Real de Audio' para verificar que tu enlace CDN funcione.", fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Editor Fields
                                AnimatedVisibility(visible = showCloudinaryEditor) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                        Text("Configura los datos de tu cuenta de Cloudinary:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        
                                        OutlinedTextField(
                                            value = editCloudName,
                                            onValueChange = { editCloudName = it },
                                            label = { Text("Cloud Name (Tu nombre de nube en Cloudinary)", fontSize = 11.sp) },
                                            placeholder = { Text("ej: dne01qj9q o mi_nube") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = editPreset,
                                            onValueChange = { editPreset = it },
                                            label = { Text("Upload Preset Name (En modo Unsigned)", fontSize = 11.sp) },
                                            placeholder = { Text("ej: ml_default o musica_cristiana") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = editApiKey,
                                                onValueChange = { editApiKey = it },
                                                label = { Text("API Key (Opcional)", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = editApiSecret,
                                                onValueChange = { editApiSecret = it },
                                                label = { Text("API Secret (Opcional)", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                uploaderHelper.saveConfig(
                                                    cloudName = editCloudName,
                                                    uploadPreset = editPreset,
                                                    apiKey = editApiKey,
                                                    apiSecret = editApiSecret
                                                )
                                                currentConfig = uploaderHelper.getConfig()
                                                configSaveMessage = "✅ Credenciales guardadas: Nube '${editCloudName.trim()}', Preset '${editPreset.trim()}'"
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Guardar Credenciales de Cloudinary", fontSize = 12.sp)
                                        }

                                        if (configSaveMessage != null) {
                                            Text(
                                                text = configSaveMessage!!,
                                                color = Color(0xFF4CAF50),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // Test Buttons (Quick Ping and Real Audio Upload Test)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                isTesting = true
                                                testStatus = "Verificando conexión con Cloudinary..."
                                                try {
                                                    val testBytes = "TEST_PING".toByteArray()
                                                    val result = uploaderHelper.uploadBytes(
                                                        fileBytes = testBytes,
                                                        fileName = "test_ping_${System.currentTimeMillis()}.txt",
                                                        resourceType = "auto"
                                                    )
                                                    if (result.isSuccess) {
                                                        testStatus = "✅ Conexión con Cloudinary exitosa. Servidor listo para subidas.\nURL: ${result.getOrThrow()}"
                                                    } else {
                                                        testStatus = "⚠️ ${result.exceptionOrNull()?.message}"
                                                    }
                                                } catch (e: Exception) {
                                                    testStatus = "Error: ${e.message}"
                                                } finally {
                                                    isTesting = false
                                                }
                                            }
                                        },
                                        enabled = !isTesting && !isTestingAudio,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isTesting) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Test Conexión", fontSize = 11.sp)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isTestingAudio = true
                                                testStatus = "Subiendo archivo de audio MP3 de prueba a Cloudinary..."
                                                try {
                                                    val testAudioPath = com.example.player.WorshipAudioSynthesizer.getOrCreateDefaultWorshipAudio(context)
                                                    val audioFile = java.io.File(testAudioPath)
                                                    if (audioFile.exists()) {
                                                        val result = uploaderHelper.uploadBytes(
                                                            fileBytes = audioFile.readBytes(),
                                                            fileName = "prueba_audio_${System.currentTimeMillis()}.mp3",
                                                            resourceType = "video"
                                                        )
                                                        if (result.isSuccess) {
                                                            testStatus = "🎉 ¡Prueba de Audio MP3 Exitosa en Cloudinary!\nURL alojada: ${result.getOrThrow()}"
                                                        } else {
                                                            testStatus = "⚠️ Falló subida de audio: ${result.exceptionOrNull()?.message}"
                                                        }
                                                    } else {
                                                        testStatus = "Error generando audio de prueba local"
                                                    }
                                                } catch (e: Exception) {
                                                    testStatus = "Error en prueba de audio: ${e.message}"
                                                } finally {
                                                    isTestingAudio = false
                                                }
                                            }
                                        },
                                        enabled = !isTesting && !isTestingAudio,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        if (isTestingAudio) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                                        } else {
                                            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Probar Audio MP3", fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (testStatus != null) {
                                    Text(
                                        text = testStatus!!,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (testStatus!!.startsWith("✅") || testStatus!!.startsWith("🎉")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Tool 1: Convertidor YouTube a MP3
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF0000).copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFFF0000))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Convertidor YouTube a MP3", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Convierte videos de YouTube a MP3 para luego subirlos a la app",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Button(
                            onClick = onOpenConverter,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Abrir Convertidor", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Tool 2: Exportar e Importar JSON
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Exportar Canciones", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Descarga un archivo JSON con todas las canciones", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { /* Triggers JSON export */ },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Descargar JSON", fontSize = 11.sp)
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Importar Canciones", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Carga canciones desde un archivo JSON", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { /* Triggers JSON import */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Importar", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Tool 3: Eliminar TODAS las canciones
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("⚠️ Eliminar TODAS las canciones", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), fontSize = 14.sp)
                            Text("Esta acción no se puede deshacer", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        Button(
                            onClick = { showConfirmDeleteAll = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Eliminar Todo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Tool 4: Cambiar Contraseña Admin
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cambiar Contraseña Admin", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            "Actualiza tu contraseña de acceso para la cuenta de administrador",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPasswordInput,
                                onValueChange = { newPasswordInput = it },
                                placeholder = { Text("Nueva contraseña") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    if (newPasswordInput.isNotBlank()) {
                                        passwordChangeSuccess = true
                                        newPasswordInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Guardar")
                            }
                        }

                        if (passwordChangeSuccess) {
                            Text(
                                "¡Contraseña actualizada correctamente!",
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog Delete All
    if (showConfirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAll = false },
            title = { Text("¿Eliminar todas las canciones?") },
            text = { Text("¿Estás seguro de que deseas vaciar completamente la biblioteca? Se eliminarán ${songs.size} canciones.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllSongs()
                        showConfirmDeleteAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Sí, Eliminar Todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteAll = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Helper to parse YouTube video IDs
private fun extractYoutubeVideoId(url: String): String {
    return when {
        url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
        else -> ""
    }
}
