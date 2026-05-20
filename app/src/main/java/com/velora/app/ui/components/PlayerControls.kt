package com.velora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    skipSeconds: Int,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main transport row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Rewind button
            SkipButton(
                icon = Icons.Rounded.Replay,
                label = "-${skipSeconds}s",
                onClick = onSkipBackward
            )

            // Play / Pause — large liquid glass pill
            LiquidGlassSurface(
                cornerRadius = 999.dp,
                alpha = 0.25f,
                modifier = Modifier.size(72.dp)
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                                scaleOut(targetScale = 0.7f) + fadeOut()
                        },
                        label = "playpause"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Forward button
            SkipButton(
                icon = Icons.Rounded.Forward10,
                label = "+${skipSeconds}s",
                onClick = onSkipForward,
                mirror = true
            )
        }

        // Skip seconds toggle
        SkipSecondsPill(
            current = skipSeconds,
            onChange = onSkipSecondsChange
        )
    }
}

@Composable
private fun SkipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    mirror: Boolean = false
) {
    LiquidGlassSurface(
        cornerRadius = 20.dp,
        alpha = 0.15f,
        modifier = Modifier.size(56.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SkipSecondsPill(
    current: Int,
    onChange: (Int) -> Unit
) {
    LiquidGlassSurface(
        cornerRadius = 999.dp,
        alpha = 0.12f,
        modifier = Modifier.wrapContentWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            listOf(5, 10).forEach { secs ->
                val isSelected = current == secs
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .then(
                            if (isSelected)
                                Modifier.run {
                                    // highlight chip
                                    this
                                }
                            else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onChange(secs) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        // Inner pill highlight
                        LiquidGlassSurface(
                            cornerRadius = 999.dp,
                            alpha = 0.4f,
                            modifier = Modifier.matchParentSize()
                        ) {}
                    }
                    Text(
                        text = "${secs}s skip",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
