package com.velora.app

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.SessionToken
import com.velora.app.ui.components.LiquidGlassSurface
import com.velora.app.ui.components.liquidPressEffect
import com.velora.app.ui.components.bouncePressEffect
import com.velora.app.ui.screens.*
import com.velora.app.model.ZipPlaylistMode
import com.velora.app.ui.theme.VeloraTheme
import kotlinx.coroutines.launch

const val APP_VERSION = "1.1.1"

@UnstableApi
class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { uri ->
            val mime = contentResolver.getType(uri) ?: ""
            if (mime == "application/zip" || uri.path?.endsWith(".zip") == true)
                viewModel.importZip(uri) else viewModel.playUri(uri, mime)
        }
        setContent {
            // Material You preference persisted via SharedPreferences
            val prefs = getSharedPreferences("velora_prefs", MODE_PRIVATE)
            var useMaterialYou by remember { mutableStateOf(prefs.getBoolean("material_you", true)) }
            VeloraTheme(useMaterialYou = useMaterialYou) {
                VeloraApp(
                    viewModel = viewModel,
                    useMaterialYou = useMaterialYou,
                    onMaterialYouToggle = { enabled ->
                        useMaterialYou = enabled
                        prefs.edit().putBoolean("material_you", enabled).apply()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun VeloraApp(
    viewModel: PlayerViewModel,
    useMaterialYou: Boolean,
    onMaterialYouToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    // 3 tabs: Library (0), Now Playing (1), Settings (2)
    val pagerState = rememberPagerState(pageCount = { 3 })

    // Permissions
    val permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()
    var hasPermission by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasPermission = results.values.any { it } }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(permissions) }

    // ExoPlayer — obtained directly from PlayerService companion object.
    // The service is started by the ViewModel; we poll until it is available.
    val exoPlayer by produceState<ExoPlayer?>(initialValue = null) {
        while (value == null) {
            value = com.velora.app.service.PlayerService.player
            if (value == null) kotlinx.coroutines.delay(100)
        }
    }

    // ZIP import: show dialog to choose playlist option
    var pendingZipUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())
        { uri -> if (uri != null) pendingZipUri = uri }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())
        { uri -> uri?.let { viewModel.importLyricsFile(it) } }

    // Auto-jump to Now Playing when track starts
    LaunchedEffect(state.currentItem) {
        if (state.currentItem != null) scope.launch { pagerState.animateScrollToPage(1) }
    }

    val selectedTab = pagerState.currentPage
    val isVideoPlayer = selectedTab == 1 && state.currentItem?.isVideo == true
    val isAudioPlayer = selectedTab == 1 && state.currentItem?.isVideo == false
    val isVideoFullscreen = isVideoPlayer && state.isLandscape
    val isAudioLandscape = isAudioPlayer && state.isLandscape

    // Video player hides bottom bar when controls are hidden
    var videoHidesBottomBar by remember { mutableStateOf(true) }

    // Audio player controls auto-hide state (shared so bottom nav can hide too)
    var audioBottomBarVisible by remember { mutableStateOf(true) }

    val onRotate: () -> Unit = {
        val landscape = !state.isLandscape
        viewModel.setLandscape(landscape)
        activity?.requestedOrientation = if (landscape)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.zipImportMessage) {
        state.zipImportMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearZipMessage() }
    }

    // ZIP import dialog — resolve the display name so it can be pre-filled
    pendingZipUri?.let { uri ->
        val zipDisplayName = remember(uri) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && idx >= 0) it.getString(idx)?.substringBeforeLast('.') ?: "" else ""
            }.takeIf { !it.isNullOrBlank() }
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "ZIP Import"
        }
        ZipImportDialog(
            playlists = state.playlists.filter { !it.isFavourites },
            defaultName = zipDisplayName,
            onCreateNew = { customName ->
                viewModel.importZip(uri, playlistMode = ZipPlaylistMode.NEW, customPlaylistName = customName)
                pendingZipUri = null
            },
            onAddToExisting = { playlistId ->
                viewModel.importZip(uri, playlistMode = ZipPlaylistMode.EXISTING, existingPlaylistId = playlistId)
                pendingZipUri = null
            },
            onNoPlaylist = { viewModel.importZip(uri, playlistMode = ZipPlaylistMode.NONE); pendingZipUri = null },
            onDismiss = { pendingZipUri = null }
        )
    }

    // Playlist selector dialog — shown when next/prev item belongs to >1 playlist
    val pendingChoice = state.pendingPlaylistChoice
    if (pendingChoice != null) {
        PlaylistChoiceDialog(
            playlists = pendingChoice,
            onChoose = { pl -> viewModel.continueInPlaylist(pl) },
            onDismiss = { viewModel.dismissPlaylistChoice() }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalPager(
                state = pagerState,
                // Video player manages its own insets; other pages need space for the pill nav
                modifier = Modifier.fillMaxSize()
                    .padding(bottom = when {
                        isVideoFullscreen || isAudioLandscape -> 0.dp
                        isVideoPlayer -> 0.dp   // video handles its own nav padding
                        else -> 56.dp
                    }),
                userScrollEnabled = !isVideoFullscreen && !isAudioLandscape
            ) { page ->
                when (page) {
                    0 -> if (hasPermission) {
                        MediaListScreen(
                            state = state,
                            filteredItems = viewModel.filteredList(),
                            onItemClick = {
                                viewModel.playItem(it)
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onFilterChange = viewModel::setFilter,
                            onImportZip = { zipPicker.launch("application/zip") },
                            onCreatePlaylist = viewModel::createPlaylist,
                            onPlayPlaylist = {
                                viewModel.playPlaylist(it)
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onAddToFavourites = viewModel::toggleFavourite,
                            isFavourite = viewModel::isFavourite,
                            onMergePlaylists = { ids, name, keep -> viewModel.mergePlaylists(ids, name, keep) },
                            onAddPlaylistToFavorites = viewModel::addPlaylistToFavorites,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onRenamePlaylist = viewModel::renamePlaylist,
                            onAddToPlaylist = viewModel::addToPlaylist,
                            onHideItem = viewModel::toggleHideItem,
                            onRemoveImported = viewModel::removeImportedMedia,
                            onMultiDeleteMedia = viewModel::multiDeleteImportedMedia,
                            onMultiDeletePlaylists = viewModel::multiDeletePlaylists,
                            nonFavPlaylists = state.playlists.filter { !it.isFavourites }
                        )
                    } else PermissionPrompt { permissionLauncher.launch(permissions) }

                    1 -> when {
                        state.currentItem == null -> NothingPlayingPlaceholder {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                        state.currentItem!!.isVideo -> VideoPlayerScreen(
                            state = state, player = exoPlayer,
                            onPlayPause = viewModel::togglePlayPause,
                            onSkipForward = viewModel::seekForward,
                            onSkipBackward = viewModel::seekBackward,
                            onSeek = viewModel::seekTo,
                            onSkipSecondsChange = viewModel::setSkipSeconds,
                            onImportLyrics = { lyricsPicker.launch("*/*") },
                            onRemoveLyrics = viewModel::removeLyrics,
                            onFavouriteToggle = { state.currentItem?.let { viewModel.toggleFavourite(it) } },
                            onShuffleToggle = viewModel::toggleShuffle,
                            onQueueToggle = viewModel::toggleQueueMode,
                            onSpeedChange = viewModel::setPlaybackSpeed,
                            onPlayNext = viewModel::playNext,
                            onPlayPrev = viewModel::playPrev,
                            isFavourite = state.currentItem?.let { viewModel.isFavourite(it) } ?: false,
                            onRotate = onRotate,
                            onHideBottomBar = { videoHidesBottomBar = it }
                        )
                        else -> AudioPlayerScreen(
                            state = state,
                            onPlayPause = viewModel::togglePlayPause,
                            onSkipForward = viewModel::seekForward,
                            onSkipBackward = viewModel::seekBackward,
                            onSeek = viewModel::seekTo,
                            onSkipSecondsChange = viewModel::setSkipSeconds,
                            onImportLyrics = { lyricsPicker.launch("*/*") },
                            onRemoveLyrics = viewModel::removeLyrics,
                            onFavouriteToggle = { state.currentItem?.let { viewModel.toggleFavourite(it) } },
                            onShuffleToggle = viewModel::toggleShuffle,
                            onQueueToggle = viewModel::toggleQueueMode,
                            onSpeedChange = viewModel::setPlaybackSpeed,
                            onPlayNext = viewModel::playNext,
                            onPlayPrev = viewModel::playPrev,
                            isFavourite = state.currentItem?.let { viewModel.isFavourite(it) } ?: false,
                            onRotate = onRotate
                        )
                    }

                    2 -> SettingsScreen(
                        useMaterialYou = useMaterialYou,
                        onMaterialYouToggle = onMaterialYouToggle
                    )
                }
            }

            // Mini player — sits just above the bottom nav with correct gap
            AnimatedVisibility(
                visible = selectedTab == 0 && state.currentItem != null,
                enter = slideInVertically { it } + fadeIn(),
                exit  = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 54.dp, start = 12.dp, end = 12.dp)
            ) {
                MiniPlayer(state = state, onPlayPause = viewModel::togglePlayPause,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } })
            }

            // Bottom nav — hidden for video player (always), audio landscape, video fullscreen
            // Show bottom nav everywhere except true fullscreen / landscape audio
            val showBottomNav = !isVideoFullscreen && !isAudioLandscape
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically { it } + fadeIn(),
                exit  = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LiquidBottomNav(
                    selectedTab = selectedTab,
                    onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
                    hasNowPlaying = state.currentItem != null,
                    isVideoPlayer = isVideoPlayer
                )
            }

            // Favorites toast
            AnimatedVisibility(
                visible = state.showFavouriteToast,
                enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(200)),
                exit  = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp)
            ) {
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.32f) {
                    Row(modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        val inf = rememberInfiniteTransition(label = "th")
                        val heartScale by inf.animateFloat(1f, 1.22f,
                            infiniteRepeatable(tween(500), RepeatMode.Reverse), "ths")
                        Icon(Icons.Rounded.Favorite, null,
                            modifier = Modifier.size(16.dp)
                                .graphicsLayer { scaleX = heartScale; scaleY = heartScale },
                            tint = Color(0xFFFF3B6B))
                        Text("Added to Favorites", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Version text — hidden from bottom bar per user request
            // (version is visible in Settings → About)
        }
    }
}

