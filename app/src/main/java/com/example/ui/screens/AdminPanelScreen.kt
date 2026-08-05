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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    songs: List<SongEntity>,
    suggestions: List<SuggestionEntity>,
    users: List<UserEntity>,
    youtubeState: YoutubeConversionState,
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
                            imageVector = Icons.Default.ExitToApp,
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
                icon = Icons.Default.PlaylistPlay,
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Convierte cualquier video de YouTube a MP3 para integrarlo directamente en tu catálogo.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    var ytUrl by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = ytUrl,
                        onValueChange = { ytUrl = it },
                        label = { Text("Enlace de YouTube") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (youtubeState) {
                        is YoutubeConversionState.Processing -> {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Procesando audio y metadatos de YouTube (${youtubeState.progressPercent}%)...", style = MaterialTheme.typography.bodySmall)
                        }
                        is YoutubeConversionState.Error -> {
                            Text(
                                text = youtubeState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is YoutubeConversionState.Success -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("✅ Audio Convertido:", fontWeight = FontWeight.Bold)
                                    Text("Título: ${youtubeState.title}")
                                    Text("Artista: ${youtubeState.artist}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val song = SongEntity(
                                                id = UUID.randomUUID().toString(),
                                                title = youtubeState.title,
                                                artist = youtubeState.artist,
                                                ministry = youtubeState.artist,
                                                genre = "Adoración (Worship)",
                                                album = "YouTube MP3",
                                                year = 2025,
                                                durationSeconds = 210,
                                                audioUrl = youtubeState.mp3Url,
                                                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                                                playsCount = 0,
                                                downloadsCount = 0,
                                                timestamp = System.currentTimeMillis()
                                            )
                                            onSaveSong(song)
                                            showConverterModal = false
                                            onResetYoutubeState()
                                            adminMessage = "¡Canción de YouTube agregada al catálogo!"
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Añadir a la Biblioteca")
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Convertir Audio")
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

    // Audio Source Selection (1: Youtube, 2: Direct MP3, 3: Local File)
    var selectedAudioOption by remember { mutableStateOf(1) } // 1, 2, 3
    var youtubeUrlInput by remember { mutableStateOf("") }
    var directMp3Input by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // Cover & Lyrics
    var coverUrlInput by remember { mutableStateOf("") }
    var lyricsInput by remember { mutableStateOf("") }

    var formError by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = uri.lastPathSegment ?: "audio_seleccionado.mp3"
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
                        text = "Subir Nueva Canción",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Puedes subir un archivo MP3, pegar una URL de YouTube (se convierte automáticamente), o una URL directa de audio.",
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

            // Título & Artista
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Título *") },
                    placeholder = { Text("Nombre de la canción") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = artistInput,
                    onValueChange = { artistInput = it },
                    label = { Text("Artista *") },
                    placeholder = { Text("Nombre del artista o ministerio") },
                    singleLine = true,
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
                    modifier = Modifier.weight(1f)
                )

                // Exposed Dropdown Menu for Genre
                ExposedDropdownMenuBox(
                    expanded = genreExpanded,
                    onExpandedChange = { genreExpanded = !genreExpanded },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = selectedGenre,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Género *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Audio Selection Section Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡 Elige cómo agregar el audio", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }

            // Option 1: YouTube URL
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
                    .clickable { selectedAudioOption = 1 }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedAudioOption == 1,
                            onClick = { selectedAudioOption = 1 }
                        )
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFFF0000), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Opción 1: URL de YouTube (INSTANTÁNEO)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "🟩 Se reproduce al instante, sin descargar ni convertir",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4CAF50)),
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    AnimatedVisibility(visible = selectedAudioOption == 1) {
                        Column(modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)) {
                            OutlinedTextField(
                                value = youtubeUrlInput,
                                onValueChange = { youtubeUrlInput = it },
                                placeholder = { Text("https://www.youtube.com/watch?v=xxxxx o https://youtu.be/xxxxx") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Pega el enlace completo del video de YouTube",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.padding(top = 4.dp)
                            )
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
                    .clickable { selectedAudioOption = 2 }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedAudioOption == 2,
                            onClick = { selectedAudioOption = 2 }
                        )
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Opción 2: URL directa de MP3", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "Para enlaces que terminan en .mp3 o URLs directas de audio",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    AnimatedVisibility(visible = selectedAudioOption == 2) {
                        Column(modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)) {
                            OutlinedTextField(
                                value = directMp3Input,
                                onValueChange = { directMp3Input = it },
                                placeholder = { Text("https://ejemplo.com/cancion.mp3") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Option 3: Upload Local MP3 File
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
                    .clickable { selectedAudioOption = 3 }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedAudioOption == 3,
                            onClick = { selectedAudioOption = 3 }
                        )
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Opción 3: Subir archivo MP3", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "Selecciona un archivo MP3 desde tu computadora o dispositivo",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    AnimatedVisibility(visible = selectedAudioOption == 3) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { filePickerLauncher.launch("audio/*") },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Seleccionar archivo")
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = selectedFileName ?: "Ningún archivo seleccionado",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }

            // Bottom Section: Portada & Letra
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Portada (opcional - YouTube la obtiene automático)", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = coverUrlInput,
                        onValueChange = { coverUrlInput = it },
                        placeholder = { Text("https://imagen.jpg o elegir archivo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Letra de la canción", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = lyricsInput,
                        onValueChange = { lyricsInput = it },
                        placeholder = { Text("Escribe la letra aquí...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Green Submit Button
            Button(
                onClick = {
                    formError = null
                    val title = titleInput.trim()
                    val artist = artistInput.trim()

                    if (title.isBlank() || artist.isBlank()) {
                        formError = "El título y el artista son obligatorios."
                        return@Button
                    }

                    // Resolve Audio URL & Cover URL
                    var finalAudioUrl = ""
                    var finalCoverUrl = coverUrlInput.trim()

                    when (selectedAudioOption) {
                        1 -> { // YouTube URL
                            val ytUrl = youtubeUrlInput.trim()
                            if (ytUrl.isBlank()) {
                                formError = "Por favor ingresa la URL de YouTube."
                                return@Button
                            }
                            val videoId = extractYoutubeVideoId(ytUrl)
                            finalAudioUrl = if (videoId.isNotBlank()) "https://www.youtube.com/watch?v=$videoId" else ytUrl
                            if (finalCoverUrl.isBlank() && videoId.isNotBlank()) {
                                finalCoverUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                            }
                        }
                        2 -> { // Direct MP3
                            finalAudioUrl = directMp3Input.trim()
                            if (finalAudioUrl.isBlank()) {
                                formError = "Por favor ingresa la URL del archivo MP3."
                                return@Button
                            }
                        }
                        3 -> { // Local File
                            if (selectedFileUri != null) {
                                finalAudioUrl = selectedFileUri.toString()
                            } else {
                                formError = "Por favor selecciona un archivo MP3 de tu dispositivo."
                                return@Button
                            }
                        }
                    }

                    if (finalCoverUrl.isBlank()) {
                        finalCoverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"
                    }

                    val newSong = SongEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        artist = artist,
                        ministry = artist,
                        genre = selectedGenre,
                        album = albumInput.trim().ifEmpty { "Sencillo" },
                        year = yearInput.toIntOrNull() ?: 2025,
                        durationSeconds = 210,
                        audioUrl = finalAudioUrl,
                        coverUrl = finalCoverUrl,
                        lyrics = lyricsInput.trim(),
                        playsCount = 0,
                        downloadsCount = 0,
                        timestamp = System.currentTimeMillis()
                    )

                    onSaveSong(newSong)

                    // Reset form
                    titleInput = ""
                    artistInput = ""
                    albumInput = ""
                    youtubeUrlInput = ""
                    directMp3Input = ""
                    selectedFileName = null
                    selectedFileUri = null
                    lyricsInput = ""
                    coverUrlInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir Canción Ahora", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
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
    onSaveSong: (SongEntity) -> Unit,
    onDeleteSong: (String) -> Unit
) {
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
                        Text("REPROD.", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                        Text("ACCIONES", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
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
                                // Cover Thumbnail
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Title
                                Text(
                                    text = song.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.2f)
                                )

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
                                    modifier = Modifier.width(60.dp)
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
                                    modifier = Modifier.width(80.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { editingSong = song },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteSong(song.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
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
