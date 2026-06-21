package com.velora.app.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.IntOffset
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
import com.velora.app.ui.theme.VeloraMotion
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
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val item = state.currentItem ?: return
    val hasLyrics = state.lyrics.isNotEmpty()
    val canSkip = state.isQueueMode && state.currentPlaylistId != null && state.queue.size > 1
    val isPlaylistContext = state.currentPlaylistId != null

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

    // Fill the entire screen including behind system bars
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video surface — fills entire Box including behind system bars
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

        // Tap-to-toggle-controls layer — sits behind the actual controls
        // so button clicks reach their handlers without being swallowed here
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(controlsVisible, speedExpanded) {
                    detectTapGestures {
                        when {
                            speedExpanded -> { speedExpanded = false; interact() }
                            controlsVisible -> controlsVisible = false
                            else -> { controlsVisible = true; interact() }
                        }
                    }
                }
        )

        // Lyrics always visible regardless of controls — rendered outside AnimatedVisibility
        if (state.lyrics.isNotEmpty()) {
            if (state.isLandscape) {
                // Landscape: single line, slides left-to-right, no background
                LyricsOverlay(
                    lyrics = state.lyrics,
                    activeIndex = state.activeLyricIndex,
                    slideDirection = LyricsSlideDirection.LEFT_TO_RIGHT,
                    singleLine = true,
                    textColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .offset(y = (-80).dp)
                        .padding(horizontal = 20.dp)
                )
            } else {
                // Portrait: single line, slides left-to-right, no background
                LyricsOverlay(
                    lyrics = state.lyrics,
                    activeIndex = state.activeLyricIndex,
                    slideDirection = LyricsSlideDirection.LEFT_TO_RIGHT,
                    singleLine = true,
                    textColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .offset(y = (-160).dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(VeloraMotion.effectsDefault()), exit = fadeOut(VeloraMotion.effectsDefault()),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isLandscape) {
                VideoControlsLandscape(
                    state = state, item = item, hasLyrics = hasLyrics,
                    canSkip = canSkip, speedExpanded = speedExpanded,
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
                    onQueueToggle = { if (isPlaylistContext) { onQueueToggle(); interact() } },
                    onSpeedChange = { onSpeedChange(it); interact() },
                    onPlayNext = { onPlayNext(); interact() },
                    onPlayPrev = { onPlayPrev(); interact() },
                    isFavourite = isFavourite,
                    onRotate = { onRotate(); interact() }
                )
            } else {
                VideoControlsPortrait(
                    state = state, item = item, hasLyrics = hasLyrics,
                    canSkip = canSkip, speedExpanded = speedExpanded,
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
                    onQueueToggle = { if (isPlaylistContext) { onQueueToggle(); interact() } },
                    onSpeedChange = { onSpeedChange(it); interact() },
                    onPlayNext = { onPlayNext(); interact() },
                    onPlayPrev = { onPlayPrev(); interact() },
                    isFavourite = isFavourite,
                    onRotate = { onRotate(); interact() },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun VideoControlsPortrait(
    state: PlayerState,
    item: com.velora.app.model.MediaItem,
    hasLyrics: Boolean,
    canSkip: Boolean,
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
    onRotate: () -> Unit,
    onBack: () -> Unit = {}
) {
    val useWhiteIcons = com.velora.app.ui.components.LocalUseWhiteIcons.current
    val iconColor    = if (useWhiteIcons) Color.White else MaterialTheme.colorScheme.primary
    val iconColorDim = if (useWhiteIcons) Color.White.copy(0.5f) else MaterialTheme.colorScheme.primary.copy(0.45f)
    Box(Modifier.fillMaxSize()) {
        // Top scrim
        Box(Modifier.fillMaxWidth().height(120.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.72f), Color.Transparent)))
            .align(Alignment.TopCenter))

        // Bottom scrim — tall enough to cover controls + nav bar
        Box(Modifier.fillMaxWidth().height(260.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.92f))))
            .align(Alignment.BottomCenter))

        // Title row with back + rotate
        Row(modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.22f) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, "Back", Modifier.size(18.dp), tint = iconColor)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium,
                color = iconColor, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.22f) {
                IconButton(onClick = onRotate) {
                    Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(20.dp), tint = iconColor)
                }
            }
        }

        // Centre transport
        Row(modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp,
                alpha = if (canSkip) 0.22f else 0.08f, modifier = Modifier.size(52.dp)) {
                IconButton(onPlayPrev, Modifier.fillMaxSize(), canSkip) {
                    Icon(Icons.Rounded.SkipPrevious, "Prev", Modifier.size(28.dp),
                        tint = if (canSkip) iconColor else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                }
            }
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.32f,
                modifier = Modifier
                    .size(84.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onPlayPause() }) }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AnimatedContent(state.isPlaying, label = "vpp") { playing ->
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null, Modifier.size(46.dp), tint = iconColor)
                    }
                }
            }
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp,
                alpha = if (canSkip) 0.22f else 0.08f, modifier = Modifier.size(52.dp)) {
                IconButton(onPlayNext, Modifier.fillMaxSize(), canSkip) {
                    Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(28.dp),
                        tint = if (canSkip) iconColor else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                }
            }
        }

        // Bottom controls — pinned above nav bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Scrubber
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
            // Speed + skip seconds row — no fixed height so text never clips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaybackSpeedControl(
                    currentSpeed     = state.playbackSpeed,
                    onSpeedChange    = onSpeedChange,
                    expanded         = speedExpanded,
                    onExpandedChange = onSpeedExpandedChange,
                    isVideoOverlay   = true
                )
                if (!speedExpanded) {
                    SkipSecondsPillVideo(state.skipSeconds, onSkipSecondsChange)
                }
            }
            // Lyrics + shuffle/queue/fav
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onImportLyrics,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Icon(Icons.Rounded.Lyrics, null, Modifier.size(14.dp), tint = iconColor.copy(0.75f))
                    Spacer(Modifier.width(4.dp))
                    Text(if (hasLyrics) "Change lyrics" else "Import lyrics",
                        fontSize = 11.sp, color = iconColor.copy(0.72f))
                }
                if (hasLyrics) {
                    LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.2f, modifier = Modifier.size(30.dp)) {
                        IconButton(onRemoveLyrics, Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.Close, "Remove", Modifier.size(14.dp), tint = Color(0xFFFF6B6B))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp,
                    alpha = if (state.isShuffle) 0.35f else 0.18f, modifier = Modifier.size(34.dp)) {
                    IconButton(onShuffleToggle, Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(15.dp),
                            tint = if (state.isShuffle) iconColor else iconColorDim)
                    }
                }
                LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp,
                    alpha = if (state.isQueueMode) 0.35f else 0.18f, modifier = Modifier.size(34.dp)) {
                    IconButton(onQueueToggle, Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.QueueMusic, "Queue", Modifier.size(15.dp),
                            tint = if (state.isQueueMode) iconColor else iconColorDim)
                    }
                }
                HeartButton(isFavourite, onFavouriteToggle)
            }
        }
    }
}

