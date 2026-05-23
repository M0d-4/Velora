package com.velora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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

    AnimatedContent(
        targetState = state.isLandscape,
        transitionSpec = {
            fadeIn(tween(320)) + slideInHorizontally { if (targetState) it else -it } togetherWith
            fadeOut(tween(220)) + slideOutHorizontally { if (targetState) -it else it }
        },
        label = "orientation"
    ) { landscape ->
        if (landscape) {
            AudioPlayerLandscape(
                state = state, item = item, hasLyrics = hasLyrics,
                onPlayPause = onPlayPause, onSkipForward = onSkipForward,
                onSkipBackward = onSkipBackward, onSeek = onSeek,
                onSkipSecondsChange = onSkipSecondsChange,
                onFavouriteToggle = onFavouriteToggle, onShuffleToggle = onShuffleToggle,
                onQueueToggle = onQueueToggle, onSpeedChange = onSpeedChange,
                isFavourite = isFavourite, onRotate = onRotate, modifier = modifier
            )
        } else {
            AudioPlayerPortrait(
                state = state, item = item, hasLyrics = hasLyrics,
                onPlayPause = onPlayPause, onSkipForward = onSkipForward,
                onSkipBackward = onSkipBackward, onSeek = onSeek,
                onSkipSecondsChange = onSkipSecondsChange, onImportLyrics = onImportLyrics,
                onFavouriteToggle = onFavouriteToggle, onShuffleToggle = onShuffleToggle,
                onQueueToggle = onQueueToggle, onSpeedChange = onSpeedChange,
                isFavourite = isFavourite, onRotate = onRotate, modifier = modifier
            )
        }
    }
}

