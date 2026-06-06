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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import com.velora.app.ui.components.LiquidGlassSurface
import com.velora.app.ui.components.LocalUsePixelUi
import com.velora.app.ui.components.LocalUseFrostedBlur
import com.velora.app.ui.components.liquidPressEffect
import com.velora.app.ui.components.bouncePressEffect
import com.velora.app.ui.screens.*
import com.velora.app.model.ZipPlaylistMode
import com.velora.app.ui.theme.VeloraTheme
import kotlinx.coroutines.launch

const val APP_VERSION = "1.1.1"

// Navigation destinations — no pager, no swipe
enum class Screen { LIBRARY, PLAYER, SETTINGS }

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
            val prefs = getSharedPreferences("velora_prefs", MODE_PRIVATE)
            var useMaterialYou by remember { mutableStateOf(prefs.getBoolean("material_you", true)) }
            var usePixelUi by remember { mutableStateOf(prefs.getBoolean("pixel_ui", false)) }
            var useFrostedBlur by remember { mutableStateOf(prefs.getBoolean("frosted_blur", false)) }
            VeloraTheme(useMaterialYou = useMaterialYou) {
                VeloraApp(
                    viewModel = viewModel,
                    useMaterialYou = useMaterialYou,
                    onMaterialYouToggle = { enabled ->
                        useMaterialYou = enabled
                        prefs.edit().putBoolean("material_you", enabled).apply()
                    },
                    usePixelUi = usePixelUi,
                    onPixelUiToggle = { enabled ->
                        usePixelUi = enabled
                        prefs.edit().putBoolean("pixel_ui", enabled).apply()
                    },
                    useFrostedBlur = useFrostedBlur,
                    onFrostedBlurToggle = { enabled ->
                        useFrostedBlur = enabled
                        prefs.edit().putBoolean("frosted_blur", enabled).apply()
                    }
                )
            }
        }
    }
}

