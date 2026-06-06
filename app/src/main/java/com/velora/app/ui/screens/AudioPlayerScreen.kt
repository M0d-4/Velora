package com.velora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
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
    onClose: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val item = state.currentItem ?: return
    AnimatedContent(
        targetState = state.isLandscape,
        transitionSpec = {
            fadeIn(animationSpec = tween(320)) + slideInHorizontally(animationSpec = tween(320)) { if (targetState) it else -it } togetherWith
            fadeOut(animationSpec = tween(220)) + slideOutHorizontally(animationSpec = tween(220)) { if (targetState) -it else it }
        }, label = "orientation"
    ) { landscape ->
        if (landscape) {
            AudioPlayerLandscape(state, item, onPlayPause, onSkipForward, onSkipBackward,
                onSeek, onSkipSecondsChange, onImportLyrics, onRemoveLyrics,
                onFavouriteToggle, onShuffleToggle,
                onQueueToggle, onSpeedChange, onPlayNext, onPlayPrev,
                isFavourite, onRotate, onClose, onBack, modifier)
        } else {
            AudioPlayerPortrait(state, item, onPlayPause, onSkipForward, onSkipBackward,
                onSeek, onSkipSecondsChange, onImportLyrics, onRemoveLyrics,
                onFavouriteToggle, onShuffleToggle, onQueueToggle, onSpeedChange,
                onPlayNext, onPlayPrev, isFavourite, onRotate, onClose, onBack, modifier)
        }
    }
}

// ── Cover-art palette extraction ──────────────────────────────────────────────
@Composable
fun rememberCoverArtColors(item: MediaItem): Pair<Color, Color> {
    val context = LocalContext.current
    var dominantColor by remember(item.id) { mutableStateOf<Color?>(null) }
    var secondaryColor by remember(item.id) { mutableStateOf<Color?>(null) }

    LaunchedEffect(item.id) {
        val artUri = item.artUri ?: run {
            dominantColor = null; secondaryColor = null; return@LaunchedEffect
        }
        try {
            val loader = ImageLoader(context)
            val req = ImageRequest.Builder(context).data(artUri).allowHardware(false).build()
            val result = loader.execute(req)
            val bmp = (result as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap }
            if (bmp != null) {
                val palette = Palette.Builder(bmp).maximumColorCount(8).generate()
                dominantColor  = palette.dominantSwatch?.rgb?.let { Color(it) }
                secondaryColor = (palette.vibrantSwatch ?: palette.mutedSwatch)?.rgb?.let { Color(it) }
            }
        } catch (_: Exception) {}
    }

    val fallbackPrimary   = MaterialTheme.colorScheme.primary
    val fallbackSecondary = MaterialTheme.colorScheme.secondary
    val primary   by animateColorAsState(
        targetValue = dominantColor ?: fallbackPrimary,
        animationSpec = tween(durationMillis = 800),
        label = "dom"
    )
    val secondary by animateColorAsState(
        targetValue = secondaryColor ?: fallbackSecondary,
        animationSpec = tween(durationMillis = 900),
        label = "sec"
    )
    return primary to secondary
}

@Composable
internal fun AnimatedBackground(
    dominantColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    val transition = rememberInfiniteTransition(label = "bgAnim")

    val o1x by transition.animateFloat(
        initialValue = 150f,
        targetValue = 750f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "o1x"
    )
    val o1y by transition.animateFloat(
        initialValue = 100f,
        targetValue = 550f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "o1y"
    )
    val o2x by transition.animateFloat(
        initialValue = 800f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "o2x"
    )
    val o2y by transition.animateFloat(
        initialValue = 700f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "o2y"
    )

    val c1 = dominantColor.copy(alpha = 0.30f)
    val c2 = secondaryColor.copy(alpha = 0.20f)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(c1, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(o1x, o1y),
                radius = 650f
            )
        ))
        Box(Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(c2, Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(o2x, o2y),
                radius = 520f
            )
        ))
    }
}

