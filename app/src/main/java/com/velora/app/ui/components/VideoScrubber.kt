package com.velora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velora.app.util.MediaRepository

/**
 * iOS 26 style video scrubber — a capsule track with a bright thumb pill that expands on drag.
 */
@Composable
fun VideoScrubber(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    val trackHeightDp by animateDpAsState(if (isDragging) 6.dp else 3.dp, label = "track")
    val thumbWidthDp by animateDpAsState(if (isDragging) 20.dp else 12.dp, label = "thumb")
    val thumbHeightDp by animateDpAsState(if (isDragging) 20.dp else 12.dp, label = "thumbH")

    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Column(modifier = modifier) {
        // Track + thumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragProgress * durationMs).toLong())
                        },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { change, _ ->
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        val p = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((p * durationMs).toLong())
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val displayProgress = if (isDragging) dragProgress else progress

            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeightDp)
                    .align(Alignment.Center)
                    .drawBehind {
                        // Unfilled
                        drawRoundRect(
                            color = surfaceVariant,
                            cornerRadius = CornerRadius(size.height / 2f)
                        )
                        // Filled
                        drawRoundRect(
                            color = primary,
                            size = Size(size.width * displayProgress, size.height),
                            cornerRadius = CornerRadius(size.height / 2f)
                        )
                    }
            )

            // Thumb pill
            Box(
                modifier = Modifier
                    .offset(x = (-thumbWidthDp / 2))
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(displayProgress)
                    .wrapContentWidth(Alignment.End)
                    .size(thumbWidthDp, thumbHeightDp)
                    .drawBehind {
                        // Glass pill with highlight
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color.White.copy(alpha = 0.85f))
                            ),
                            cornerRadius = CornerRadius(size.minDimension / 2f)
                        )
                        // Top shine
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.5f),
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, size.height * 0.4f),
                            cornerRadius = CornerRadius(size.minDimension / 2f)
                        )
                    }
            )
        }

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = MediaRepository.formatDuration(if (isDragging) (dragProgress * durationMs).toLong() else positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
            Text(
                text = MediaRepository.formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}
