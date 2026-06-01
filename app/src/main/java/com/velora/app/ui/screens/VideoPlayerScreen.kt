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

    LaunchedEffect(lastInteractionTime, controlsVisible) {
        if (controlsVisible) {
            delay(3000L)
            if (System.currentTimeMillis() - lastInteractionTime >= 3000L) {
                controlsVisible = false
                speedExpanded = false
            }
        }
    }

    fun interact() { lastInteractionTime = System.currentTimeMillis() }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
            .pointerInput(controlsVisible, speedExpanded) {
                detectTapGestures {
                    when {
                        speedExpanded -> { speedExpanded = false; interact() }
                        controlsVisible -> controlsVisible = false
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
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                }
            },
            update = { view -> if (view.player != player) view.player = player },
            modifier = Modifier.fillMaxSize()
        )

        // ── Controls overlay ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(180)), exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isLandscape) {
                VideoControlsLandscape(
                    state = state, item = item, hasLyrics = hasLyrics,
                    isInPlaylist = isInPlaylist, speedExpanded = speedExpanded,
                    onSpeedExpandedChange = { speedExpanded = it },
                    onPlayPause = { onPlayPause(); interact() },
                    onSkipForward = { onSkipForward(); interact() },
                    onSkipBackward = { onSkipBackward(); interact() },
                    onSeek = { onSeek(it); interact() },
                    onSkipSecondsChange = { onSkipSecondsChange(it); interact() },
                    onImportLyrics = { onImportLyrics(); interact() },
                    onRemoveLyrics = { onRemoveLyrics(); interact() },
                    onFavouriteToggle = { onFavouriteToggle(); interact() },
                    onShuffleToggle = { onShuffleToggle(); interact() },
                    onQueueToggle = { onQueueToggle(); interact() },
                    onSpeedChange = { onSpeedChange(it); interact() },
                    onPlayNext = { onPlayNext(); interact() },
                    onPlayPrev = { onPlayPrev(); interact() },
                    isFavourite = isFavourite,
                    onRotate = { onRotate(); interact() }
                )
            } else {
                VideoControlsPortrait(
                    state = state, item = item, hasLyrics = hasLyrics,
                    isInPlaylist = isInPlaylist, speedExpanded = speedExpanded,
                    onSpeedExpandedChange = { speedExpanded = it },
                    onPlayPause = { onPlayPause(); interact() },
                    onSkipForward = { onSkipForward(); interact() },
                    onSkipBackward = { onSkipBackward(); interact() },
                    onSeek = { onSeek(it); interact() },
                    onSkipSecondsChange = { onSkipSecondsChange(it); interact() },
                    onImportLyrics = { onImportLyrics(); interact() },
                    onRemoveLyrics = { onRemoveLyrics(); interact() },
                    onFavouriteToggle = { onFavouriteToggle(); interact() },
                    onShuffleToggle = { onShuffleToggle(); interact() },
                    onQueueToggle = { onQueueToggle(); interact() },
                    onSpeedChange = { onSpeedChange(it); interact() },
                    onPlayNext = { onPlayNext(); interact() },
                    onPlayPrev = { onPlayPrev(); interact() },
                    isFavourite = isFavourite,
                    onRotate = { onRotate(); interact() }
                )
            }
        }
    }
}

