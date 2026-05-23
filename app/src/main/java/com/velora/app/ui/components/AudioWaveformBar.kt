package com.velora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Waveform scrubber bar with interactive seek (tap + drag).
 * Bars animate smoothly with the actual playback progress.
 */
@Composable
fun AudioWaveformBar(
    amplitudes: List<Float>,
    progress: Float,          // 0..1
    onSeek: (Float) -> Unit,  // called with 0..1 fraction
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

    // Animate each bar height
    val animatedAmps = amplitudes.map { amp ->
        val anim by animateFloatAsState(
            targetValue = amp,
            animationSpec = tween(durationMillis = 80, easing = LinearEasing),
            label = "bar"
        )
        anim
    }

    // Track drag state locally so scrubber preview is smooth
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    val displayProgress = if (dragging) dragProgress else progress

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        onSeek(dragProgress)
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
    ) {
        val barCount = amplitudes.size
        val totalWidth = size.width
        val totalHeight = size.height
        val gap = totalWidth * 0.008f
        val barWidth = (totalWidth - gap * (barCount - 1)) / barCount
        val progressX = totalWidth * displayProgress

        animatedAmps.forEachIndexed { i, amp ->
            val x = i * (barWidth + gap)
            val barH = (totalHeight * 0.15f) + (totalHeight * 0.80f * amp)
            val top = (totalHeight - barH) / 2f

            val isFilled = (x + barWidth / 2f) <= progressX
            val isEdge = !isFilled && (x + barWidth / 2f) <= progressX + barWidth * 2

            val color = when {
                isFilled -> primary
                isEdge -> Color(
                    red = (primary.red * 0.6f + onSurface.red * 0.4f),
                    green = (primary.green * 0.6f + onSurface.green * 0.4f),
                    blue = (primary.blue * 0.6f + onSurface.blue * 0.4f),
                    alpha = 1f
                )
                else -> onSurface
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(x, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }

        // Draw scrubber thumb at current position
        val thumbX = totalWidth * displayProgress
        drawCircle(
            color = primary,
            radius = 6f,
            center = Offset(thumbX.coerceIn(6f, totalWidth - 6f), totalHeight / 2f)
        )
    }
}