@UnstableApi
@Composable
fun VeloraApp(
    viewModel: PlayerViewModel,
    useMaterialYou: Boolean,
    onMaterialYouToggle: (Boolean) -> Unit,
    usePixelUi: Boolean = false,
    onPixelUiToggle: (Boolean) -> Unit = {},
    useFrostedBlur: Boolean = false,
    onFrostedBlurToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Single screen state — no pager, no swiping
    var currentScreen by remember { mutableStateOf(Screen.LIBRARY) }
    var showSettings by remember { mutableStateOf(false) }

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

    val exoPlayer by produceState<ExoPlayer?>(initialValue = null) {
        while (value == null) {
            value = com.velora.app.service.PlayerService.player
            if (value == null) kotlinx.coroutines.delay(100)
        }
    }

    var pendingZipUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())
        { uri -> if (uri != null) pendingZipUri = uri }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())
        { uri -> uri?.let { viewModel.importLyricsFile(it) } }

    // Auto-jump to player when track starts (expand from bar)
    LaunchedEffect(state.currentItem) {
        if (state.currentItem != null) currentScreen = Screen.PLAYER
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.zipImportMessage) {
        state.zipImportMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearZipMessage() }
    }

    val onRotate: () -> Unit = {
        val landscape = !state.isLandscape
        viewModel.setLandscape(landscape)
        activity?.requestedOrientation = if (landscape)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

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

    val pendingChoice = state.pendingPlaylistChoice
    if (pendingChoice != null) {
        PlaylistChoiceDialog(
            playlists = pendingChoice,
            onChoose = { pl -> viewModel.continueInPlaylist(pl) },
            onDismiss = { viewModel.dismissPlaylistChoice() }
        )
    }

    val isVideoPlayer = currentScreen == Screen.PLAYER && state.currentItem?.isVideo == true
    val isAudioPlayer = currentScreen == Screen.PLAYER && state.currentItem?.isVideo == false
    val isVideoFullscreen = isVideoPlayer && state.isLandscape
    val isAudioLandscape = isAudioPlayer && state.isLandscape

    CompositionLocalProvider(
        LocalUsePixelUi provides usePixelUi,
        LocalUseFrostedBlur provides (useFrostedBlur && !usePixelUi)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { _ ->
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
            ) {
                // ── Main content ──────────────────────────────────────────────
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        when {
                            // Library -> Player: slide up
                            initialState == Screen.LIBRARY && targetState == Screen.PLAYER ->
                                slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it } +
                                    fadeIn(tween(280)) togetherWith
                                slideOutVertically(tween(280)) { -it / 4 } + fadeOut(tween(200))
                            // Player -> Library: slide down
                            initialState == Screen.PLAYER && targetState == Screen.LIBRARY ->
                                slideInVertically(tween(320, easing = FastOutSlowInEasing)) { -it / 4 } +
                                    fadeIn(tween(220)) togetherWith
                                slideOutVertically(tween(380, easing = FastOutSlowInEasing)) { it } +
                                    fadeOut(tween(280))
                            else ->
                                fadeIn(tween(280)) togetherWith fadeOut(tween(220))
                        }
                    },
                    label = "screenTransition",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                when (screen) {
                    Screen.LIBRARY -> {
                        if (hasPermission) {
                            MediaListScreen(
                                state = state,
                                filteredItems = viewModel.filteredList(),
                                onItemClick = {
                                    viewModel.playItem(it)
                                    currentScreen = Screen.PLAYER
                                },
                                onFilterChange = viewModel::setFilter,
                                onImportZip = { zipPicker.launch("application/zip") },
                                onCreatePlaylist = viewModel::createPlaylist,
                                onPlayPlaylist = {
                                    viewModel.playPlaylist(it)
                                    currentScreen = Screen.PLAYER
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
                                nonFavPlaylists = state.playlists.filter { !it.isFavourites },
                                onSettingsClick = { showSettings = true }
                            )
                        } else {
                            PermissionPrompt { permissionLauncher.launch(permissions) }
                        }

                        // Mini player for audio — floats above the library filter bar (bottom)
                        AnimatedVisibility(
                            visible = state.currentItem != null && state.currentItem?.isVideo == false,
                            enter = slideInVertically { it } + fadeIn(),
                            exit  = slideOutVertically { it } + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 132.dp, start = 12.dp, end = 12.dp)
                        ) {
                            MiniPlayer(
                                state = state,
                                onPlayPause = viewModel::togglePlayPause,
                                onClose = viewModel::stopPlayback,
                                onClick = { currentScreen = Screen.PLAYER }
                            )
                        }
                    }

                    Screen.PLAYER -> {
                        when {
                            state.currentItem == null -> {
                                NothingPlayingPlaceholder { currentScreen = Screen.LIBRARY }
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
                                onHideBottomBar = {},
                                onBack = { currentScreen = Screen.LIBRARY }
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
                                onRotate = onRotate,
                                onClose = { viewModel.stopPlayback(); currentScreen = Screen.LIBRARY },
                                onBack = { currentScreen = Screen.LIBRARY }
                            )
                        }
                    }

                    Screen.SETTINGS -> {
                        SettingsScreen(
                            useMaterialYou = useMaterialYou,
                            onMaterialYouToggle = onMaterialYouToggle,
                            usePixelUi = usePixelUi,
                            onPixelUiToggle = onPixelUiToggle,
                            useFrostedBlur = useFrostedBlur,
                            onFrostedBlurToggle = onFrostedBlurToggle,
                            onBack = { showSettings = false }
                        )
                    }
                }
                } // end AnimatedContent

                // ── Settings overlay ──────────────────────────────────────────
                AnimatedVisibility(
                    visible = showSettings,
                    enter = slideInVertically(
                        animationSpec = tween(340, easing = FastOutSlowInEasing)
                    ) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) { it } + fadeOut(tween(180)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Single Box — no blocking clickable that could swallow close-button taps
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        SettingsScreen(
                            useMaterialYou = useMaterialYou,
                            onMaterialYouToggle = onMaterialYouToggle,
                            usePixelUi = usePixelUi,
                            onPixelUiToggle = onPixelUiToggle,
                            useFrostedBlur = useFrostedBlur,
                            onFrostedBlurToggle = onFrostedBlurToggle,
                            onBack = { showSettings = false }
                        )
                    }
                }

                // ── Favorites toast ───────────────────────────────────────────
                AnimatedVisibility(
                    visible = state.showFavouriteToast,
                    enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(200)),
                    exit  = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                ) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.32f) {
                        Row(modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            val inf = rememberInfiniteTransition(label = "th")
                            val heartScale by inf.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.22f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 500),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "ths"
                            )
                            Icon(Icons.Rounded.Favorite, null,
                                modifier = Modifier.size(16.dp)
                                    .graphicsLayer { scaleX = heartScale; scaleY = heartScale },
                                tint = Color(0xFFFF3B6B))
                            Text("Added to Favorites", fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ── Mini player (audio only) ───────────────────────────────────────────────────

@Composable
private fun MiniPlayer(state: PlayerState, onPlayPause: () -> Unit, onClose: () -> Unit, onClick: () -> Unit) {
    val item = state.currentItem ?: return
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.25f,
        modifier = Modifier.fillMaxWidth().liquidPressEffect(pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                }
            }
            Column(Modifier.weight(1f).padding(end = 4.dp)) {
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
            val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(36.dp).height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f))
            val playInteraction = remember { MutableInteractionSource() }
            val playPressed by playInteraction.collectIsPressedAsState()
            Box(Modifier.bouncePressEffect(playPressed)) {
                IconButton(onClick = onPlayPause, interactionSource = playInteraction,
                    modifier = Modifier.size(40.dp)) {
                    Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            // X close button
            val closeInteraction = remember { MutableInteractionSource() }
            val closePressed by closeInteraction.collectIsPressedAsState()
            Box(Modifier.bouncePressEffect(closePressed)) {
                IconButton(
                    onClick = onClose,
                    interactionSource = closeInteraction,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Close, "Close player",
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────────

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

                if (playlists.isNotEmpty() && !showNameField) {
                    Text("Or add to existing:", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.padding(top = 4.dp))
                    playlists.forEach { pl ->
                        OutlinedButton(onClick = { onAddToExisting(pl.id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.PlaylistPlay, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(pl.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

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
