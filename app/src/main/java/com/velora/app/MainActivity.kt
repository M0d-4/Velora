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
import androidx.media3.exoplayer.ExoPlayer
import com.velora.app.ui.components.LiquidGlassSurface
import com.velora.app.ui.screens.*
import com.velora.app.ui.theme.VeloraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle intent (open from file manager)
        intent?.data?.let { uri ->
            val mime = contentResolver.getType(uri) ?: ""
            viewModel.playUri(uri, mime)
        }

        setContent {
            VeloraTheme {
                VeloraApp(viewModel)
            }
        }
    }
}

@Composable
fun VeloraApp(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Library, 1=Now Playing

    // Permissions
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    else
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    var hasPermission by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(
                    androidx.compose.ui.platform.LocalContext.current,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(permissions)
    }

    // Auto-switch to Now Playing when track starts
    LaunchedEffect(state.currentItem) {
        if (state.currentItem != null) selectedTab = 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Page content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "page",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp) // nav bar height
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
                            onFilterChange = viewModel::setFilter
                        )
                    } else {
                        PermissionPrompt { permissionLauncher.launch(permissions) }
                    }
                }
                1 -> {
                    if (state.currentItem == null) {
                        NothingPlayingPlaceholder { selectedTab = 0 }
                    } else if (state.currentItem!!.isVideo) {
                        // For video we need the ExoPlayer instance — get from service
                        VideoPlayerScreen(
                            state = state,
                            player = null, // injected via PlayerService in real integration
                            onPlayPause = viewModel::togglePlayPause,
                            onSkipForward = viewModel::seekForward,
                            onSkipBackward = viewModel::seekBackward,
                            onSeek = viewModel::seekTo,
                            onSkipSecondsChange = viewModel::setSkipSeconds
                        )
                    } else {
                        AudioPlayerScreen(
                            state = state,
                            onPlayPause = viewModel::togglePlayPause,
                            onSkipForward = viewModel::seekForward,
                            onSkipBackward = viewModel::seekBackward,
                            onSeek = viewModel::seekTo,
                            onSkipSecondsChange = viewModel::setSkipSeconds
                        )
                    }
                }
            }
        }

        // Mini player (when on library tab and something playing)
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

        // Bottom Navigation
        LiquidBottomNav(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            hasNowPlaying = state.currentItem != null,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

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
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
                    )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    icon = Icons.Rounded.LibraryMusic,
                    label = "Library",
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                NavItem(
                    icon = if (hasNowPlaying) Icons.Rounded.MusicVideo else Icons.Rounded.PlayCircle,
                    label = "Now Playing",
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    badge = hasNowPlaying
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: Boolean = false
) {
    val color = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .let {
                if (selected) it else it
            }
    ) {
        IconButton(onClick = onClick) {
            Box {
                Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
                if (badge && !selected) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
        Text(label, fontSize = 10.sp, color = color, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun MiniPlayer(
    state: PlayerState,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    val item = state.currentItem ?: return
    LiquidGlassSurface(
        cornerRadius = 20.dp,
        alpha = 0.25f,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Track info
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    item.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }

            // Progress bar tiny
            val progress = if (state.durationMs > 0)
                state.positionMs.toFloat() / state.durationMs else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(60.dp).height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )

            Spacer(Modifier.width(12.dp))

            // Play/pause
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LiquidGlassSurface(
            cornerRadius = 24.dp,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Rounded.FolderOpen, null, Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("Storage Access Needed",
                    style = MaterialTheme.typography.titleMedium,
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
