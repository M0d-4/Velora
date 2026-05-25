package com.velora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp

/**
 * iOS 26-style waveform scrubber.
 * - Tap anywhere to seek immediately.
 * - Drag horizontally to scrub; onSeek fires only on release to avoid resetting.
 * - While dragging the display shows the drag position live; the real progress
 *   from the ViewModel is ignored until the finger lifts.
 */
@Composable
fun AudioWaveformBar(
    amplitudes: List<Float>,
    progress: Float,           // 0..1 from ViewModel
    onSeek: (Float) -> Unit,   // only called on tap or drag-end
    modifier: Modifier = Modifier
) {
    val primary   = MaterialTheme.colorScheme.primary
    val unfilled  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val thumb     = primary

    // Separate "interaction in progress" from real progress so the bar
    // never jumps back to the ViewModel value mid-drag.
    var isDragging   by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val displayProgress = if (isDragging) dragFraction else progress

    // Smoothly animate each bar amplitude
    val animatedAmps = amplitudes.map { amp ->
        val v by animateFloatAsState(amp, tween(80, easing = LinearEasing), label = "amp")
        v
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Wait for the first finger down
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startFrac = (down.position.x / size.width).coerceIn(0f, 1f)
                    isDragging   = true
                    dragFraction = startFrac

                    var hasMoved = false
                    // Track subsequent moves
                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull() ?: break
                        if (pointer.pressed) {
                            val frac = (pointer.position.x / size.width).coerceIn(0f, 1f)
                            dragFraction = frac
                            hasMoved = true
                            pointer.consume()
                        } else {
                            break
                        }
                    } while (true)

                    // Finger lifted — commit seek
                    isDragging = false
                    onSeek(dragFraction)
                }
            }
    ) {
        val count      = amplitudes.size
        val w          = size.width
        val h          = size.height
        val gap        = w * 0.006f
        val barW       = (w - gap * (count - 1)) / count
        val progressX  = w * displayProgress

        animatedAmps.forEachIndexed { i, amp ->
            val x    = i * (barW + gap)
            val barH = (h * 0.12f) + (h * 0.82f * amp.coerceIn(0.04f, 1f))
            val top  = (h - barH) / 2f
            val cx   = x + barW / 2f

            // Colour: filled (before thumb) → gradient edge → unfilled
            val distFromThumb = cx - progressX
            val color = when {
                cx <= progressX                   -> primary
                distFromThumb <= barW * 3         -> lerp(primary, unfilled, distFromThumb / (barW * 3))
                else                              -> unfilled
            }

            drawRoundRect(
                color     = color,
                topLeft   = Offset(x, top),
                size      = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f)
            )
        }

        // iOS-style thumb: larger glowing circle
        val thumbX = (w * displayProgress).coerceIn(8f, w - 8f)
        // Glow
        drawCircle(color = primary.copy(alpha = 0.25f), radius = 14f,
            center = Offset(thumbX, h / 2f))
        // Solid thumb
        drawCircle(color = thumb, radius = 7f,
            center = Offset(thumbX, h / 2f))
    }
}