// ── Bottom nav: 3 tabs ─────────────────────────────────────────────────────────

/**
 * Compact floating pill nav bar — replaces the full-width tab bar.
 * Three icon buttons in a small frosted pill that floats above content.
 * Visible in all screens including video player.
 */
@Composable
private fun LiquidBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    hasNowPlaying: Boolean,
    isVideoPlayer: Boolean = false
) {
    // Wrap both gradient and pill in a column that pads for system nav
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Faint gradient behind pill
        Box(
            modifier = Modifier.fillMaxWidth().height(16.dp)
                .background(Brush.verticalGradient(
                    listOf(Color.Transparent,
                        if (isVideoPlayer) Color.Black.copy(0.55f)
                        else MaterialTheme.colorScheme.background.copy(0.92f))
                ))
        )
        // The pill + system nav insets
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isVideoPlayer) Color.Black.copy(0.45f)
                    else MaterialTheme.colorScheme.background.copy(0.92f)
                )
                .navigationBarsPadding()
                .padding(bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
        LiquidGlassSurface(
            cornerRadius = 999.dp,
            alpha = if (isVideoPlayer) 0.30f else 0.22f,
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillNavItem(
                    icon = Icons.Rounded.LibraryMusic,
                    label = "Library",
                    selected = selectedTab == 0,
                    isVideoOverlay = isVideoPlayer,
                    onClick = { onTabSelected(0) }
                )
                PillNavItem(
                    icon = if (hasNowPlaying) Icons.Rounded.MusicVideo else Icons.Rounded.PlayCircle,
                    label = "Playing",
                    selected = selectedTab == 1,
                    badge = hasNowPlaying && selectedTab != 1,
                    isVideoOverlay = isVideoPlayer,
                    onClick = { onTabSelected(1) }
                )
                PillNavItem(
                    icon = Icons.Rounded.Settings,
                    label = "Settings",
                    selected = selectedTab == 2,
                    isVideoOverlay = isVideoPlayer,
                    onClick = { onTabSelected(2) }
                )
            }
        }
        } // Box
    } // Column
}

