package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.SongEntity

data class ArtistItem(val name: String, val ministry: String, val photoUrl: String, val songCount: Int)

@Composable
fun ExploreScreen(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filteredSongs: List<SongEntity>,
    favoriteIds: List<String>,
    currentPlayingSong: SongEntity?,
    isPlaying: Boolean,
    onSongSelect: (SongEntity) -> Unit,
    onToggleFavorite: (SongEntity) -> Unit,
    onDownloadSong: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Canciones & Búsqueda, 1 = Artistas, 2 = Géneros

    val artistsList = remember(filteredSongs) {
        filteredSongs.groupBy { it.artist }.map { (artistName, songList) ->
            ArtistItem(
                name = artistName,
                ministry = songList.firstOrNull()?.ministry?.ifEmpty { "Ministerio" } ?: "Ministerio",
                photoUrl = songList.firstOrNull()?.coverUrl ?: "",
                songCount = songList.size
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 90.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Explorar Alabanzas",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar por título, artista o letra...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("explore_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Resultados (${filteredSongs.size})", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Artistas / Ministerios", modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Songs List
            if (filteredSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron canciones.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filteredSongs) { song ->
                        SongRowItem(
                            song = song,
                            isFavorite = favoriteIds.contains(song.id),
                            isCurrent = currentPlayingSong?.id == song.id,
                            isPlaying = isPlaying && currentPlayingSong?.id == song.id,
                            onPlay = { onSongSelect(song) },
                            onFavorite = { onToggleFavorite(song) },
                            onDownload = { onDownloadSong(song) }
                        )
                    }
                }
            }
        } else {
            // Artists Catalog Grid
            if (artistsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay artistas registrados en el catálogo.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(artistsList) { artist ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSearchChange(artist.name)
                                    selectedTab = 0
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = artist.photoUrl.ifEmpty { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300" },
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${artist.songCount} Canciones",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
