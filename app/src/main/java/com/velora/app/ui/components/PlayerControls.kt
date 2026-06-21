package com.velora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.velora.app.ui.theme.VeloraMotion
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ── PlayerControls ────────────────────────────────────────────────────────────
// NOTE: No rewind/forward SkipButtons here — those live inside MediaScrubber.
// This composable only contains: shuffle/queue row, skip-seconds + heart pill,
// and the play/pause button.
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    skipSeconds: Int,
    isFavourite: Boolean,
    isShuffle: Boolean,
    isQueueMode: Boolean,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipSecondsChange: (Int) -> Unit,
    onFavouriteToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    onQueueToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val useWhiteIcons = LocalUseWhiteIcons.current
    val iconColor    = if (useWhiteIcons) Color.White else MaterialTheme.colorScheme.primary
    val iconColorDim = if (useWhiteIcons) Color.White.copy(0.5f) else MaterialTheme.colorScheme.primary.copy(0.5f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Shuffle + Queue
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedIconButton(
                onClick = onShuffleToggle,
                active = isShuffle,
                cornerRadius = 20.dp,
                size = 42.dp
            ) {
                Icon(Icons.Rounded.Shuffle, "Shuffle", modifier = Modifier.size(18.dp),
                    tint = if (isShuffle) iconColor
                           else iconColorDim)
            }

            val queueInteraction = remember { MutableInteractionSource() }
            val queuePressed by queueInteraction.collectIsPressedAsState()
            LiquidGlassSurface(
                cornerRadius = 20.dp,
                alpha = if (isQueueMode) 0.32f else 0.12f,
                modifier = Modifier.height(42.dp).wrapContentWidth().liquidPressEffect(queuePressed)
            ) {
                Row(
                    modifier = Modifier
                        .clickable(interactionSource = queueInteraction, indication = null, onClick = onQueueToggle)
                        .padding(horizontal = 14.dp).fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Rounded.QueueMusic, null, modifier = Modifier.size(16.dp),
                        tint = if (isQueueMode) iconColor
                               else iconColorDim)
                    Text(if (isQueueMode) "Queue On" else "Play Next", fontSize = 12.sp,
                        color = if (isQueueMode) iconColor
                                else iconColorDim)
                }
            }
        }

        // Row 2: [5s | 10s | ♥] — drag left/right to change skip seconds
        SkipAndHeartPill(
            skipSeconds = skipSeconds,
            isFavourite = isFavourite,
            onSkipSecondsChange = onSkipSecondsChange,
            onFavouriteToggle = onFavouriteToggle
        )

        // Row 3: Play/Pause only — skip buttons are inside MediaScrubber
        AnimatedPlayPauseButton(isPlaying = isPlaying, onClick = onPlayPause)
    }
}

// ── Skip + Heart pill ─────────────────────────────────────────────────────────
@Composable
fun SkipAndHeartPill(
    skipSeconds: Int,
    isFavourite: Boolean,
    onSkipSecondsChange: (Int) -> Unit,
    onFavouriteToggle: () -> Unit
) {
    val useWhiteIcons = LocalUseWhiteIcons.current
    val iconColor    = if (useWhiteIcons) Color.White else MaterialTheme.colorScheme.primary
    val iconColorDim = if (useWhiteIcons) Color.White.copy(0.5f) else MaterialTheme.colorScheme.primary.copy(0.5f)
    val options = listOf(5, 10)
    val heartScale = remember { Animatable(1f) }
    val splashScale = remember { Animatable(0f) }
    val splashAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val heartTint by animateColorAsState(
        targetValue = if (isFavourite) Color(0xFFFF3B6B) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        animationSpec = VeloraMotion.effectsSlow(), label = "heartColor"
    )
    var dragAccum by remember { mutableFloatStateOf(0f) }

    LiquidGlassSurface(forceGlass = true, cornerRadius = 999.dp, alpha = 0.18f) {
        Row(
            modifier = Modifier
                .pointerInput(skipSeconds, options) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onDragEnd   = { dragAccum = 0f },
                        onDragCancel = { dragAccum = 0f },
                        onHorizontalDrag = { _, delta ->
                            dragAccum += delta
                            val threshold = 60f
                            val currentIdx = options.indexOf(skipSeconds)
                            if (dragAccum > threshold && currentIdx < options.lastIndex) {
                                onSkipSecondsChange(options[currentIdx + 1]); dragAccum = 0f
                            } else if (dragAccum < -threshold && currentIdx > 0) {
                                onSkipSecondsChange(options[currentIdx - 1]); dragAccum = 0f
                            }
                        }
                    )
                }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { secs ->
                val isSelected = skipSeconds == secs
                val bgAlpha by animateFloatAsState(if (isSelected) 0.22f else 0f, VeloraMotion.effectsDefault(), label = "skipbg_$secs")
                val textScale by animateFloatAsState(if (isSelected) 1.08f else 1f, VeloraMotion.standardSpatialDefault(), label = "skipScale_$secs")
                val skipPillShape = pixelAwareShape(999.dp)
                Box(
                    modifier = Modifier
                        .clip(skipPillShape)
                        .background(iconColor.copy(alpha = bgAlpha), skipPillShape)
                        .pointerInput(secs) { detectTapGestures { onSkipSecondsChange(secs) } }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .graphicsLayer { scaleX = textScale; scaleY = textScale },
                    contentAlignment = Alignment.Center
                ) {
                    Text("${secs}s", fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) iconColor
                                else iconColorDim)
                }
            }

            Spacer(Modifier.width(2.dp))
            Box(Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.onSurface.copy(0.15f)))
            Spacer(Modifier.width(2.dp))

            val heartInteraction = remember { MutableInteractionSource() }
            val heartPressed by heartInteraction.collectIsPressedAsState()
            val heartPillShape = pixelAwareShape(999.dp)

            Box(
                modifier = Modifier
                    .clip(heartPillShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Red splash ripple
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = splashScale.value; scaleY = splashScale.value; alpha = splashAlpha.value
                        }
                        .background(Color(0xFFFF3B6B).copy(alpha = 0.35f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .then(if (isFavourite) Modifier.background(Color(0xFFFF3B6B).copy(alpha = 0.14f), heartPillShape) else Modifier)
                        .clickable(interactionSource = heartInteraction, indication = null) {
                            onFavouriteToggle()
                            scope.launch {
                                heartScale.animateTo(1.45f, VeloraMotion.effectsFast())
                                heartScale.animateTo(1f, VeloraMotion.standardSpatialFast())
                            }
                            if (!isFavourite) {
                                scope.launch {
                                    splashScale.snapTo(0f); splashAlpha.snapTo(0.9f)
                                    splashScale.animateTo(1.8f, VeloraMotion.effectsSlow())
                                    splashAlpha.animateTo(0f, VeloraMotion.effectsSlow())
                                    splashScale.snapTo(0f)
                                }
                            }
                        }
                        .graphicsLayer { val s = if (heartPressed) 0.88f else 1f; scaleX = s; scaleY = s },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Favorite",
                        modifier = Modifier.size(18.dp).scale(heartScale.value),
                        tint = heartTint
                    )
                }
            }
        }
    }
}

