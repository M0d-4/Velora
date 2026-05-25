package com.velora.app.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    onFavouriteToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    onQueueToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    isFavourite: Boolean,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.currentItem ?: return
    val hasLyrics = state.lyrics.isNotEmpty()
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastInteractionTime, state.isPlaying) {
        if (state.isPlaying && controlsVisible) {
            val elapsed = System.currentTimeMillis() - lastInteractionTime
            val remaining = 3000L - elapsed
            if (remaining > 0) kotlinx.coroutines.delay(remaining)
            if (System.currentTimeMillis() - lastInteractionTime >= 3000L) controlsVisible = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)
        .pointerInput(Unit) {
            detectTapGestures {
                controlsVisible = !controlsVisible
                lastInteractionTime = System.currentTimeMillis()
            }
        }) {
        AndroidView(factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(android.graphics.Color.BLACK)
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            }
        }, update = { it.player = player }, modifier = Modifier.fillMaxSize())

        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { lastInteractionTime = System.currentTimeMillis() } }) {
                // Gradients
                Box(Modifier.fillMaxWidth().height(120.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.65f), Color.Transparent)))
                    .align(Alignment.TopCenter))
                Box(Modifier.fillMaxWidth().height(280.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
                    .align(Alignment.BottomCenter))

                // Title + rotate
                Row(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
                    .statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = Color.White,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.22f) {
                        IconButton(onClick = { onRotate(); lastInteractionTime = System.currentTimeMillis() }) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", modifier = Modifier.size(20.dp), tint = Color.White)
                        }
                    }
                }

                // Centre transport — FastRewind + Play + FastForward (no numbers)
                Row(modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    VideoControlButton(Icons.Rounded.FastRewind, "Rewind") { onSkipBackward(); lastInteractionTime = System.currentTimeMillis() }
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.28f, modifier = Modifier.size(80.dp)) {
                        IconButton(onClick = { onPlayPause(); lastInteractionTime = System.currentTimeMillis() }, modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(targetState = state.isPlaying, label = "vpp") { playing ->
                                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null,
                                    modifier = Modifier.size(44.dp), tint = Color.White)
                            }
                        }
                    }
                    VideoControlButton(Icons.Rounded.FastForward, "Forward") { onSkipForward(); lastInteractionTime = System.currentTimeMillis() }
                }

                // Bottom controls
                Column(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onImportLyrics) {
                            Icon(Icons.Rounded.Lyrics, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(0.7f))
                            Spacer(Modifier.width(4.dp))
                            Text(if (hasLyrics) "Change lyrics" else "Import lyrics", fontSize = 11.sp, color = Color.White.copy(0.7f))
                        }
                        LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (state.isShuffle) 0.35f else 0.18f, modifier = Modifier.size(34.dp)) {
                            IconButton(onClick = { onShuffleToggle(); lastInteractionTime = System.currentTimeMillis() }, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.Shuffle, "Shuffle", modifier = Modifier.size(15.dp), tint = if (state.isShuffle) Color.White else Color.White.copy(0.5f))
                            }
                        }
                        LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (state.isQueueMode) 0.35f else 0.18f, modifier = Modifier.size(34.dp)) {
                            IconButton(onClick = { onQueueToggle(); lastInteractionTime = System.currentTimeMillis() }, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.QueueMusic, "Queue", modifier = Modifier.size(15.dp), tint = if (state.isQueueMode) Color.White else Color.White.copy(0.5f))
                            }
                        }
                        HeartButton(isFavourite = isFavourite, onToggle = { onFavouriteToggle(); lastInteractionTime = System.currentTimeMillis() })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlaybackSpeedControl(currentSpeed = state.playbackSpeed,
                            onSpeedChange = { onSpeedChange(it); lastInteractionTime = System.currentTimeMillis() },
                            isVideoOverlay = true)
                        SkipSecondsPillVideo(current = state.skipSeconds, onChange = { onSkipSecondsChange(it); lastInteractionTime = System.currentTimeMillis() })
                    }
                    MediaScrubber(positionMs = state.positionMs, durationMs = state.durationMs,
                        onSeek = { onSeek(it); lastInteractionTime = System.currentTimeMillis() },
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (hasLyrics) {
            LyricsOverlay(lyrics = state.lyrics, activeIndex = state.activeLyricIndex,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(160.dp)
                    .padding(bottom = if (controlsVisible) 150.dp else 24.dp))
        }
    }
}

@Composable
private fun VideoControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.2f, modifier = Modifier.size(60.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, label, modifier = Modifier.size(32.dp), tint = Color.White.copy(0.9f))
        }
    }
}

@Composable
private fun SkipSecondsPillVideo(current: Int, onChange: (Int) -> Unit) {
    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.22f, modifier = Modifier.wrapContentWidth()) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            listOf(5, 10).forEach { secs ->
                val isSelected = current == secs
                Box(modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .then(if (isSelected) Modifier.background(Color.White.copy(0.2f), RoundedCornerShape(999.dp)) else Modifier)
                    .pointerInput(Unit) { detectTapGestures { onChange(secs) } }
                    .padding(horizontal = 14.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text("${secs}s", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp, color = if (isSelected) Color.White else Color.White.copy(0.5f))
                }
            }
        }
    }
}
