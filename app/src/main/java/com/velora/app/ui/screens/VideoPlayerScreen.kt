package com.velora.app.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.velora.app.PlayerState
import com.velora.app.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    state: PlayerState,
    player: ExoPlayer?,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipSecondsChange: (Int) -> Unit,
    onImportLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.currentItem ?: return
    val hasLyrics = state.lyrics.isNotEmpty()
    var controlsVisible by remember { mutableStateOf(true) }

    // Auto-hide controls 3s after playback starts
    LaunchedEffect(state.isPlaying, controlsVisible) {
        if (state.isPlaying && controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        // ── Video surface ─────────────────────────────────────────────────────
        // We always show the PlayerView regardless of whether player is null
        // so it's ready when the player connects
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { playerView ->
                // Wire in the player whenever it becomes available
                playerView.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Controls overlay ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )
                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )

                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 20.dp, vertical = 48.dp)
                )

                // Center transport
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    VideoControlButton(Icons.Rounded.Replay, "Skip backward", onSkipBackward)

                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.28f,
                        modifier = Modifier.size(80.dp)) {
                        IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(targetState = state.isPlaying, label = "vpp") { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    VideoControlButton(Icons.Rounded.Forward10, "Skip forward", onSkipForward)
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Import lyrics button
                    TextButton(onClick = onImportLyrics) {
                        Icon(Icons.Rounded.Lyrics, null,
                            modifier = Modifier.size(14.dp), tint = Color.White.copy(0.7f))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (hasLyrics) "Change lyrics" else "Import lyrics",
                            fontSize = 11.sp, color = Color.White.copy(0.7f)
                        )
                    }

                    // Skip pill
                    SkipSecondsPillVideo(current = state.skipSeconds, onChange = onSkipSecondsChange)

                    // iOS 26 scrubber
                    VideoScrubber(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        onSeek = onSeek,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Lyrics — always visible, transparent
        if (hasLyrics) {
            LyricsOverlay(
                lyrics = state.lyrics,
                activeIndex = state.activeLyricIndex,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(bottom = if (controlsVisible) 150.dp else 24.dp)
            )
        }
    }
}

@Composable
private fun VideoControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.2f, modifier = Modifier.size(60.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, label, modifier = Modifier.size(32.dp), tint = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun SkipSecondsPillVideo(current: Int, onChange: (Int) -> Unit) {
    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.22f, modifier = Modifier.wrapContentWidth()) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
            listOf(5, 10).forEach { secs ->
                val isSelected = current == secs
                TextButton(
                    onClick = { onChange(secs) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("${secs}s",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp)
                }
            }
        }
    }
}