// ── Animated play/pause button ────────────────────────────────────────────────
@Composable
fun AnimatedPlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    val useWhiteIcons = LocalUseWhiteIcons.current
    val iconColor = if (useWhiteIcons) Color.White else MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = VeloraMotion.standardSpatialSlow(), label = "ppScale"
    )
    LiquidGlassSurface(
        cornerRadius = 999.dp, alpha = 0.25f,
        modifier = Modifier.size(72.dp).graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize(), interactionSource = interaction) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    scaleIn(initialScale = 0.55f, animationSpec = VeloraMotion.standardSpatialDefault()) +
                    fadeIn(VeloraMotion.effectsDefault()) togetherWith
                    scaleOut(targetScale = 0.55f, animationSpec = VeloraMotion.standardSpatialDefault()) +
                    fadeOut(VeloraMotion.effectsFast())
                }, label = "playpause"
            ) { playing ->
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null, modifier = Modifier.size(38.dp),
                    tint = iconColor
                )
            }
        }
    }
}

// ── Generic animated icon button ─────────────────────────────────────────────
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    active: Boolean = false,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = VeloraMotion.standardSpatialSlow(), label = "btnScale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (active) 0.32f else 0.12f, animationSpec = VeloraMotion.effectsDefault(), label = "btnBg"
    )
    LiquidGlassSurface(
        cornerRadius = cornerRadius, alpha = bgAlpha,
        modifier = Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize(),
            interactionSource = interaction, content = { content() })
    }
}

// ── Rewind / Forward skip button used in audio player transport row ──────────
@Composable
fun SkipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    val useWhiteIcons = LocalUseWhiteIcons.current
    val iconColor = if (useWhiteIcons) Color.White else MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = VeloraMotion.standardSpatialSlow(), label = "skipBtnScale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (pressed) (if (desc == "Rewind") -18f else 18f) else 0f,
        animationSpec = VeloraMotion.standardSpatialSlow(), label = "skipBtnRot"
    )
    LiquidGlassSurface(
        cornerRadius = 20.dp,
        alpha = 0.15f,
        modifier = Modifier
            .size(50.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            interactionSource = interaction
        ) {
            Icon(icon, desc,
                modifier = Modifier.size(26.dp),
                tint = iconColor)
        }
    }
}

// ── Heart button (standalone, used in video player) ───────────────────────────
@Composable
fun HeartButton(isFavourite: Boolean, onToggle: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val splashScale = remember { Animatable(0f) }
    val splashAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = VeloraMotion.standardSpatialSlow(), label = "heartBtnScale"
    )
    val tint by animateColorAsState(
        targetValue = if (isFavourite) Color(0xFFFF3B6B) else Color.White.copy(alpha = 0.55f),
        animationSpec = VeloraMotion.effectsSlow(), label = "heartColor"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .graphicsLayer { scaleX = splashScale.value; scaleY = splashScale.value; alpha = splashAlpha.value }
                .background(Color(0xFFFF3B6B).copy(alpha = 0.40f), CircleShape)
        )
        LiquidGlassSurface(
            cornerRadius = 999.dp,
            alpha = if (isFavourite) 0.30f else 0.18f,
            modifier = Modifier.size(34.dp).graphicsLayer { scaleX = btnScale; scaleY = btnScale }
        ) {
            IconButton(
                onClick = {
                    onToggle()
                    scope.launch {
                        scale.animateTo(1.45f, VeloraMotion.effectsFast())
                        scale.animateTo(1f, VeloraMotion.standardSpatialFast())
                    }
                    if (!isFavourite) {
                        scope.launch {
                            splashScale.snapTo(0f); splashAlpha.snapTo(0.9f)
                            splashScale.animateTo(1.8f, VeloraMotion.effectsSlow())
                            splashAlpha.animateTo(0f, VeloraMotion.effectsSlow())
                            splashScale.snapTo(0f)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                interactionSource = interaction
            ) {
                Icon(
                    if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Favorite",
                    modifier = Modifier.size(16.dp).scale(scale.value),
                    tint = tint
                )
            }
        }
    }
}
