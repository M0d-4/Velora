package com.velora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: shuffle + queue mode
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle pill
            val shuffleInteraction = remember { MutableInteractionSource() }
            val shufflePressed by shuffleInteraction.collectIsPressedAsState()
            LiquidGlassSurface(
                cornerRadius = 20.dp,
                alpha = if (isShuffle) 0.32f else 0.12f,
                modifier = Modifier
                    .size(42.dp)
                    .liquidPressEffect(shufflePressed)
            ) {
                IconButton(
                    onClick = onShuffleToggle,
                    modifier = Modifier.fillMaxSize(),
                    interactionSource = shuffleInteraction
                ) {
                    Icon(
                        Icons.Rounded.Shuffle, "Shuffle",
                        modifier = Modifier.size(18.dp),
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            // Queue pill
            val queueInteraction = remember { MutableInteractionSource() }
            val queuePressed by queueInteraction.collectIsPressedAsState()
            LiquidGlassSurface(
                cornerRadius = 20.dp,
                alpha = if (isQueueMode) 0.32f else 0.12f,
                modifier = Modifier
                    .height(42.dp)
                    .wrapContentWidth()
                    .liquidPressEffect(queuePressed)
            ) {
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = queueInteraction,
                            indication = null,
                            onClick = onQueueToggle
                        )
                        .padding(horizontal = 14.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Rounded.QueueMusic, null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isQueueMode) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        if (isQueueMode) "Queue On" else "Play Next",
                        fontSize = 12.sp,
                        color = if (isQueueMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Row 2: Rewind | Play/Pause | Forward
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SkipButton(icon = Icons.Rounded.Replay, desc = "Rewind", onClick = onSkipBackward)

            // Play / Pause — bigger glass pill
            val playInteraction = remember { MutableInteractionSource() }
            val playPressed by playInteraction.collectIsPressedAsState()
            LiquidGlassSurface(
                cornerRadius = 999.dp,
                alpha = 0.25f,
                modifier = Modifier
                    .size(72.dp)
                    .liquidPressEffect(playPressed)
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize(),
                    interactionSource = playInteraction
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            scaleIn(initialScale = 0.65f) + fadeIn() togetherWith
                                scaleOut(targetScale = 0.65f) + fadeOut()
                        },
                        label = "playpause"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            SkipButton(icon = Icons.Rounded.Forward10, desc = "Forward", onClick = onSkipForward)
        }

        // Row 3: 5s | 10s | ♥  — all inside one unified pill
        SkipAndHeartPill(
            skipSeconds = skipSeconds,
            isFavourite = isFavourite,
            onSkipSecondsChange = onSkipSecondsChange,
            onFavouriteToggle = onFavouriteToggle
        )
    }
}

/**
 * Single unified pill: [5s] [10s] | [♥]
 * The heart sits right beside the skip-seconds switcher inside the same LiquidGlass container.
 */
@Composable
fun SkipAndHeartPill(
    skipSeconds: Int,
    isFavourite: Boolean,
    onSkipSecondsChange: (Int) -> Unit,
    onFavouriteToggle: () -> Unit
) {
    val heartScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val heartTint by animateColorAsState(
        targetValue = if (isFavourite) Color(0xFFFF3B6B)
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        animationSpec = tween(280),
        label = "heartColor"
    )

    LiquidGlassSurface(
        cornerRadius = 999.dp,
        alpha = 0.18f,
        modifier = Modifier.wrapContentWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 5s / 10s options
            listOf(5, 10).forEach { secs ->
                val isSelected = skipSeconds == secs
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .then(
                            if (isSelected) Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                RoundedCornerShape(999.dp)
                            ) else Modifier
                        )
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { onSkipSecondsChange(secs) }
                        .graphicsLayer {
                            val s = if (pressed) 0.90f else 1f
                            scaleX = s; scaleY = s
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${secs}s",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Thin divider between skip seconds and heart
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )

            // Heart button — same height, right side of the pill
            val heartInteraction = remember { MutableInteractionSource() }
            val heartPressed by heartInteraction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .then(
                        if (isFavourite) Modifier.background(
                            Color(0xFFFF3B6B).copy(alpha = 0.14f),
                            RoundedCornerShape(999.dp)
                        ) else Modifier
                    )
                    .clickable(
                        interactionSource = heartInteraction,
                        indication = null
                    ) {
                        onFavouriteToggle()
                        scope.launch {
                            heartScale.animateTo(1.45f, tween(110, easing = FastOutSlowInEasing))
                            heartScale.animateTo(1f, spring(stiffness = Spring.StiffnessHigh))
                        }
                    }
                    .graphicsLayer {
                        val s = if (heartPressed) 0.88f else 1f
                        scaleX = s; scaleY = s
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favourite",
                    modifier = Modifier.size(18.dp).scale(heartScale.value),
                    tint = heartTint
                )
            }
        }
    }
}

@Composable
fun SkipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    LiquidGlassSurface(
        cornerRadius = 20.dp,
        alpha = 0.15f,
        modifier = Modifier
            .size(56.dp)
            .liquidPressEffect(pressed)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            interactionSource = interaction
        ) {
            Icon(icon, desc,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
        }
    }
}

// Standalone heart for video player overlay (no pill wrapping needed there)
@Composable
fun HeartButton(isFavourite: Boolean, onToggle: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val tint by animateColorAsState(
        targetValue = if (isFavourite) Color(0xFFFF3B6B)
                      else Color.White.copy(alpha = 0.55f),
        animationSpec = tween(280),
        label = "heartColor"
    )

    LiquidGlassSurface(
        cornerRadius = 999.dp,
        alpha = if (isFavourite) 0.30f else 0.18f,
        modifier = Modifier
            .size(34.dp)
            .liquidPressEffect(pressed)
    ) {
        IconButton(
            onClick = {
                onToggle()
                scope.launch {
                    scale.animateTo(1.45f, tween(110))
                    scale.animateTo(1f, spring(stiffness = Spring.StiffnessHigh))
                }
            },
            modifier = Modifier.fillMaxSize(),
            interactionSource = interaction
        ) {
            Icon(
                imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favourite",
                modifier = Modifier.size(16.dp).scale(scale.value),
                tint = tint
            )
        }
    }
}