// ── Animated background orbs ──────────────────────────────────────────────────
@Composable
internal fun AnimatedBackground() {
    val inf = rememberInfiniteTransition(label = "bg")
    val orb1x by inf.animateFloat(0.15f, 0.75f,
        infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "o1x")
    val orb1y by inf.animateFloat(0.10f, 0.55f,
        infiniteRepeatable(tween(7500, easing = LinearEasing), RepeatMode.Reverse), "o1y")
    val orb2x by inf.animateFloat(0.80f, 0.30f,
        infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "o2x")
    val orb2y by inf.animateFloat(0.70f, 0.20f,
        infiniteRepeatable(tween(8200, easing = LinearEasing), RepeatMode.Reverse), "o2y")

    val primary   = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(primary, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(orb1x, orb1y), radius = 600f)))
        Box(Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(secondary, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(orb2x, orb2y), radius = 500f)))
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
                AsyncImage(model = artUri, contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)))
            } else {
                Icon(Icons.Rounded.MusicNote, null,
                    modifier = Modifier.size((sizeDp * 0.36).dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Portrait ──────────────────────────────────────────────────────────────────
@Composable
private fun AudioPlayerPortrait(
    state: PlayerState, item: MediaItem, hasLyrics: Boolean,
    onPlayPause: () -> Unit, onSkipForward: () -> Unit, onSkipBackward: () -> Unit,
    onSeek: (Long) -> Unit, onSkipSecondsChange: (Int) -> Unit, onImportLyrics: () -> Unit,
    onFavouriteToggle: () -> Unit, onShuffleToggle: () -> Unit, onQueueToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit, isFavourite: Boolean, onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var prevItemId by remember { mutableLongStateOf(item.id) }
    var enterFromRight by remember { mutableStateOf(true) }
    if (prevItemId != item.id) { enterFromRight = item.id > prevItemId; prevItemId = item.id }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedBackground()
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(40.dp))
            // Top row: rotate + speed
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                PlaybackSpeedControl(
                    currentSpeed = state.playbackSpeed,
                    onSpeedChange = onSpeedChange
                )
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                    IconButton(onClick = onRotate) {
                        Icon(Icons.Rounded.ScreenRotation, "Rotate", modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            AnimatedAlbumArt(item = item, isPlaying = state.isPlaying, sizeDp = 220)
            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = item,
                transitionSpec = {
                    val e = slideInHorizontally { if (enterFromRight) it / 2 else -it / 2 } + fadeIn(tween(300))
                    val x = slideOutHorizontally { if (enterFromRight) -it / 2 else it / 2 } + fadeOut(tween(200))
                    e togetherWith x
                }, label = "trackTitle"
            ) { trackItem ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(trackItem.title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(trackItem.artist, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center, maxLines = 1)
                }
            }
            Spacer(Modifier.height(18.dp))
            // Waveform scrubber card
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    val progress = if (state.durationMs > 0)
                        state.positionMs.toFloat() / state.durationMs else 0f
                    AudioWaveformBar(amplitudes = state.waveformAmplitudes, progress = progress,
                        onSeek = { p -> onSeek((p * state.durationMs).toLong()) },
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatMs(state.positionMs), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(formatMs(state.durationMs), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PlayerControls(isPlaying = state.isPlaying, skipSeconds = state.skipSeconds,
                isFavourite = isFavourite, isShuffle = state.isShuffle, isQueueMode = state.isQueueMode,
                onPlayPause = onPlayPause, onSkipForward = onSkipForward, onSkipBackward = onSkipBackward,
                onSkipSecondsChange = onSkipSecondsChange, onFavouriteToggle = onFavouriteToggle,
                onShuffleToggle = onShuffleToggle, onQueueToggle = onQueueToggle)
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onImportLyrics) {
                Icon(Icons.Rounded.Lyrics, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                Spacer(Modifier.width(6.dp))
                Text(if (hasLyrics) "Change lyrics file" else "Import lyrics (.lrc / .srt)",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
            if (hasLyrics) {
                LyricsOverlay(lyrics = state.lyrics, activeIndex = state.activeLyricIndex,
                    modifier = Modifier.fillMaxWidth().weight(1f))
            } else Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Landscape ─────────────────────────────────────────────────────────────────
@Composable
private fun AudioPlayerLandscape(
    state: PlayerState, item: MediaItem, hasLyrics: Boolean,
    onPlayPause: () -> Unit, onSkipForward: () -> Unit, onSkipBackward: () -> Unit,
    onSeek: (Long) -> Unit, onSkipSecondsChange: (Int) -> Unit,
    onFavouriteToggle: () -> Unit, onShuffleToggle: () -> Unit, onQueueToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit, isFavourite: Boolean, onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedBackground()
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // LEFT
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                AnimatedAlbumArt(item = item, isPlaying = state.isPlaying, sizeDp = 148)
                Spacer(Modifier.height(12.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(3.dp))
                Text(item.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center, maxLines = 1)
            }
            // RIGHT
            Column(modifier = Modifier.weight(0.6f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    PlaybackSpeedControl(currentSpeed = state.playbackSpeed, onSpeedChange = onSpeedChange)
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onRotate) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
                PlayerControls(isPlaying = state.isPlaying, skipSeconds = state.skipSeconds,
                    isFavourite = isFavourite, isShuffle = state.isShuffle, isQueueMode = state.isQueueMode,
                    onPlayPause = onPlayPause, onSkipForward = onSkipForward, onSkipBackward = onSkipBackward,
                    onSkipSecondsChange = onSkipSecondsChange, onFavouriteToggle = onFavouriteToggle,
                    onShuffleToggle = onShuffleToggle, onQueueToggle = onQueueToggle)
                LiquidGlassSurface(cornerRadius = 16.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(item.title, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Spacer(Modifier.height(6.dp))
                        val progress = if (state.durationMs > 0)
                            state.positionMs.toFloat() / state.durationMs else 0f
                        AudioWaveformBar(amplitudes = state.waveformAmplitudes, progress = progress,
                            onSeek = { p -> onSeek((p * state.durationMs).toLong()) },
                            modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatMs(state.positionMs), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text(formatMs(state.durationMs), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

internal fun formatMs(ms: Long): String {
    val total = ms / 1000; val m = total / 60; val s = total % 60
    return "%d:%02d".format(m, s)
}
