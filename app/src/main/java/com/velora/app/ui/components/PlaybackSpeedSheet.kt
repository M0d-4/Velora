package com.velora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

/**
 * Compact inline speed picker. Expands/collapses with animation.
 * onExpandedChange lets the parent know when the panel opens/closes
 * so it can hide other controls.
 * Horizontal drag within the expanded panel won't leak to the tab pager
 * (pointerInput consumes horizontal events inside the pill).
 */
@Composable
fun PlaybackSpeedControl(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    isVideoOverlay: Boolean = false,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    fun setExpanded(v: Boolean) {
        expanded = v
        onExpandedChange?.invoke(v)
    }

    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            if (targetState) {
                // Expanding: fade+slide in from left
                (fadeIn(tween(200)) + expandHorizontally(tween(220))) togetherWith
                (fadeOut(tween(150)))
            } else {
                // Collapsing
                (fadeIn(tween(200))) togetherWith
                (fadeOut(tween(150)) + shrinkHorizontally(tween(200)))
            }
        },
        label = "speedPanel"
    ) { isExpanded ->
        if (!isExpanded) {
            // Collapsed: single pill
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            LiquidGlassSurface(
                cornerRadius = 999.dp,
                alpha = if (isVideoOverlay) 0.22f else 0.18f,
                modifier = Modifier.wrapContentWidth().liquidPressEffect(pressed)
            ) {
                Row(
                    modifier = Modifier
                        .clickable(interactionSource = interaction, indication = null) { setExpanded(true) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(14.dp),
                        tint = if (isVideoOverlay) Color.White.copy(0.85f) else MaterialTheme.colorScheme.primary)
                    Text(speedLabel(currentSpeed), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (isVideoOverlay) Color.White.copy(0.9f) else MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            // Expanded: horizontal pill row
            // Consume horizontal drag so the pager doesn't intercept
            LiquidGlassSurface(
                cornerRadius = 999.dp,
                alpha = if (isVideoOverlay) 0.28f else 0.18f,
                modifier = Modifier
                    .wrapContentWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, _ -> /* consume */ }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SPEEDS.forEach { speed ->
                        val isSelected = currentSpeed == speed
                        val bgAlpha by animateFloatAsState(
                            if (isSelected) if (isVideoOverlay) 0.22f else 0.20f else 0f,
                            tween(180), label = "speedbg"
                        )
                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isVideoOverlay) Color.White.copy(bgAlpha)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha),
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable(interactionSource = interaction, indication = null) {
                                    onSpeedChange(speed)
                                    setExpanded(false)
                                }
                                .graphicsLayer { val s = if (pressed) 0.88f else 1f; scaleX = s; scaleY = s }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                speedLabel(speed), fontSize = 11.sp,
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