@Composable
private fun PillNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    badge: Boolean = false,
    isVideoOverlay: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bgAlpha by animateFloatAsState(
        if (selected) if (isVideoOverlay) 0.28f else 0.22f else 0f,
        spring(stiffness = Spring.StiffnessMediumLow), label = "pillBg"
    )
    val activeColor = if (isVideoOverlay) Color.White else MaterialTheme.colorScheme.primary
    val inactiveColor = if (isVideoOverlay) Color.White.copy(0.45f) else MaterialTheme.colorScheme.onSurface.copy(0.45f)
    val iconColor = if (selected) activeColor else inactiveColor

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(activeColor.copy(alpha = bgAlpha), RoundedCornerShape(999.dp))
            .bouncePressEffect(pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box {
                Icon(icon, label, tint = iconColor, modifier = Modifier.size(18.dp))
                if (badge) {
                    Box(
                        Modifier.size(5.dp)
                            .background(if (isVideoOverlay) Color.White else MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                            .align(Alignment.TopEnd)
                    )
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(180)),
                exit  = fadeOut(tween(130)) + shrinkHorizontally(tween(130))
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = activeColor
                )
            }
        }
    }
}

// ── Mini player ────────────────────────────────────────────────────────────────

@Composable
private fun MiniPlayer(state: PlayerState, onPlayPause: () -> Unit, onClick: () -> Unit) {
    val item = state.currentItem ?: return
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.25f,
        modifier = Modifier.fillMaxWidth().liquidPressEffect(pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Thumbnail
            Box(modifier = Modifier.size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(0.4f)),
                contentAlignment = Alignment.Center) {
                val art = item.artUri
                if (art != null) {
                    AsyncImage(model = art, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                } else {
                    Icon(if (item.isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                        null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                }
            }
            // Title + artist (2 lines for title)
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface, lineHeight = 15.sp)
                if (item.artist.isNotBlank() && item.artist != "Unknown") {
                    Text(item.artist, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            // Progress + play button
            val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(44.dp).height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f))
            val playInteraction = remember { MutableInteractionSource() }
            val playPressed by playInteraction.collectIsPressedAsState()
            Box(Modifier.bouncePressEffect(playPressed)) {
                IconButton(onClick = onPlayPause, interactionSource = playInteraction) {
                    Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LiquidGlassSurface(cornerRadius = 24.dp, modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Rounded.FolderOpen, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Storage Access Needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Velora needs access to your media files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                Button(onClick = onRequest) { Text("Grant Permission") }
            }
        }
    }
}

@Composable
private fun NothingPlayingPlaceholder(onBrowse: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Rounded.PlayCircleOutline, null, Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(0.25f))
            Text("Nothing playing yet", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            TextButton(onClick = onBrowse) { Text("Browse Library") }
        }
    }
}


