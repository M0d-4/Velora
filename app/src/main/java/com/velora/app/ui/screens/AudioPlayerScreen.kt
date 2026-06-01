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
import com.velora.app.ui.components.*

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
            AudioPlayerLandscape(state, item, onPlayPause, onSkipForward, onSkipBackward,
                onSeek, onSkipSecondsChange, onFavouriteToggle, onShuffleToggle,
                onQueueToggle, onSpeedChange, onPlayNext, onPlayPrev,
                isFavourite, onRotate, modifier)
        } else {
            AudioPlayerPortrait(state, item, onPlayPause, onSkipForward, onSkipBackward,
                onSeek, onSkipSecondsChange, onImportLyrics, onRemoveLyrics,
                onFavouriteToggle, onShuffleToggle, onQueueToggle, onSpeedChange,
                onPlayNext, onPlayPrev, isFavourite, onRotate, modifier)
        }
    }
}

@Composable
internal fun AnimatedBackground() {
    val inf = rememberInfiniteTransition(label = "bg")
    val o1x by inf.animateFloat(150f, 750f, infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "o1x")
    val o1y by inf.animateFloat(100f, 550f, infiniteRepeatable(tween(7500, easing = LinearEasing), RepeatMode.Reverse), "o1y")
    val o2x by inf.animateFloat(800f, 300f, infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "o2x")
    val o2y by inf.animateFloat(700f, 200f, infiniteRepeatable(tween(8200, easing = LinearEasing), RepeatMode.Reverse), "o2y")
    val primary   = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(primary, Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(o1x, o1y), radius = 600f)))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(secondary, Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(o2x, o2y), radius = 500f)))
    }
}

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
    isFavourite: Boolean, onRotate: () -> Unit, modifier: Modifier = Modifier
) {
    val hasLyrics = state.lyrics.isNotEmpty()
    // Prev/Next only enabled when playing from a named playlist
    val isInPlaylist = state.isQueueMode && state.queue.size > 1 && state.currentPlaylistId != null

    var speedExpanded by remember { mutableStateOf(false) }
    var prevItemId by remember { mutableLongStateOf(item.id) }
    var enterFromRight by remember { mutableStateOf(true) }
    if (prevItemId != item.id) { enterFromRight = item.id > prevItemId; prevItemId = item.id }

    Box(modifier = modifier.fillMaxSize()
        .pointerInput(Unit) { detectTapGestures { if (speedExpanded) speedExpanded = false } }) {
        AnimatedBackground()
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Speed + rotate ────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                PlaybackSpeedControl(state.playbackSpeed, onSpeedChange, speedExpanded) { speedExpanded = it }
                AnimatedVisibility(!speedExpanded,
                    enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                    exit  = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f)) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onRotate) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Album art (lifted, near top) ──────────────────────────────
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedAlbumArt(item, state.isPlaying, 200)
            }
            Spacer(Modifier.height(16.dp))

            // ── Title + artist ────────────────────────────────────────────
            AnimatedContent(targetState = item,
                transitionSpec = {
                    (slideInHorizontally { if (enterFromRight) it / 2 else -it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally { if (enterFromRight) -it / 2 else it / 2 } + fadeOut(tween(200)))
                }, label = "title") { t ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(t.artist, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        textAlign = TextAlign.Center, maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))

            // ── Lyrics directly under title (scrolling UP) ────────────────
            if (hasLyrics) {
                LyricsOverlay(
                    lyrics = state.lyrics,
                    activeIndex = state.activeLyricIndex,
                    slideDirection = LyricsSlideDirection.UP,
                    modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 80.dp)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))

            // ── Shuffle + Queue ───────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AnimatedIconButton(onClick = onShuffleToggle, active = state.isShuffle,
                    cornerRadius = 20.dp, size = 42.dp) {
                    Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(18.dp),
                        tint = if (state.isShuffle) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
                LiquidGlassSurface(cornerRadius = 20.dp,
                    alpha = if (state.isQueueMode) 0.32f else 0.12f,
                    modifier = Modifier.height(42.dp).wrapContentWidth()) {
                    Row(modifier = Modifier
                        .pointerInput(Unit) { detectTapGestures { onQueueToggle() } }
                        .padding(horizontal = 14.dp).fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Rounded.QueueMusic, null, Modifier.size(16.dp),
                            tint = if (state.isQueueMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                        Text(if (state.isQueueMode) "Queue On" else "Play Next", fontSize = 12.sp,
                            color = if (state.isQueueMode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // ── Transport: Prev | Rewind | Play/Pause | Forward | Next ────
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()) {
                // Prev
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                    modifier = Modifier.size(44.dp)) {
                    IconButton(onClick = { if (isInPlaylist) onPlayPrev() },
                        modifier = Modifier.fillMaxSize(), enabled = isInPlaylist) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(24.dp),
                            tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.85f)
                                   else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                    }
                }
                // Rewind (skip backward)
                LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.15f, modifier = Modifier.size(46.dp)) {
                    IconButton(onClick = onSkipBackward, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.FastRewind, "Rewind", Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                    }
                }
                // Play/Pause (centred, largest)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AnimatedPlayPauseButton(isPlaying = state.isPlaying, onClick = onPlayPause)
                }
                // Forward (skip forward)
                LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.15f, modifier = Modifier.size(46.dp)) {
                    IconButton(onClick = onSkipForward, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Rounded.FastForward, "Forward", Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                    }
                }
                // Next
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                    modifier = Modifier.size(44.dp)) {
                    IconButton(onClick = { if (isInPlaylist) onPlayNext() },
                        modifier = Modifier.fillMaxSize(), enabled = isInPlaylist) {
                        Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(24.dp),
                            tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.85f)
                                   else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // ── Import lyrics | skip-seconds + heart ─────────────────────
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.13f) {
                    Row(modifier = Modifier
                        .pointerInput(Unit) { detectTapGestures { onImportLyrics() } }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Rounded.Lyrics, null, Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                        Text(if (hasLyrics) "Change" else "Import lyrics",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.primary.copy(0.7f))
                        if (hasLyrics) {
                            Spacer(Modifier.width(2.dp))
                            Box(modifier = Modifier.size(15.dp).clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.error.copy(0.12f))
                                .pointerInput(Unit) { detectTapGestures { onRemoveLyrics() } },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, "Remove", Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(0.8f))
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // Single SkipAndHeartPill
                SkipAndHeartPill(
                    skipSeconds = state.skipSeconds,
                    isFavourite = isFavourite,
                    onSkipSecondsChange = onSkipSecondsChange,
                    onFavouriteToggle = onFavouriteToggle
                )
            }
            Spacer(Modifier.height(8.dp))

            // ── Time bar — scrubber only (no extra skip buttons) ──────────
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                MediaScrubber(
                    positionMs    = state.positionMs,
                    durationMs    = state.durationMs,
                    onSeek        = onSeek,
                    skipSeconds   = state.skipSeconds,
                    // No skip callbacks → no flanking buttons appear
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )
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
    isFavourite: Boolean, onRotate: () -> Unit, modifier: Modifier = Modifier
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

            // Left: art + title + lyrics
            Column(modifier = Modifier.weight(0.44f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                AnimatedAlbumArt(item, state.isPlaying, 140)
                Spacer(Modifier.height(10.dp))
                Text(item.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(3.dp))
                Text(item.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                    textAlign = TextAlign.Center, maxLines = 1)
                if (hasLyrics) {
                    Spacer(Modifier.height(8.dp))
                    LyricsOverlay(state.lyrics, state.activeLyricIndex,
                        singleLine = false, slideDirection = LyricsSlideDirection.UP,
                        modifier = Modifier.fillMaxWidth().weight(1f))
                } else Spacer(Modifier.weight(1f))
            }

            // Right: speed/rotate | controls | scrubber (at bottom)
            Column(modifier = Modifier.weight(0.56f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    PlaybackSpeedControl(state.playbackSpeed, onSpeedChange, speedExpanded) { speedExpanded = it }
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onRotate) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))

                // Transport: Prev | Rewind | Play | Forward | Next
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                        modifier = Modifier.size(34.dp)) {
                        IconButton({ if (isInPlaylist) onPlayPrev() }, Modifier.fillMaxSize(), isInPlaylist) {
                            Icon(Icons.Rounded.SkipPrevious, "Prev", Modifier.size(18.dp),
                                tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.85f)
                                       else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                        }
                    }
                    LiquidGlassSurface(cornerRadius = 16.dp, alpha = 0.15f, modifier = Modifier.size(36.dp)) {
                        IconButton(onClick = onSkipBackward, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.FastRewind, "Rewind", Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                        }
                    }
                    AnimatedPlayPauseButton(isPlaying = state.isPlaying, onClick = onPlayPause)
                    LiquidGlassSurface(cornerRadius = 16.dp, alpha = 0.15f, modifier = Modifier.size(36.dp)) {
                        IconButton(onClick = onSkipForward, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.FastForward, "Forward", Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.85f))
                        }
                    }
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                        modifier = Modifier.size(34.dp)) {
                        IconButton({ if (isInPlaylist) onPlayNext() }, Modifier.fillMaxSize(), isInPlaylist) {
                            Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(18.dp),
                                tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.85f)
                                       else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Scrubber — pinned to bottom, no extra skip buttons
                LiquidGlassSurface(cornerRadius = 14.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                    MediaScrubber(
                        positionMs = state.positionMs, durationMs = state.durationMs,
                        onSeek = onSeek, skipSeconds = state.skipSeconds,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
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
