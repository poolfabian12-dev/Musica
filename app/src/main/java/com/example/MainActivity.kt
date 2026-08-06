package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FullScreenPlayerDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SecurityReverificationDialog
import com.example.ui.components.SuggestSongDialog
import com.example.ui.screens.*
import com.example.ui.theme.MusicaCristianaTheme
import com.example.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Explore : Screen("explore", "Explorar", Icons.Default.Search)
    object Downloads : Screen("downloads", "Descargas", Icons.Default.DownloadForOffline)
    object Favorites : Screen("favorites", "Favoritos", Icons.Default.Favorite)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
    object Admin : Screen("admin", "Admin", Icons.Outlined.AdminPanelSettings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MusicaCristianaTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    // Collect Reactive States
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val needsReverification by viewModel.needsReverification.collectAsStateWithLifecycle()
    val autoLoginCount by viewModel.autoLoginCount.collectAsStateWithLifecycle()
    val savedUserForReverification by viewModel.savedUserForReverification.collectAsStateWithLifecycle()

    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val favoriteSongIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val userPlaylists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val youtubeState by viewModel.youtubeState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()

    // Player State
    val currentSong by viewModel.playerManager.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.playerManager.currentPosition.collectAsStateWithLifecycle()
    val durationMs by viewModel.playerManager.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.playerManager.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.playerManager.isShuffle.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.playerManager.sleepTimerMinutes.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.playerManager.selectedPreset.collectAsStateWithLifecycle()

    // Screen Navigation State
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showFullScreenPlayer by remember { mutableStateOf(false) }
    var showSuggestDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // User Message Toast Effect
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val favoriteSongs = remember(allSongs, favoriteSongIds) {
        allSongs.filter { favoriteSongIds.contains(it.id) }
    }

    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    val isAdmin = currentUser?.role == "admin"
    val isGuest = currentUser?.role == "guest"

    // Security Re-verification Dialog after 20 auto-logins
    if (needsReverification) {
        SecurityReverificationDialog(
            user = savedUserForReverification,
            onVerify = { pwd -> viewModel.verifyIdentity(pwd) },
            onSwitchUser = { viewModel.switchUser() }
        )
    }

    if (currentUser == null) {
        LoginScreen(
            onLoginSuccess = { email, name, role, password ->
                viewModel.login(email, name, role, password)
                currentScreen = Screen.Home
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Column {
                    // MiniPlayer Sticky Banner
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                        onPlayPause = { viewModel.playerManager.togglePlayPause() },
                        onNext = { viewModel.playerManager.playNext() },
                        onOpenFullScreen = { showFullScreenPlayer = true }
                    )

                    // Bottom Navigation Bar
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        val screens = if (isAdmin) {
                            listOf(Screen.Home, Screen.Explore, Screen.Downloads, Screen.Favorites, Screen.Profile, Screen.Admin)
                        } else {
                            listOf(Screen.Home, Screen.Explore, Screen.Downloads, Screen.Favorites, Screen.Profile)
                        }

                        screens.forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen.route == screen.route,
                                onClick = { currentScreen = screen },
                                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("nav_${screen.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        songs = allSongs,
                        favoriteIds = favoriteSongIds,
                        currentPlayingSong = currentSong,
                        isPlaying = isPlaying,
                        unreadNotificationsCount = unreadNotificationsCount,
                        isGuest = isGuest,
                        onSongSelect = { song -> viewModel.playSong(song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onDownloadSong = { song -> viewModel.downloadSong(song) },
                        onOpenSuggestDialog = { showSuggestDialog = true },
                        onOpenNotifications = { currentScreen = Screen.Profile },
                        onOpenAuth = { viewModel.logout() }
                    )

                    Screen.Explore -> ExploreScreen(
                        searchQuery = searchQuery,
                        onSearchChange = { q -> viewModel.setSearchQuery(q) },
                        filteredSongs = filteredSongs,
                        favoriteIds = favoriteSongIds,
                        currentPlayingSong = currentSong,
                        isPlaying = isPlaying,
                        onSongSelect = { song -> viewModel.playSong(song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onDownloadSong = { song -> viewModel.downloadSong(song) }
                    )

                    Screen.Downloads -> DownloadsScreen(
                        downloadedSongs = downloadedSongs,
                        favoriteIds = favoriteSongIds,
                        currentPlayingSong = currentSong,
                        isPlaying = isPlaying,
                        isGuest = isGuest,
                        onSongSelect = { song -> viewModel.playSong(song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onRemoveDownload = { id -> viewModel.removeDownload(id) },
                        onOpenAuth = { viewModel.logout() }
                    )

                    Screen.Favorites -> FavoritesScreen(
                        favoriteSongs = favoriteSongs,
                        favoriteIds = favoriteSongIds,
                        playlists = userPlaylists,
                        currentPlayingSong = currentSong,
                        isPlaying = isPlaying,
                        isGuest = isGuest,
                        onSongSelect = { song -> viewModel.playSong(song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onDownloadSong = { song -> viewModel.downloadSong(song) },
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onOpenAuth = { viewModel.logout() }
                    )

                    Screen.Profile -> ProfileScreen(
                        currentUser = currentUser,
                        favoriteCount = favoriteSongs.size,
                        downloadCount = downloadedSongs.size,
                        notifications = notifications,
                        autoLoginCount = autoLoginCount,
                        firebaseSyncStatus = firebaseSyncStatus,
                        onLoginAsRole = { email, name, role -> viewModel.login(email, name, role) },
                        onLogout = { viewModel.logout() },
                        onMarkNotificationRead = { id -> viewModel.markNotificationRead(id) },
                        onOpenAuth = { viewModel.logout() }
                    )

                    Screen.Admin -> AdminPanelScreen(
                        songs = allSongs,
                        suggestions = suggestions,
                        users = allUsers,
                        youtubeState = youtubeState,
                        currentPlayingSong = currentSong,
                        isPlaying = isPlaying,
                        onPlaySong = { song -> viewModel.playSong(song) },
                        onSaveSong = { song -> viewModel.saveSong(song) },
                        onDeleteSong = { id -> viewModel.deleteSong(id) },
                        onUpdateSuggestionStatus = { id, status -> viewModel.updateSuggestionStatus(id, status) },
                        onConvertYoutubeUrl = { url -> viewModel.convertYoutubeUrl(url) },
                        onResetYoutubeState = { viewModel.resetYoutubeState() },
                        onLogout = { viewModel.logout() }
                    )
                }
            }
        }
    }

    // Modal Full Screen Player
    FullScreenPlayerDialog(
        show = showFullScreenPlayer,
        song = currentSong,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        isFavorite = favoriteSongIds.contains(currentSong?.id),
        repeatMode = repeatMode,
        isShuffle = isShuffle,
        sleepTimerMinutes = sleepTimerMinutes,
        equalizerPresets = viewModel.playerManager.equalizerPresets,
        selectedPreset = selectedPreset,
        onDismiss = { showFullScreenPlayer = false },
        onPlayPause = { viewModel.playerManager.togglePlayPause() },
        onNext = { viewModel.playerManager.playNext() },
        onPrevious = { viewModel.playerManager.playPrevious() },
        onSeek = { pos -> viewModel.playerManager.seekTo(pos) },
        onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
        onDownload = { currentSong?.let { viewModel.downloadSong(it) } },
        onToggleRepeat = { viewModel.playerManager.toggleRepeatMode() },
        onToggleShuffle = { viewModel.playerManager.toggleShuffle() },
        onSetSleepTimer = { mins -> viewModel.playerManager.setSleepTimer(mins) },
        onSelectPreset = { preset -> viewModel.playerManager.setEqualizerPreset(preset) }
    )

    // Modal User Suggest Song Dialog
    SuggestSongDialog(
        show = showSuggestDialog,
        onDismiss = { showSuggestDialog = false },
        onSubmit = { title, artist, ministry, reason ->
            viewModel.submitSuggestion(title, artist, ministry, reason)
        }
    )
}
