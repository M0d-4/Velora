package com.velora.app.ui.screens

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.velora.app.PlayerState
import com.velora.app.model.MediaItem
import com.velora.app.model.Playlist
import com.velora.app.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun AudioPlayerScreen(
    state: PlayerState,
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
    modifier: Modifier = Modifier
) {
    val item = state.currentItem ?: return
    AnimatedContent(
        targetState = state.isLandscape,
        transitionSpec = {
            fadeIn(tween(320)) + slideInHorizontally { if (targetState) it else -it } togetherWith
            fadeOut(tween(220)) + slideOutHorizontally { if (targetState) -it else it }
        }, label = "orientation"
    ) { landscape ->
        if (landscape) {
            AudioPlayerLandscape(
                state = state, item = item,
                onPlayPause = onPlayPause, onSkipForward = onSkipForward,
                onSkipBackward = onSkipBackward, onSeek = onSeek,
                onSkipSecondsChange = onSkipSecondsChange,
                onFavouriteToggle = onFavouriteToggle, onShuffleToggle = onShuffleToggle,
                onQueueToggle = onQueueToggle, onSpeedChange = onSpeedChange,
                onPlayNext = onPlayNext, onPlayPrev = onPlayPrev,
                isFavourite = isFavourite, onRotate = onRotate, modifier = modifier
            )
        } else {
            AudioPlayerPortrait(
                state = state, item = item,
                onPlayPause = onPlayPause, onSkipForward = onSkipForward,
                onSkipBackward = onSkipBackward, onSeek = onSeek,
                onSkipSecondsChange = onSkipSecondsChange,
                onImportLyrics = onImportLyrics, onRemoveLyrics = onRemoveLyrics,
                onFavouriteToggle = onFavouriteToggle, onShuffleToggle = onShuffleToggle,
                onQueueToggle = onQueueToggle, onSpeedChange = onSpeedChange,
                onPlayNext = onPlayNext, onPlayPrev = onPlayPrev,
                isFavourite = isFavourite, onRotate = onRotate, modifier = modifier
            )
        }
    }
}

// ── Animated background ───────────────────────────────────────────────────────
@Composable
internal fun AnimatedBackground() {
    val inf = rememberInfiniteTransition(label = "bg")
    val o1x by inf.animateFloat(0.15f, 0.75f, infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "o1x")
    val o1y by inf.animateFloat(0.10f, 0.55f, infiniteRepeatable(tween(7500, easing = LinearEasing), RepeatMode.Reverse), "o1y")
    val o2x by inf.animateFloat(0.80f, 0.30f, infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "o2x")
    val o2y by inf.animateFloat(0.70f, 0.20f, infiniteRepeatable(tween(8200, easing = LinearEasing), RepeatMode.Reverse), "o2y")
    val primary   = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(primary, Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(o1x * 1000f, o1y * 1000f), radius = 600f)))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(secondary, Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(o2x * 1000f, o2y * 1000f), radius = 500f)))
    }
}

