package com.velora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

/**
 * Waveform progress bar — bars of varying heights coloured by playback progress.
 * Mimics the style seen in Apple Music / modern audio players.
 */
@Composable
fun AudioWaveformBar(
    amplitudes: List<Float>,
    progress: Float,                 // 0..1
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

    // Animate each bar height transition
    val animatedAmps = amplitudes.map { amp ->
        val anim by animateFloatAsState(
            targetValue = amp,
            animationSpec = tween(durationMillis = 80, easing = LinearEasing),
            label = "bar"
        )
        anim
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val barCount = amplitudes.size
        val totalWidth = size.width
        val totalHeight = size.height
        val gap = totalWidth * 0.008f
        val barWidth = (totalWidth - gap * (barCount - 1)) / barCount
        val progressX = totalWidth * progress

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
    }
}