@Composable
private fun PlaylistChoiceDialog(
    playlists: List<com.velora.app.model.Playlist>,
    onChoose: (com.velora.app.model.Playlist) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Continue in which playlist?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "This track is in multiple playlists. Choose which one to continue in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.65f)
                )
                Spacer(Modifier.height(4.dp))
                playlists.forEach { pl ->
                    LiquidGlassSurface(cornerRadius = 14.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChoose(pl) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.PlaylistPlay, null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(pl.name, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text("${pl.itemIds.size} tracks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                            Icon(Icons.Rounded.ChevronRight, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ZipImportDialog(
    playlists: List<com.velora.app.model.Playlist>,
    defaultName: String,
    onCreateNew: (String) -> Unit,
    onAddToExisting: (Long) -> Unit,
    onNoPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    var showNameField by remember { mutableStateOf(false) }
    var playlistName by remember(defaultName) { mutableStateOf(defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import ZIP") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How would you like to import these files?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                Spacer(Modifier.height(4.dp))

                // Create new playlist
                if (!showNameField) {
                    FilledTonalButton(onClick = { showNameField = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PlaylistAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create new playlist")
                    }
                } else {
                    LiquidGlassSurface(cornerRadius = 16.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Playlist name", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = playlistName,
                                onValueChange = { playlistName = it },
                                singleLine = true,
                                placeholder = { Text(defaultName, color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.25f)
                                ),
                                trailingIcon = {
                                    if (playlistName != defaultName) {
                                        IconButton(onClick = { playlistName = defaultName }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Refresh, "Reset to zip name",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                        }
                                    }
                                }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { showNameField = false }, modifier = Modifier.weight(1f)) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = { onCreateNew(playlistName.trim().ifBlank { defaultName }) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Create")
                                }
                            }
                        }
                    }
                }

                // Add to existing
                if (playlists.isNotEmpty() && !showNameField) {
                    Text("Or add to existing:", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.padding(top = 4.dp))
                    playlists.forEach { pl ->
                        OutlinedButton(onClick = { onAddToExisting(pl.id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.PlaylistPlay, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(pl.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }

                // No playlist
                if (!showNameField) {
                    TextButton(onClick = onNoPlaylist, modifier = Modifier.fillMaxWidth()) {
                        Text("Import without playlist", color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