// ── Animated album art ────────────────────────────────────────────────────────
@Composable
internal fun AnimatedAlbumArt(item: MediaItem, isPlaying: Boolean, sizeDp: Int = 220) {
    val inf = rememberInfiniteTransition(label = "art")
    val floatY by inf.animateFloat(0f, if (isPlaying) -8f else 0f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), "float")
    val pulse by inf.animateFloat(1f, if (isPlaying) 1.03f else 1f,
        infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), "pulse")
    LiquidGlassSurface(cornerRadius = 28.dp, alpha = 0.2f,
        modifier = Modifier.size(sizeDp.dp).graphicsLayer { translationY = floatY; scaleX = pulse; scaleY = pulse }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val artUri = item.artUri
            if (artUri != null) {
                AsyncImage(model = artUri, contentDescription = "Album art", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)))
            } else {
                Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size((sizeDp * 0.36).dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Portrait ──────────────────────────────────────────────────────────────────
@Composable
private fun AudioPlayerPortrait(
    state: PlayerState, item: MediaItem,
    onPlayPause: () -> Unit, onSkipForward: () -> Unit, onSkipBackward: () -> Unit,
    onSeek: (Long) -> Unit, onSkipSecondsChange: (Int) -> Unit,
    onImportLyrics: () -> Unit, onRemoveLyrics: () -> Unit,
    onFavouriteToggle: () -> Unit, onShuffleToggle: () -> Unit, onQueueToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit, onPlayNext: () -> Unit, onPlayPrev: () -> Unit,
    isFavourite: Boolean, onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasLyrics = state.lyrics.isNotEmpty()
    // Next/Prev enabled only when there is a proper playlist queue (not just shuffle-all)
    val isInPlaylist = state.isQueueMode && state.queue.size > 1 && state.currentPlaylistId != null

    // Speed expanded state — fully hoisted
    var speedExpanded by remember { mutableStateOf(false) }

    var prevItemId by remember { mutableLongStateOf(item.id) }
    var enterFromRight by remember { mutableStateOf(true) }
    if (prevItemId != item.id) { enterFromRight = item.id > prevItemId; prevItemId = item.id }

    Box(
        modifier = modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    // Any tap outside the speed bar collapses it
                    if (speedExpanded) speedExpanded = false
                }
            }
    ) {
        AnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Top row: speed picker (fully controlled) + rotate
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                PlaybackSpeedControl(
                    currentSpeed = state.playbackSpeed,
                    onSpeedChange = onSpeedChange,
                    expanded = speedExpanded,
                    onExpandedChange = { speedExpanded = it }
                )
                AnimatedVisibility(
                    visible = !speedExpanded,
                    enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                    exit  = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f)
                ) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onRotate) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            Spacer(Modifier.weight(0.5f))

            // Album art
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedAlbumArt(item = item, isPlaying = state.isPlaying, sizeDp = 200)
            }

            Spacer(Modifier.height(20.dp))

            // Title + artist
            AnimatedContent(targetState = item,
                transitionSpec = {
                    (slideInHorizontally { if (enterFromRight) it / 2 else -it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally { if (enterFromRight) -it / 2 else it / 2 } + fadeOut(tween(200)))
                }, label = "title") { t ->
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(t.artist, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center, maxLines = 1)
                }
            }

            Spacer(Modifier.weight(0.5f))

            // Scrubber (skip zones built into the track)
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                MediaScrubber(
                    positionMs     = state.positionMs,
                    durationMs     = state.durationMs,
                    onSeek         = onSeek,
                    skipSeconds    = state.skipSeconds,
                    onSkipBackward = onSkipBackward,
                    onSkipForward  = onSkipForward,
                    modifier       = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(12.dp))

            // Prev | Controls | Next
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()) {
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                    modifier = Modifier.size(40.dp)) {
                    IconButton(onClick = { if (isInPlaylist) onPlayPrev() },
                        modifier = Modifier.fillMaxSize(), enabled = isInPlaylist) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", modifier = Modifier.size(22.dp),
                            tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.75f)
                                   else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                    }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PlayerControls(
                        isPlaying = state.isPlaying, skipSeconds = state.skipSeconds,
                        isFavourite = isFavourite, isShuffle = state.isShuffle, isQueueMode = state.isQueueMode,
                        onPlayPause = onPlayPause, onSkipForward = onSkipForward, onSkipBackward = onSkipBackward,
                        onSkipSecondsChange = onSkipSecondsChange, onFavouriteToggle = onFavouriteToggle,
                        onShuffleToggle = onShuffleToggle, onQueueToggle = onQueueToggle
                    )
                }
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                    modifier = Modifier.size(40.dp)) {
                    IconButton(onClick = { if (isInPlaylist) onPlayNext() },
                        modifier = Modifier.fillMaxSize(), enabled = isInPlaylist) {
                        Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(22.dp),
                            tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.75f)
                                   else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))

            // Import / Remove lyrics
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onImportLyrics) {
                    Icon(Icons.Rounded.Lyrics, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasLyrics) "Change lyrics" else "Import lyrics (.lrc / .srt)",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
                if (hasLyrics) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.14f) {
                        IconButton(onClick = onRemoveLyrics, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Close, "Remove lyrics", modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error.copy(0.75f))
                        }
                    }
                }
            }

            // Lyrics — UP direction, always visible in portrait when present
            if (hasLyrics) {
                LyricsOverlay(
                    lyrics = state.lyrics,
                    activeIndex = state.activeLyricIndex,
                    slideDirection = LyricsSlideDirection.UP,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 8.dp)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Landscape ─────────────────────────────────────────────────────────────────
@Composable
private fun AudioPlayerLandscape(
    state: PlayerState, item: MediaItem,
    onPlayPause: () -> Unit, onSkipForward: () -> Unit, onSkipBackward: () -> Unit,
    onSeek: (Long) -> Unit, onSkipSecondsChange: (Int) -> Unit,
    onFavouriteToggle: () -> Unit, onShuffleToggle: () -> Unit, onQueueToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit, onPlayNext: () -> Unit, onPlayPrev: () -> Unit,
    isFavourite: Boolean, onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasLyrics = state.lyrics.isNotEmpty()
    val isInPlaylist = state.isQueueMode && state.queue.size > 1 && state.currentPlaylistId != null
    var speedExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()
        .pointerInput(Unit) { detectTapGestures { if (speedExpanded) speedExpanded = false } }) {
        AnimatedBackground()
        Row(modifier = Modifier.fillMaxSize().systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {

            // LEFT: art + title + artist + lyrics (UP direction)
            Column(modifier = Modifier.weight(0.46f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top) {
                Spacer(Modifier.height(4.dp))
                AnimatedAlbumArt(item = item, isPlaying = state.isPlaying, sizeDp = 160)
                Spacer(Modifier.height(10.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(item.artist, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center, maxLines = 1)
                // Lyrics in landscape audio: UP direction (bottom-to-top)
                if (hasLyrics) {
                    Spacer(Modifier.height(10.dp))
                    LyricsOverlay(
                        lyrics = state.lyrics,
                        activeIndex = state.activeLyricIndex,
                        singleLine = false,
                        slideDirection = LyricsSlideDirection.UP,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }

            // RIGHT: speed + rotate | prev/controls/next | scrubber
            Column(modifier = Modifier.weight(0.54f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    PlaybackSpeedControl(
                        currentSpeed = state.playbackSpeed,
                        onSpeedChange = onSpeedChange,
                        expanded = speedExpanded,
                        onExpandedChange = { speedExpanded = it }
                    )
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onRotate) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                        modifier = Modifier.size(36.dp)) {
                        IconButton(onClick = { if (isInPlaylist) onPlayPrev() },
                            modifier = Modifier.fillMaxSize(), enabled = isInPlaylist) {
                            Icon(Icons.Rounded.SkipPrevious, "Previous", modifier = Modifier.size(20.dp),
                                tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.75f)
                                       else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                        }
                    }
                    PlayerControls(
                        isPlaying = state.isPlaying, skipSeconds = state.skipSeconds,
                        isFavourite = isFavourite, isShuffle = state.isShuffle, isQueueMode = state.isQueueMode,
                        onPlayPause = onPlayPause, onSkipForward = onSkipForward, onSkipBackward = onSkipBackward,
                        onSkipSecondsChange = onSkipSecondsChange, onFavouriteToggle = onFavouriteToggle,
                        onShuffleToggle = onShuffleToggle, onQueueToggle = onQueueToggle
                    )
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                        modifier = Modifier.size(36.dp)) {
                        IconButton(onClick = { if (isInPlaylist) onPlayNext() },
                            modifier = Modifier.fillMaxSize(), enabled = isInPlaylist) {
                            Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(20.dp),
                                tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.75f)
                                       else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                LiquidGlassSurface(cornerRadius = 16.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                    MediaScrubber(
                        positionMs     = state.positionMs,
                        durationMs     = state.durationMs,
                        onSeek         = onSeek,
                        skipSeconds    = state.skipSeconds,
                        onSkipBackward = onSkipBackward,
                        onSkipForward  = onSkipForward,
                        modifier       = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

internal fun formatMs(ms: Long): String {
    val total = ms / 1000; val m = total / 60; val s = total % 60
    return "%d:%02d".format(m, s)
}
