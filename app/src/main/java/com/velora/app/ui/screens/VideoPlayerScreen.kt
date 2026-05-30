package com.velora.app.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
    onRemoveLyrics: () -> Unit,
    onFavouriteToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    onQueueToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrev: () -> Unit,
    isFavourite: Boolean,
    onRotate: () -> Unit,
    onHideBottomBar: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.currentItem ?: return
    val hasLyrics = state.lyrics.isNotEmpty()
    val isInPlaylist = state.isQueueMode && state.queue.size > 1 && state.currentPlaylistId != null

    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var speedExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible) { onHideBottomBar(!controlsVisible) }
    LaunchedEffect(Unit) { onHideBottomBar(true) }

    // Auto-hide ONLY while playing; paused keeps controls visible indefinitely
    LaunchedEffect(lastInteractionTime, state.isPlaying, controlsVisible) {
        if (controlsVisible && state.isPlaying) {
            delay(3000L)
            if (System.currentTimeMillis() - lastInteractionTime >= 3000L) {
                controlsVisible = false
                speedExpanded = false
            }
        }
        // When playback pauses, always show controls
        if (!state.isPlaying) controlsVisible = true
    }

    fun interact() { lastInteractionTime = System.currentTimeMillis() }

    // Unified tap handler on the root Box
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
            .pointerInput(controlsVisible, speedExpanded) {
                detectTapGestures {
                    when {
                        speedExpanded -> { speedExpanded = false; interact() }
                        controlsVisible -> { controlsVisible = false }
                        else -> { controlsVisible = true; interact() }
                    }
                }
            }
    ) {
        // ── Video surface ──────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                }
            },
            update = { view -> if (view.player != player) view.player = player },
            modifier = Modifier.fillMaxSize()
        )

        // ── Lyrics — higher up in portrait, animates with controls ────────
        if (hasLyrics) {
            val lyricsBottomPad by animateDpAsState(
                targetValue = if (controlsVisible) 260.dp else 48.dp,
                animationSpec = tween(250), label = "lyricsPad"
            )
            LyricsOverlay(
                lyrics = state.lyrics, activeIndex = state.activeLyricIndex,
                slideDirection = LyricsSlideDirection.LEFT_TO_RIGHT,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(bottom = lyricsBottomPad)
            )
        }

        // ── Controls overlay (fade in/out, taps pass through to root box) ──
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(180)), exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            // This Box does NOT consume taps — they fall through to the root pointerInput
            Box(Modifier.fillMaxSize()) {
                // Scrims
                Box(Modifier.fillMaxWidth().height(130.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.70f), Color.Transparent)))
                    .align(Alignment.TopCenter))
                Box(Modifier.fillMaxWidth().height(300.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.88f))))
                    .align(Alignment.BottomCenter))

                // Title + rotate
                Row(
                    modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
                        .statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = Color.White,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.22f) {
                        IconButton(onClick = { onRotate(); interact() }) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(20.dp), tint = Color.White)
                        }
                    }
                }

                // Centre transport (NO rewind/forward here — those are in the scrubber)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Prev
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.20f else 0.08f,
                        modifier = Modifier.size(50.dp)) {
                        IconButton({ if (isInPlaylist) { onPlayPrev(); interact() } },
                            Modifier.fillMaxSize(), isInPlaylist) {
                            Icon(Icons.Rounded.SkipPrevious, "Prev", Modifier.size(26.dp),
                                tint = if (isInPlaylist) Color.White.copy(0.9f) else Color.White.copy(0.25f))
                        }
                    }
                    // Play/Pause (large centre button)
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.30f, modifier = Modifier.size(80.dp)) {
                        IconButton({ onPlayPause(); interact() }, Modifier.fillMaxSize()) {
                            AnimatedContent(state.isPlaying, label = "vpp") { playing ->
                                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    null, Modifier.size(44.dp), tint = Color.White)
                            }
                        }
                    }
                    // Next
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.20f else 0.08f,
                        modifier = Modifier.size(50.dp)) {
                        IconButton({ if (isInPlaylist) { onPlayNext(); interact() } },
                            Modifier.fillMaxSize(), isInPlaylist) {
                            Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(26.dp),
                                tint = if (isInPlaylist) Color.White.copy(0.9f) else Color.White.copy(0.25f))
                        }
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Scrubber WITH visible rewind/forward flanking buttons
                    MediaScrubber(
                        positionMs     = state.positionMs,
                        durationMs     = state.durationMs,
                        onSeek         = { onSeek(it); interact() },
                        skipSeconds    = state.skipSeconds,
                        onSkipBackward = { onSkipBackward(); interact() },
                        onSkipForward  = { onSkipForward(); interact() },
                        isVideoOverlay = true,
                        modifier       = Modifier.fillMaxWidth()
                    )

                    // Speed + skip-seconds row
                    Row(Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
                        PlaybackSpeedControl(
                            currentSpeed = state.playbackSpeed,
                            onSpeedChange = { onSpeedChange(it); interact() },
                            expanded = speedExpanded, onExpandedChange = { speedExpanded = it },
                            isVideoOverlay = true
                        )
                        AnimatedVisibility(!speedExpanded,
                            enter = fadeIn(tween(180)) + expandHorizontally(),
                            exit  = fadeOut(tween(150)) + shrinkHorizontally()) {
                            SkipSecondsPillVideo(state.skipSeconds) { onSkipSecondsChange(it); interact() }
                        }
                    }

                    // Lyrics + shuffle/queue/fav row
                    Row(Arrangement.spacedBy(6.dp), Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onImportLyrics(); interact() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.Lyrics, null, Modifier.size(14.dp), tint = Color.White.copy(0.7f))
                            Spacer(Modifier.width(4.dp))
                            Text(if (hasLyrics) "Change lyrics" else "Import lyrics",
                                fontSize = 11.sp, color = Color.White.copy(0.7f))
                        }
                        if (hasLyrics) {
                            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.2f, modifier = Modifier.size(30.dp)) {
                                IconButton({ onRemoveLyrics(); interact() }, Modifier.fillMaxSize()) {
                                    Icon(Icons.Rounded.Close, "Remove", Modifier.size(14.dp), tint = Color(0xFFFF6B6B))
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        LiquidGlassSurface(cornerRadius = 999.dp,
                            alpha = if (state.isShuffle) 0.35f else 0.18f, modifier = Modifier.size(34.dp)) {
                            IconButton({ onShuffleToggle(); interact() }, Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(15.dp),
                                    tint = if (state.isShuffle) Color.White else Color.White.copy(0.5f))
                            }
                        }
                        LiquidGlassSurface(cornerRadius = 999.dp,
                            alpha = if (state.isQueueMode) 0.35f else 0.18f, modifier = Modifier.size(34.dp)) {
                            IconButton({ onQueueToggle(); interact() }, Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.QueueMusic, "Queue", Modifier.size(15.dp),
                                    tint = if (state.isQueueMode) Color.White else Color.White.copy(0.5f))
                            }
                        }
                        HeartButton(isFavourite, { onFavouriteToggle(); interact() })
                    }
                }
            }
        }
    }
}

@Composable
private fun SkipSecondsPillVideo(current: Int, onChange: (Int) -> Unit) {
    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.22f, modifier = Modifier.wrapContentWidth()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            listOf(5, 10).forEach { secs ->
                val isSelected = current == secs
                val bgAlpha by animateFloatAsState(if (isSelected) 0.2f else 0f, tween(200), label = "skipbg")
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(bgAlpha), RoundedCornerShape(999.dp))
                        .pointerInput(Unit) { detectTapGestures { onChange(secs) } }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${secs}s", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp, color = if (isSelected) Color.White else Color.White.copy(0.5f))
                }
            }
        }
    }
}