@Composable
internal fun AnimatedAlbumArt(item: MediaItem, isPlaying: Boolean, sizeDp: Int = 220) {
    val transition = rememberInfiniteTransition(label = "artAnim")

    val floatY by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) -8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LiquidGlassSurface(
        cornerRadius = 28.dp,
        alpha = 0.2f,
        modifier = Modifier
            .size(sizeDp.dp)
            .graphicsLayer { translationY = floatY; scaleX = pulse; scaleY = pulse }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val artUri = item.artUri
            if (artUri != null) {
                AsyncImage(
                    model = artUri, contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                )
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
    state: PlayerState, item: MediaItem,
    onPlayPause: () -> Unit, onSkipForward: () -> Unit, onSkipBackward: () -> Unit,
    onSeek: (Long) -> Unit, onSkipSecondsChange: (Int) -> Unit,
    onImportLyrics: () -> Unit, onRemoveLyrics: () -> Unit,
    onFavouriteToggle: () -> Unit, onShuffleToggle: () -> Unit, onQueueToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit, onPlayNext: () -> Unit, onPlayPrev: () -> Unit,
    isFavourite: Boolean, onRotate: () -> Unit, onClose: () -> Unit = {}, onBack: () -> Unit = {}, modifier: Modifier = Modifier
) {
    val hasLyrics = state.lyrics.isNotEmpty()
    val canSkip = state.isQueueMode && state.currentPlaylistId != null && state.queue.size > 1
    val isPlaylistContext = state.currentPlaylistId != null

    var speedExpanded by remember { mutableStateOf(false) }
    var prevItemId by remember { mutableLongStateOf(item.id) }
    var enterFromRight by remember { mutableStateOf(true) }
    if (prevItemId != item.id) { enterFromRight = item.id > prevItemId; prevItemId = item.id }

    val (dominantColor, secondaryColor) = rememberCoverArtColors(item)

    Box(modifier = modifier.fillMaxSize()
        .pointerInput(Unit) { detectTapGestures { if (speedExpanded) speedExpanded = false } }) {
        AnimatedBackground(dominantColor = dominantColor, secondaryColor = secondaryColor)
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                // Back + Close buttons grouped on left
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBackIosNew, "Back", Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                        }
                    }
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, "Close", Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !speedExpanded,
                    enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f),
                    exit  = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f)
                ) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onRotate) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedAlbumArt(item, state.isPlaying, 150)
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = item,
                transitionSpec = {
                    (slideInHorizontally(animationSpec = tween(300)) { if (enterFromRight) it / 2 else -it / 2 } + fadeIn(animationSpec = tween(300))) togetherWith
                    (slideOutHorizontally(animationSpec = tween(200)) { if (enterFromRight) -it / 2 else it / 2 } + fadeOut(animationSpec = tween(200)))
                }, label = "title"
            ) { t ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(t.artist, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        textAlign = TextAlign.Center, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (hasLyrics) {
                LyricsOverlay(lyrics = state.lyrics, activeIndex = state.activeLyricIndex,
                    slideDirection = LyricsSlideDirection.UP,
                    centerAndHighlight = state.centerLyrics,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AnimatedIconButton(onClick = onShuffleToggle, active = state.isShuffle, cornerRadius = 20.dp, size = 42.dp) {
                    Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(18.dp),
                        tint = if (state.isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
                LiquidGlassSurface(cornerRadius = 20.dp, alpha = if (state.isQueueMode) 0.32f else if (!isPlaylistContext) 0.05f else 0.12f,
                    modifier = Modifier.height(42.dp).wrapContentWidth()
                        .then(if (isPlaylistContext) Modifier.clickable { onQueueToggle() } else Modifier)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp).fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Rounded.QueueMusic, null, Modifier.size(16.dp),
                            tint = if (!isPlaylistContext) MaterialTheme.colorScheme.onSurface.copy(0.25f)
                                   else if (state.isQueueMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                        Text(if (state.isQueueMode) "Queue On" else "Play Next", fontSize = 12.sp,
                            color = if (!isPlaylistContext) MaterialTheme.colorScheme.onSurface.copy(0.25f)
                                    else if (state.isQueueMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (canSkip) 0.15f else 0.06f, modifier = Modifier.size(50.dp)) {
                    IconButton(onClick = { if (canSkip) onPlayPrev() }, modifier = Modifier.fillMaxSize(), enabled = canSkip) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(28.dp),
                            tint = if (canSkip) MaterialTheme.colorScheme.onSurface.copy(0.85f) else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                    }
                }
                Spacer(Modifier.width(16.dp))
                AnimatedPlayPauseButton(isPlaying = state.isPlaying, onClick = onPlayPause)
                Spacer(Modifier.width(16.dp))
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (canSkip) 0.15f else 0.06f, modifier = Modifier.size(50.dp)) {
                    IconButton(onClick = { if (canSkip) onPlayNext() }, modifier = Modifier.fillMaxSize(), enabled = canSkip) {
                        Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(28.dp),
                            tint = if (canSkip) MaterialTheme.colorScheme.onSurface.copy(0.85f) else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.13f) {
                    Row(modifier = Modifier.pointerInput(Unit) { detectTapGestures { onImportLyrics() } }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Rounded.Lyrics, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                        Text(if (hasLyrics) "Change" else "Import lyrics", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary.copy(0.7f))
                        if (hasLyrics) {
                            Spacer(Modifier.width(2.dp))
                            Box(modifier = Modifier.size(15.dp).clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.error.copy(0.12f))
                                .pointerInput(Unit) { detectTapGestures { onRemoveLyrics() } },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, "Remove", Modifier.size(10.dp), tint = MaterialTheme.colorScheme.error.copy(0.8f))
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                SkipAndHeartPill(skipSeconds = state.skipSeconds, isFavourite = isFavourite,
                    onSkipSecondsChange = onSkipSecondsChange, onFavouriteToggle = onFavouriteToggle)
            }
            Spacer(Modifier.height(8.dp))
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                MediaScrubber(positionMs = state.positionMs, durationMs = state.durationMs, onSeek = onSeek,
                    skipSeconds = state.skipSeconds, onSkipBackward = onSkipBackward, onSkipForward = onSkipForward,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                PlaybackSpeedControl(currentSpeed = state.playbackSpeed, onSpeedChange = onSpeedChange,
                    expanded = speedExpanded, onExpandedChange = { speedExpanded = it })
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
    onImportLyrics: () -> Unit, onRemoveLyrics: () -> Unit,
    onFavouriteToggle: () -> Unit, onShuffleToggle: () -> Unit, onQueueToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit, onPlayNext: () -> Unit, onPlayPrev: () -> Unit,
    isFavourite: Boolean, onRotate: () -> Unit, onClose: () -> Unit = {}, onBack: () -> Unit = {}, modifier: Modifier = Modifier
) {
    val hasLyrics = state.lyrics.isNotEmpty()
    val canSkip = state.isQueueMode && state.currentPlaylistId != null && state.queue.size > 1
    val isPlaylistContext = state.currentPlaylistId != null
    var speedExpanded by remember { mutableStateOf(false) }
    val (dominantColor, secondaryColor) = rememberCoverArtColors(item)

    Box(modifier = modifier.fillMaxSize()
        .pointerInput(Unit) { detectTapGestures { if (speedExpanded) speedExpanded = false } }) {
        AnimatedBackground(dominantColor = dominantColor, secondaryColor = secondaryColor)
        // Back + Close pinned to very top-left of the screen
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(start = 16.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, "Back", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                }
            }
            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, "Close", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                }
            }
        }
        Row(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(0.44f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AnimatedAlbumArt(item, state.isPlaying, 150)
                Spacer(Modifier.height(12.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(item.artist, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f), textAlign = TextAlign.Center, maxLines = 1)
            }
            Column(modifier = Modifier.weight(0.56f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Top row: speed control on left, rotate on right
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PlaybackSpeedControl(currentSpeed = state.playbackSpeed, onSpeedChange = onSpeedChange,
                        expanded = speedExpanded, onExpandedChange = { speedExpanded = it })
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                        IconButton(onClick = onRotate) {
                            Icon(Icons.Rounded.ScreenRotation, "Rotate", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        }
                    }
                }
                if (hasLyrics) {
                    LyricsOverlay(lyrics = state.lyrics, activeIndex = state.activeLyricIndex,
                        slideDirection = LyricsSlideDirection.UP, centerAndHighlight = state.centerLyrics,
                        modifier = Modifier.fillMaxWidth().weight(1f))
                } else { Spacer(Modifier.weight(1f)) }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (canSkip) 0.15f else 0.06f, modifier = Modifier.size(42.dp)) {
                        IconButton({ if (canSkip) onPlayPrev() }, Modifier.fillMaxSize(), canSkip) {
                            Icon(Icons.Rounded.SkipPrevious, "Prev", Modifier.size(24.dp),
                                tint = if (canSkip) MaterialTheme.colorScheme.onSurface.copy(0.85f) else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    AnimatedPlayPauseButton(isPlaying = state.isPlaying, onClick = onPlayPause)
                    Spacer(Modifier.width(12.dp))
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (canSkip) 0.15f else 0.06f, modifier = Modifier.size(42.dp)) {
                        IconButton({ if (canSkip) onPlayNext() }, Modifier.fillMaxSize(), canSkip) {
                            Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(24.dp),
                                tint = if (canSkip) MaterialTheme.colorScheme.onSurface.copy(0.85f) else MaterialTheme.colorScheme.onSurface.copy(0.25f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AnimatedIconButton(onClick = onShuffleToggle, active = state.isShuffle, cornerRadius = 20.dp, size = 36.dp) {
                        Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(16.dp),
                            tint = if (state.isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                    }
                    LiquidGlassSurface(cornerRadius = 20.dp, alpha = if (state.isQueueMode) 0.32f else if (!isPlaylistContext) 0.05f else 0.12f,
                        modifier = Modifier.height(36.dp).wrapContentWidth()
                            .then(if (isPlaylistContext) Modifier.clickable { onQueueToggle() } else Modifier)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp).fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.QueueMusic, null, Modifier.size(14.dp),
                                tint = if (!isPlaylistContext) MaterialTheme.colorScheme.onSurface.copy(0.25f)
                                       else if (state.isQueueMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                            Text(if (state.isQueueMode) "Queue On" else "Queue", fontSize = 11.sp,
                                color = if (!isPlaylistContext) MaterialTheme.colorScheme.onSurface.copy(0.25f)
                                        else if (state.isQueueMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.13f) {
                        Row(modifier = Modifier.pointerInput(Unit) { detectTapGestures { onImportLyrics() } }.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Lyrics, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                            Text(if (hasLyrics) "Change" else "Import Lyrics", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary.copy(0.7f))
                            if (hasLyrics) {
                                Spacer(Modifier.width(2.dp))
                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(0.12f))
                                    .pointerInput(Unit) { detectTapGestures { onRemoveLyrics() } },
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Close, "Remove", Modifier.size(9.dp), tint = MaterialTheme.colorScheme.error.copy(0.8f))
                                }
                            }
                        }
                    }
                    SkipAndHeartPill(skipSeconds = state.skipSeconds, isFavourite = isFavourite,
                        onSkipSecondsChange = onSkipSecondsChange, onFavouriteToggle = onFavouriteToggle)
                }
                Spacer(Modifier.height(8.dp))
                LiquidGlassSurface(cornerRadius = 14.dp, alpha = 0.12f, modifier = Modifier.fillMaxWidth()) {
                    MediaScrubber(positionMs = state.positionMs, durationMs = state.durationMs, onSeek = onSeek,
                        skipSeconds = state.skipSeconds, onSkipBackward = onSkipBackward, onSkipForward = onSkipForward,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }
    }
}

internal fun formatMs(ms: Long): String {
    val total = ms / 1000; val m = total / 60; val s = total % 60
    return "%d:%02d".format(m, s)
}
