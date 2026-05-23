package com.velora.app

import android.Manifest
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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.velora.app.ui.components.LiquidGlassSurface
import com.velora.app.ui.components.liquidPressEffect
import com.velora.app.ui.screens.*
import com.velora.app.ui.theme.VeloraTheme

const val APP_VERSION = "1.0.0"

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
        setContent { VeloraTheme { VeloraApp(viewModel) } }
    }
}

@UnstableApi
@Composable
fun VeloraApp(viewModel: PlayerViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.state.collectAsState()

    // Pager for swipe-to-switch tabs
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // Permissions
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    var hasPermission by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasPermission = results.values.any { it } }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(permissions) }

    // ExoPlayer
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(Unit) {
        val token = SessionToken(context,
            android.content.ComponentName(context, com.velora.app.service.PlayerService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try { exoPlayer = future.get() as? ExoPlayer } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
        onDispose { MediaController.releaseFuture(future) }
    }

    // File pickers
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())
        { uri -> uri?.let { viewModel.importZip(it) } }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent())
        { uri -> uri?.let { viewModel.importLyricsFile(it) } }

    // Auto-jump to Now Playing tab when track starts
    LaunchedEffect(state.currentItem) {
        if (state.currentItem != null) scope.launch { pagerState.animateScrollToPage(1) }
    }

    val selectedTab = pagerState.currentPage
    val isVideoPlayer = selectedTab == 1 && state.currentItem?.isVideo == true
    val showVersionText = !isVideoPlayer || !state.isPlaying
    val isVideoFullscreen = isVideoPlayer && state.isLandscape

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.zipImportMessage) {
        state.zipImportMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearZipMessage() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)) {

            // ── Swipeable page content ─────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(bottom = if (isVideoFullscreen) 0.dp else 72.dp),
                // Disable swiping while video is fullscreen or in video overlay
                userScrollEnabled = !isVideoFullscreen
            ) { page ->
                when (page) {
                    0 -> if (hasPermission) {
                        MediaListScreen(
                            state = state, filteredItems = viewModel.filteredList(),
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
                            onMergePlaylists = viewModel::mergePlaylists,
                            onDeletePlaylist = viewModel::deletePlaylist
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
                            onFavouriteToggle = { state.currentItem?.let { viewModel.toggleFavourite(it) } },
                            onShuffleToggle = viewModel::toggleShuffle,
                            onQueueToggle = viewModel::toggleQueueMode,
                            onSpeedChange = viewModel::setPlaybackSpeed,
                            isFavourite = state.currentItem?.let { viewModel.isFavourite(it) } ?: false,
                            onRotate = { viewModel.setLandscape(!state.isLandscape) }
                        )
                        else -> AudioPlayerScreen(
                            state = state,
                            onPlayPause = viewModel::togglePlayPause,
                            onSkipForward = viewModel::seekForward,
                            onSkipBackward = viewModel::seekBackward,
                            onSeek = viewModel::seekTo,
                            onSkipSecondsChange = viewModel::setSkipSeconds,
                            onImportLyrics = { lyricsPicker.launch("*/*") },
                            onFavouriteToggle = { state.currentItem?.let { viewModel.toggleFavourite(it) } },
                            onShuffleToggle = viewModel::toggleShuffle,
                            onQueueToggle = viewModel::toggleQueueMode,
                            onSpeedChange = viewModel::setPlaybackSpeed,
                            isFavourite = state.currentItem?.let { viewModel.isFavourite(it) } ?: false,
                            onRotate = { viewModel.setLandscape(!state.isLandscape) }
                        )
                    }
                }
            }

            // ── Mini player (library tab only) ────────────────────────────────
            AnimatedVisibility(
                visible = selectedTab == 0 && state.currentItem != null,
                enter = slideInVertically { it } + fadeIn(),
                exit  = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp, start = 12.dp, end = 12.dp)
            ) {
                MiniPlayer(state = state, onPlayPause = viewModel::togglePlayPause,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } })
            }

            // ── Bottom nav ────────────────────────────────────────────────────
            AnimatedVisibility(visible = !isVideoFullscreen,
                enter = slideInVertically { it } + fadeIn(),
                exit  = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)) {
                LiquidBottomNav(
                    selectedTab = selectedTab,
                    onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
                    hasNowPlaying = state.currentItem != null
                )
            }

            // ── Favourite toast ────────────────────────────────────────────────
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
                            modifier = Modifier.size(16.dp).graphicsLayer { scaleX = heartScale; scaleY = heartScale },
                            tint = Color(0xFFFF3B6B))
                        Text("Added to Favourites", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ── App version ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showVersionText, enter = fadeIn(tween(300)), exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = if (!isVideoFullscreen) 76.dp else 6.dp).fillMaxWidth()
            ) {
                Text("Velora v$APP_VERSION", fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.28f), textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Bottom nav ────────────────────────────────────────────────────────────────

@Composable
private fun LiquidBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit, hasNowPlaying: Boolean) {
    Box(modifier = Modifier.fillMaxWidth()
        .background(Brush.verticalGradient(listOf(Color.Transparent,
            MaterialTheme.colorScheme.background.copy(0.97f))))
        .navigationBarsPadding()) {
        LiquidGlassSurface(cornerRadius = 24.dp, alpha = 0.18f,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                NavItem(Icons.Rounded.LibraryMusic, "Library", selectedTab == 0) { onTabSelected(0) }
                NavItem(
                    if (hasNowPlaying) Icons.Rounded.MusicVideo else Icons.Rounded.PlayCircle,
                    "Now Playing", selectedTab == 1, badge = hasNowPlaying
                ) { onTabSelected(1) }
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, selected: Boolean, badge: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val indicatorScale by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(stiffness = Spring.StiffnessMediumLow), label = "ind")
    val color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(0.5f)

    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 20.dp).liquidPressEffect(pressed)) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp), interactionSource = interaction) {
            Box {
                Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
                if (badge && !selected) {
                    Box(Modifier.size(7.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                        .align(Alignment.TopEnd))
                }
            }
        }
        Text(label, fontSize = 9.sp, color = color,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Box(Modifier.width(20.dp).height(2.dp)
            .graphicsLayer { scaleX = indicatorScale; alpha = indicatorScale }
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)))
    }
}

// ── Mini player ───────────────────────────────────────────────────────────────

@Composable
private fun MiniPlayer(state: PlayerState, onPlayPause: () -> Unit, onClick: () -> Unit) {
    val item = state.currentItem ?: return
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.25f,
        modifier = Modifier.fillMaxWidth().liquidPressEffect(pressed)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                Text(item.artist, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f), maxLines = 1)
            }
            val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.width(56.dp).height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f))
            Spacer(Modifier.width(10.dp))
            val playInteraction = remember { MutableInteractionSource() }
            val playPressed by playInteraction.collectIsPressedAsState()
            Box(Modifier.liquidPressEffect(playPressed)) {
                IconButton(onClick = onPlayPause, interactionSource = playInteraction) {
                    Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LiquidGlassSurface(cornerRadius = 24.dp, modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Rounded.FolderOpen, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Storage Access Needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Velora needs access to your media files.", style = MaterialTheme.typography.bodySmall,
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

fun Modifier.clickable(
    interactionSource: MutableInteractionSource,
    indication: androidx.compose.foundation.Indication?,
    onClick: () -> Unit
): Modifier = this.then(
    androidx.compose.foundation.clickable(
        interactionSource = interactionSource, indication = indication, onClick = onClick)
)
