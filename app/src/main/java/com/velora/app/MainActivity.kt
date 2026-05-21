package com.velora.app

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.velora.app.ui.components.LiquidGlassSurface
import com.velora.app.ui.screens.*
import com.velora.app.ui.theme.VeloraTheme

@UnstableApi
class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle VIEW intent (open from file manager)
        intent?.data?.let { uri ->
            val mime = contentResolver.getType(uri) ?: ""
            if (mime == "application/zip" || uri.path?.endsWith(".zip") == true) {
                viewModel.importZip(uri)
            } else {
                viewModel.playUri(uri, mime)
            }
        }

        setContent {
            VeloraTheme {
                VeloraApp(viewModel)
            }
        }
    }
}

@UnstableApi
@Composable
fun VeloraApp(viewModel: PlayerViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── Permissions ───────────────────────────────────────────────────────────
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    else
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    var hasPermission by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> hasPermission = results.values.any { it } }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(permissions)
    }

    // ── Obtain ExoPlayer from the running service ─────────────────────────────
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(Unit) {
        val token = SessionToken(
            context,
            android.content.ComponentName(context, com.velora.app.service.PlayerService::class.java)
        )
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                val ctrl = future.get()
                // Unwrap the underlying ExoPlayer via reflection-free cast
                // MediaController wraps the real player; we find it via service binder
                // The simplest working approach: cast Player to ExoPlayer if possible
                val player = ctrl as? ExoPlayer
                exoPlayer = player
            } catch (_: Exception) {}
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
        onDispose { MediaController.releaseFuture(future) }
    }

    // ── File pickers ──────────────────────────────────────────────────────────
    // ZIP picker
    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importZip(it) } }

    // Lyrics picker (.lrc or .srt)
    val lyricsPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importLyricsFile(it) } }

    // Auto-switch to Now Playing when track starts
    LaunchedEffect(state.currentItem) {
        if (state.currentItem != null) selectedTab = 1
    }

    // ── ZIP import snackbar ───────────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.zipImportMessage) {
        state.zipImportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearZipMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Page content ──────────────────────────────────────────────────
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                },
                label = "page",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp)
            ) { tab ->
                when (tab) {
                    0 -> {
                        if (hasPermission) {
                            MediaListScreen(
                                state = state,
                                filteredItems = viewModel.filteredList(),
                                onItemClick = {
                                    viewModel.playItem(it)
                                    selectedTab = 1
                                },
                                onFilterChange = viewModel::setFilter,
                                onImportZip = { zipPicker.launch("application/zip") }
                            )
                        } else {
                            PermissionPrompt { permissionLauncher.launch(permissions) }
                        }
                    }
                    1 -> {
                        if (state.currentItem == null) {
                            NothingPlayingPlaceholder { selectedTab = 0 }
                        } else if (state.currentItem!!.isVideo) {
                            VideoPlayerScreen(
                                state = state,
                                player = exoPlayer,
                                onPlayPause = viewModel::togglePlayPause,
                                onSkipForward = viewModel::seekForward,
                                onSkipBackward = viewModel::seekBackward,
                                onSeek = viewModel::seekTo,
                                onSkipSecondsChange = viewModel::setSkipSeconds,
                                onImportLyrics = { lyricsPicker.launch("*/*") }
                            )
                        } else {
                            AudioPlayerScreen(
                                state = state,
                                onPlayPause = viewModel::togglePlayPause,
                                onSkipForward = viewModel::seekForward,
                                onSkipBackward = viewModel::seekBackward,
                                onSeek = viewModel::seekTo,
                                onSkipSecondsChange = viewModel::setSkipSeconds,
                                onImportLyrics = { lyricsPicker.launch("*/*") }
                            )
                        }
                    }
                }
            }

            // ── Mini player ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = selectedTab == 0 && state.currentItem != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp, start = 12.dp, end = 12.dp)
            ) {
                MiniPlayer(
                    state = state,
                    onPlayPause = viewModel::togglePlayPause,
                    onClick = { selectedTab = 1 }
                )
            }

            // ── Bottom nav ────────────────────────────────────────────────────
            LiquidBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                hasNowPlaying = state.currentItem != null,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ── Bottom navigation ─────────────────────────────────────────────────────────

@Composable
private fun LiquidBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    hasNowPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
                )
            )
            .navigationBarsPadding()
    ) {
        LiquidGlassSurface(
            cornerRadius = 28.dp,
            alpha = 0.18f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
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
    label: String,
    selected: Boolean,
    badge: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)) {
        IconButton(onClick = onClick) {
            Box {
                Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
                if (badge && !selected) {
                    Box(Modifier.size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                        .align(Alignment.TopEnd))
                }
            }
        }
        Text(label, fontSize = 10.sp, color = color,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ── Mini player ───────────────────────────────────────────────────────────────

@Composable
private fun MiniPlayer(state: PlayerState, onPlayPause: () -> Unit, onClick: () -> Unit) {
    val item = state.currentItem ?: return
    LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.25f,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(item.artist, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
            }
            val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(60.dp).height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onPlayPause) {
                Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LiquidGlassSurface(cornerRadius = 24.dp, modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Rounded.FolderOpen, null, Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("Storage Access Needed", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("Velora needs access to your media files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Button(onClick = onRequest) { Text("Grant Permission") }
            }
        }
    }
}

@Composable
private fun NothingPlayingPlaceholder(onBrowse: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Rounded.PlayCircleOutline, null, Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            Text("Nothing playing yet",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            TextButton(onClick = onBrowse) { Text("Browse Library") }
        }
    }
}

// Needed for MiniPlayer clickable
fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
