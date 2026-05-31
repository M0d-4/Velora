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
    isFavourite: Boolean, onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasLyrics = state.lyrics.isNotEmpty()
    // Next/Prev only when playing from an actual named playlist
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

            // Speed + rotate row
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                PlaybackSpeedControl(
                    currentSpeed = state.playbackSpeed,
                    onSpeedChange = onSpeedChange,
                    expanded = speedExpanded,
                    onExpandedChange = { speedExpanded = it }
                )
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

            Spacer(Modifier.weight(0.3f))

            // Album art
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedAlbumArt(item, state.isPlaying, 200)
            }
            Spacer(Modifier.height(20.dp))

            // Title + artist
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

            Spacer(Modifier.weight(0.3f))

            // ── Prev | Controls | Next ────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()) {
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                    modifier = Modifier.size(40.dp)) {
                    IconButton(onClick = { if (isInPlaylist) onPlayPrev() },
                        modifier = Modifier.fillMaxSize(), enabled = isInPlaylist) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(22.dp),
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
                        Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(22.dp),
                            tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.75f)
                                   else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // ── Import lyrics button inline with the SkipAndHeart pill ────
            // [Import lyrics]  [5s | 10s | ♥]
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Import / change / remove lyrics compact button
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.13f) {
                    Row(
                        modifier = Modifier
                            .pointerInput(Unit) { detectTapGestures { onImportLyrics() } }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.Lyrics, null, Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                        Text(
                            if (hasLyrics) "Change" else "Import lyrics",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.primary.copy(0.7f)
                        )
                        if (hasLyrics) {
                            Spacer(Modifier.width(2.dp))
                            Box(Modifier.size(15.dp).clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.error.copy(0.12f)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, "Remove", Modifier.size(10.dp)
                                    .pointerInput(Unit) { detectTapGestures { onRemoveLyrics() } },
                                    tint = MaterialTheme.colorScheme.error.copy(0.8f))
                            }
                        }
                    }
                }
                // Spacer pushes SkipAndHeartPill to the right
                Spacer(Modifier.weight(1f))
                // Skip seconds + heart pill (from PlayerControls)
                SkipAndHeartPill(
                    skipSeconds = state.skipSeconds,
                    isFavourite = isFavourite,
                    onSkipSecondsChange = onSkipSecondsChange,
                    onFavouriteToggle = onFavouriteToggle
                )
            }
            Spacer(Modifier.height(8.dp))

            // ── Time bar ─────────────────────────────────────────────────
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                MediaScrubber(
                    positionMs     = state.positionMs,
                    durationMs     = state.durationMs,
                    onSeek         = onSeek,
                    skipSeconds    = state.skipSeconds,
                    onSkipBackward = onSkipBackward,
                    onSkipForward  = onSkipForward,
                    modifier       = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // ── Lyrics: UP direction — always below time bar ─────────────
            if (hasLyrics) {
                Spacer(Modifier.height(8.dp))
                LyricsOverlay(
                    lyrics = state.lyrics,
                    activeIndex = state.activeLyricIndex,
                    slideDirection = LyricsSlideDirection.UP,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 100.dp)
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

            // LEFT col: art + title + lyrics
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
                    LyricsOverlay(
                        lyrics = state.lyrics, activeIndex = state.activeLyricIndex,
                        singleLine = false, slideDirection = LyricsSlideDirection.UP,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                } else Spacer(Modifier.weight(1f))
            }

            // RIGHT col: speed/rotate | controls | scrubber
            Column(modifier = Modifier.weight(0.56f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    PlaybackSpeedControl(state.playbackSpeed, onSpeedChange, speedExpanded, { speedExpanded = it })
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onRotate) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))

                // Prev / Controls / Next
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                        modifier = Modifier.size(34.dp)) {
                        IconButton({ if (isInPlaylist) onPlayPrev() }, Modifier.fillMaxSize(), isInPlaylist) {
                            Icon(Icons.Rounded.SkipPrevious, "Prev", Modifier.size(18.dp),
                                tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.75f)
                                       else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                        }
                    }
                    PlayerControls(
                        isPlaying = state.isPlaying,
                        skipSeconds = state.skipSeconds,
                        isFavourite = isFavourite,
                        isShuffle = state.isShuffle,
                        isQueueMode = state.isQueueMode,
                        onPlayPause = onPlayPause,
                        onSkipForward = onSkipForward,
                        onSkipBackward = onSkipBackward,
                        onSkipSecondsChange = onSkipSecondsChange,
                        onFavouriteToggle = onFavouriteToggle,
                        onShuffleToggle = onShuffleToggle,
                        onQueueToggle = onQueueToggle
                    )
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (isInPlaylist) 0.15f else 0.06f,
                        modifier = Modifier.size(34.dp)) {
                        IconButton({ if (isInPlaylist) onPlayNext() }, Modifier.fillMaxSize(), isInPlaylist) {
                            Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(18.dp),
                                tint = if (isInPlaylist) MaterialTheme.colorScheme.onSurface.copy(0.75f)
                                       else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))

                LiquidGlassSurface(cornerRadius = 14.dp, alpha = 0.12f, Modifier.fillMaxWidth()) {
                    MediaScrubber(
                        positionMs = state.positionMs, durationMs = state.durationMs,
                        onSeek = onSeek, skipSeconds = state.skipSeconds,
                        onSkipBackward = onSkipBackward, onSkipForward = onSkipForward,
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