@Composable
private fun VideoControlsLandscape(
    state: PlayerState,
    item: com.velora.app.model.MediaItem,
    hasLyrics: Boolean,
    canSkip: Boolean,
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
    val useWhiteIcons = com.velora.app.ui.components.LocalUseWhiteIcons.current
    val iconColor    = if (useWhiteIcons) Color.White else MaterialTheme.colorScheme.primary
    val iconColorDim = if (useWhiteIcons) Color.White.copy(0.5f) else MaterialTheme.colorScheme.primary.copy(0.45f)
    Box(Modifier.fillMaxSize()) {
        // Scrims
        Box(Modifier.fillMaxWidth().height(90.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.65f), Color.Transparent)))
            .align(Alignment.TopCenter))
        Box(Modifier.fillMaxWidth().height(200.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.88f))))
            .align(Alignment.BottomCenter))

        // Title + rotate
        Row(modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, style = MaterialTheme.typography.titleSmall,
                color = iconColor, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.22f) {
                IconButton(onClick = onRotate) {
                    Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(18.dp), tint = iconColor)
                }
            }
        }

        // Centre transport
        Row(modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp,
                alpha = if (canSkip) 0.22f else 0.08f, modifier = Modifier.size(44.dp)) {
                IconButton(onPlayPrev, Modifier.fillMaxSize(), canSkip) {
                    Icon(Icons.Rounded.SkipPrevious, "Prev", Modifier.size(24.dp),
                        tint = if (canSkip) iconColor else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                }
            }
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.32f,
                modifier = Modifier
                    .size(72.dp)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onPlayPause() }) }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AnimatedContent(state.isPlaying, label = "vpp_ls") { playing ->
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null, Modifier.size(38.dp), tint = iconColor)
                    }
                }
            }
            LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp,
                alpha = if (canSkip) 0.22f else 0.08f, modifier = Modifier.size(44.dp)) {
                IconButton(onPlayNext, Modifier.fillMaxSize(), canSkip) {
                    Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(24.dp),
                        tint = if (canSkip) iconColor else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Scrubber
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
            // Speed + skip + actions row — no fixed height so text never clips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaybackSpeedControl(
                    currentSpeed     = state.playbackSpeed,
                    onSpeedChange    = onSpeedChange,
                    expanded         = speedExpanded,
                    onExpandedChange = onSpeedExpandedChange,
                    isVideoOverlay   = true
                )
                if (!speedExpanded) {
                    SkipSecondsPillVideo(state.skipSeconds, onSkipSecondsChange)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onImportLyrics,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Icon(Icons.Rounded.Lyrics, null, Modifier.size(13.dp), tint = iconColor.copy(0.75f))
                    Spacer(Modifier.width(3.dp))
                    Text(if (hasLyrics) "Change lyrics" else "Import lyrics",
                        fontSize = 10.sp, color = iconColor.copy(0.72f))
                }
                if (hasLyrics) {
                    LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.2f, modifier = Modifier.size(28.dp)) {
                        IconButton(onRemoveLyrics, Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.Close, "Remove", Modifier.size(13.dp), tint = Color(0xFFFF6B6B))
                        }
                    }
                }
                LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp,
                    alpha = if (state.isShuffle) 0.35f else 0.18f, modifier = Modifier.size(30.dp)) {
                    IconButton(onShuffleToggle, Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(14.dp),
                            tint = if (state.isShuffle) iconColor else iconColorDim)
                    }
                }
                LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp,
                    alpha = if (state.isQueueMode) 0.35f else 0.18f, modifier = Modifier.size(30.dp)) {
                    IconButton(onQueueToggle, Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.QueueMusic, "Queue", Modifier.size(14.dp),
                            tint = if (state.isQueueMode) iconColor else iconColorDim)
                    }
                }
                HeartButton(isFavourite, onFavouriteToggle)
            }
        }
    }
}

@Composable
private fun SkipSecondsPillVideo(current: Int, onChange: (Int) -> Unit) {
    val useWhiteIcons = com.velora.app.ui.components.LocalUseWhiteIcons.current
    val iconColor    = if (useWhiteIcons) Color.White else MaterialTheme.colorScheme.primary
    val iconColorDim = if (useWhiteIcons) Color.White.copy(0.5f) else MaterialTheme.colorScheme.primary.copy(0.45f)
    LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.22f, modifier = Modifier.wrapContentWidth()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            listOf(5, 10).forEach { secs ->
                val isSelected = current == secs
                val bgAlpha by animateFloatAsState(if (isSelected) 0.2f else 0f, VeloraMotion.effectsDefault(), label = "skipbg")
                val pillShape = pixelAwareShape(999.dp)
                Box(modifier = Modifier.clip(pillShape)
                    .background(Color.White.copy(bgAlpha), pillShape)
                    .pointerInput(Unit) { detectTapGestures { onChange(secs) } }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center) {
                    Text("${secs}s", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp, color = if (isSelected) iconColor else iconColorDim)
                }
            }
        }
    }
}