// ── Portrait controls ──────────────────────────────────────────────────────────
@Composable
private fun VideoControlsPortrait(
    state: PlayerState,
    item: com.velora.app.model.MediaItem,
    hasLyrics: Boolean,
    isInPlaylist: Boolean,
    speedExpanded: Boolean,
    onSpeedExpandedChange: (Boolean) -> Unit,
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
    onRotate: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Top scrim
        Box(Modifier.fillMaxWidth().height(120.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.72f), Color.Transparent)))
            .align(Alignment.TopCenter))

        // Bottom scrim
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.6f)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.92f))))
            .align(Alignment.BottomCenter))

        // Title + rotate
        Row(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, style = MaterialTheme.typography.titleMedium,
                color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.22f) {
                IconButton(onClick = onRotate) {
                    Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(20.dp), tint = Color.White)
                }
            }
        }

        // Lyrics — above controls, shifted up from bottom
        if (hasLyrics) {
            LyricsOverlay(
                lyrics = state.lyrics,
                activeIndex = state.activeLyricIndex,
                slideDirection = LyricsSlideDirection.LEFT_TO_RIGHT,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(60.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 200.dp)
            )
        }

        // Centre transport
        Row(modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LiquidGlassSurface(cornerRadius = 999.dp,
                alpha = if (isInPlaylist) 0.22f else 0.08f, modifier = Modifier.size(52.dp)) {
                IconButton({ if (isInPlaylist) onPlayPrev() }, Modifier.fillMaxSize(), isInPlaylist) {
                    Icon(Icons.Rounded.SkipPrevious, "Prev", Modifier.size(28.dp),
                        tint = if (isInPlaylist) Color.White.copy(0.95f) else Color.White.copy(0.25f))
                }
            }
            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.32f, modifier = Modifier.size(84.dp)) {
                IconButton(onPlayPause, Modifier.fillMaxSize()) {
                    AnimatedContent(state.isPlaying, label = "vpp") { playing ->
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null, Modifier.size(46.dp), tint = Color.White)
                    }
                }
            }
            LiquidGlassSurface(cornerRadius = 999.dp,
                alpha = if (isInPlaylist) 0.22f else 0.08f, modifier = Modifier.size(52.dp)) {
                IconButton({ if (isInPlaylist) onPlayNext() }, Modifier.fillMaxSize(), isInPlaylist) {
                    Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(28.dp),
                        tint = if (isInPlaylist) Color.White.copy(0.95f) else Color.White.copy(0.25f))
                }
            }
        }

        // Bottom controls — proper nav bar padding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaScrubber(
                positionMs     = state.positionMs,
                durationMs     = state.durationMs,
                onSeek         = onSeek,
                skipSeconds    = state.skipSeconds,
                onSkipBackward = onSkipBackward,
                onSkipForward  = onSkipForward,
                isVideoOverlay = true,
                modifier       = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PlaybackSpeedControl(
                    currentSpeed     = state.playbackSpeed,
                    onSpeedChange    = onSpeedChange,
                    expanded         = speedExpanded,
                    onExpandedChange = onSpeedExpandedChange,
                    isVideoOverlay   = true
                )
                AnimatedVisibility(!speedExpanded,
                    enter = fadeIn(tween(180)) + expandHorizontally(),
                    exit  = fadeOut(tween(150)) + shrinkHorizontally()) {
                    SkipSecondsPillVideo(state.skipSeconds, onSkipSecondsChange)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onImportLyrics,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Icon(Icons.Rounded.Lyrics, null, Modifier.size(14.dp), tint = Color.White.copy(0.7f))
                    Spacer(Modifier.width(4.dp))
                    Text(if (hasLyrics) "Change lyrics" else "Import lyrics",
                        fontSize = 11.sp, color = Color.White.copy(0.7f))
                }
                if (hasLyrics) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.2f, modifier = Modifier.size(30.dp)) {
                        IconButton(onRemoveLyrics, Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.Close, "Remove", Modifier.size(14.dp), tint = Color(0xFFFF6B6B))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                LiquidGlassSurface(cornerRadius = 999.dp,
                    alpha = if (state.isShuffle) 0.35f else 0.18f, modifier = Modifier.size(34.dp)) {
                    IconButton(onShuffleToggle, Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(15.dp),
                            tint = if (state.isShuffle) Color.White else Color.White.copy(0.5f))
                    }
                }
                LiquidGlassSurface(cornerRadius = 999.dp,
                    alpha = if (state.isQueueMode) 0.35f else 0.18f, modifier = Modifier.size(34.dp)) {
                    IconButton(onQueueToggle, Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.QueueMusic, "Queue", Modifier.size(15.dp),
                            tint = if (state.isQueueMode) Color.White else Color.White.copy(0.5f))
                    }
                }
                HeartButton(isFavourite, onFavouriteToggle)
            }
        }
    }
}

