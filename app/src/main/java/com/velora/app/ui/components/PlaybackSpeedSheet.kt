package com.velora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

/**
 * Compact inline speed picker rendered as a horizontal row of liquid-glass pills.
 * Shows a speed button that expands into a pill row on tap.
 */
@Composable
fun PlaybackSpeedControl(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    isVideoOverlay: Boolean = false   // true → use white tones for video overlay
) {
    var expanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!expanded) {
            // Collapsed: single pill showing current speed
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()

            LiquidGlassSurface(
                cornerRadius = 999.dp,
                alpha = if (isVideoOverlay) 0.22f else 0.18f,
                modifier = Modifier
                    .wrapContentWidth()
                    .liquidPressEffect(pressed)
            ) {
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { expanded = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Rounded.Speed, null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isVideoOverlay) Color.White.copy(0.85f)
                               else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        speedLabel(currentSpeed),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isVideoOverlay) Color.White.copy(0.9f)
                                else MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // Expanded: horizontal pill row
            LiquidGlassSurface(
                cornerRadius = 999.dp,
                alpha = if (isVideoOverlay) 0.28f else 0.18f,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SPEEDS.forEach { speed ->
                        val isSelected = currentSpeed == speed
                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .then(
                                    if (isSelected) Modifier.background(
                                        if (isVideoOverlay) Color.White.copy(0.22f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                        RoundedCornerShape(999.dp)
                                    ) else Modifier
                                )
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) {
                                    onSpeedChange(speed)
                                    expanded = false
                                }
                                .graphicsLayer {
                                    val s = if (pressed) 0.88f else 1f
                                    scaleX = s; scaleY = s
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                speedLabel(speed),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected && isVideoOverlay -> Color.White
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isVideoOverlay -> Color.White.copy(0.6f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun speedLabel(speed: Float): String = when (speed) {
    0.5f  -> "0.5×"
    0.75f -> "0.75×"
    1f    -> "1×"
    1.25f -> "1.25×"
    1.5f  -> "1.5×"
    1.75f -> "1.75×"
    2f    -> "2×"
    else  -> "${speed}×"
}