// ── Landscape controls ─────────────────────────────────────────────────────────
@Composable
private fun VideoControlsLandscape(
    state: PlayerState,
    item: com.velora.app.model.MediaItem,
    hasLyrics: Boolean,
    isInPlaylist: Boolean,
    speedExpanded: Boolean,
    onSpeedExpandedChange: (Boolean) -> Unit,
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
    onRotate: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Top scrim
        Box(Modifier.fillMaxWidth().height(100.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.65f), Color.Transparent)))
            .align(Alignment.TopCenter))

        // Bottom scrim
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.5f)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.88f))))
            .align(Alignment.BottomCenter))

        // Title row at top
        Row(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, style = MaterialTheme.typography.titleSmall,
                color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.22f) {
                IconButton(onClick = onRotate) {
                    Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(18.dp), tint = Color.White)
                }
            }
        }

        // Centre transport
        Row(modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LiquidGlassSurface(cornerRadius = 999.dp,
                alpha = if (isInPlaylist) 0.22f else 0.08f, modifier = Modifier.size(44.dp)) {
                IconButton({ if (isInPlaylist) onPlayPrev() }, Modifier.fillMaxSize(), isInPlaylist) {
                    Icon(Icons.Rounded.SkipPrevious, "Prev", Modifier.size(24.dp),
                        tint = if (isInPlaylist) Color.White.copy(0.95f) else Color.White.copy(0.25f))
                }
            }
            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.32f, modifier = Modifier.size(72.dp)) {
                IconButton(onPlayPause, Modifier.fillMaxSize()) {
                    AnimatedContent(state.isPlaying, label = "vpp_ls") { playing ->
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null, Modifier.size(38.dp), tint = Color.White)
                    }
                }
            }
            LiquidGlassSurface(cornerRadius = 999.dp,
                alpha = if (isInPlaylist) 0.22f else 0.08f, modifier = Modifier.size(44.dp)) {
                IconButton({ if (isInPlaylist) onPlayNext() }, Modifier.fillMaxSize(), isInPlaylist) {
                    Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(24.dp),
                        tint = if (isInPlaylist) Color.White.copy(0.95f) else Color.White.copy(0.25f))
                }
            }
        }

        // Lyrics if available
        if (hasLyrics) {
            LyricsOverlay(
                lyrics = state.lyrics,
                activeIndex = state.activeLyricIndex,
                slideDirection = LyricsSlideDirection.LEFT_TO_RIGHT,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 140.dp)
            )
        }

        // Bottom controls row — all in one row for landscape
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed + skip pill stacked left
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    PlaybackSpeedControl(
                        currentSpeed     = state.playbackSpeed,
                        onSpeedChange    = onSpeedChange,
                        expanded         = speedExpanded,
                        onExpandedChange = onSpeedExpandedChange,
                        isVideoOverlay   = true
                    )
                    AnimatedVisibility(!speedExpanded,
                        enter = fadeIn(tween(180)) + expandHorizontally(),
                        exit  = fadeOut(tween(150)) + shrinkHorizontally()) {
                        SkipSecondsPillVideo(state.skipSeconds, onSkipSecondsChange)
                    }
                }
            }

            // Scrubber takes remaining space
            Column(modifier = Modifier.weight(1f)) {
                MediaScrubber(
                    positionMs     = state.positionMs,
                    durationMs     = state.durationMs,
                    onSeek         = onSeek,
                    skipSeconds    = state.skipSeconds,
                    onSkipBackward = onSkipBackward,
                    onSkipForward  = onSkipForward,
                    isVideoOverlay = true,
                    modifier       = Modifier.fillMaxWidth()
                )
            }

            // Icons stacked right
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onImportLyrics,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Icon(Icons.Rounded.Lyrics, null, Modifier.size(13.dp), tint = Color.White.copy(0.7f))
                        Spacer(Modifier.width(3.dp))
                        Text(if (hasLyrics) "Change" else "Lyrics",
                            fontSize = 10.sp, color = Color.White.copy(0.7f))
                    }
                    if (hasLyrics) {
                        LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.2f, modifier = Modifier.size(26.dp)) {
                            IconButton(onRemoveLyrics, Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.Close, "Remove", Modifier.size(12.dp), tint = Color(0xFFFF6B6B))
                            }
                        }
                    }
                    LiquidGlassSurface(cornerRadius = 999.dp,
                        alpha = if (state.isShuffle) 0.35f else 0.18f, modifier = Modifier.size(30.dp)) {
                        IconButton(onShuffleToggle, Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(14.dp),
                                tint = if (state.isShuffle) Color.White else Color.White.copy(0.5f))
                        }
                    }
                    LiquidGlassSurface(cornerRadius = 999.dp,
                        alpha = if (state.isQueueMode) 0.35f else 0.18f, modifier = Modifier.size(30.dp)) {
                        IconButton(onQueueToggle, Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.QueueMusic, "Queue", Modifier.size(14.dp),
                                tint = if (state.isQueueMode) Color.White else Color.White.copy(0.5f))
                        }
                    }
                    HeartButton(isFavourite, onFavouriteToggle)
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
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(bgAlpha), RoundedCornerShape(999.dp))
                    .pointerInput(Unit) { detectTapGestures { onChange(secs) } }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center) {
                    Text("${secs}s", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp, color = if (isSelected) Color.White else Color.White.copy(0.5f))
                }
            }
        }
    }
}
