package com.velora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * iOS 26-style scrubber with tap-zone skip buttons built into each end of the bar.
 *
 * • Tap/drag the track itself → seek
 * • Tap the left ~20% of the track area → rewind [skipSeconds]
 * • Tap the right ~20% of the track area → fast-forward [skipSeconds]
 * Both skip zones flash a brief icon animation so the user gets visual feedback.
 *
 * [skipSeconds] and [onSkipForward]/[onSkipBackward] are optional; if omitted the
 * scrubber behaves exactly as before (seek-only).
 */
@Composable
fun MediaScrubber(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    skipSeconds: Int = 10,
    onSkipForward: (() -> Unit)? = null,
    onSkipBackward: (() -> Unit)? = null
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    var isDragging      by remember { mutableStateOf(false) }
    var dragProgress    by remember { mutableFloatStateOf(progress) }

    // Flash states for skip feedback icons
    var rewindFlash     by remember { mutableStateOf(false) }
    var forwardFlash    by remember { mutableStateOf(false) }
    val rewindAlpha     by animateFloatAsState(if (rewindFlash) 1f else 0f,
        tween(if (rewindFlash) 80 else 350), label = "rAlpha")
    val forwardAlpha    by animateFloatAsState(if (forwardFlash) 1f else 0f,
        tween(if (forwardFlash) 80 else 350), label = "fAlpha")

    val scope = rememberCoroutineScope()

    fun flashRewind() {
        if (onSkipBackward == null) return
        rewindFlash = true
        scope.launch { delay(400); rewindFlash = false }
        onSkipBackward()
    }

    fun flashForward() {
        if (onSkipForward == null) return
        forwardFlash = true
        scope.launch { delay(400); forwardFlash = false }
        onSkipForward()
    }

    // Tap-zone width: 22% of track on each side
    val tapZoneFraction = 0.22f

    val trackHeightDp by animateDpAsState(if (isDragging) 6.dp else 3.dp, label = "track")
    val thumbWidthDp  by animateDpAsState(if (isDragging) 20.dp else 12.dp, label = "thumb")
    val thumbHeightDp by animateDpAsState(if (isDragging) 20.dp else 12.dp, label = "thumbH")

    val primary        = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Column(modifier = modifier) {
        // ── Track + thumb + skip zones ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)   // taller hit area so skip zones are easy to tap
                .pointerInput(durationMs, onSkipForward, onSkipBackward) {
                    detectTapGestures { offset ->
                        val frac = offset.x / size.width
                        when {
                            frac <= tapZoneFraction && onSkipBackward != null -> flashRewind()
                            frac >= (1f - tapZoneFraction) && onSkipForward != null -> flashForward()
                            else -> {
                                val p = frac.coerceIn(0f, 1f)
                                onSeek((p * durationMs).toLong())
                            }
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd   = {
                            isDragging = false
                            onSeek((dragProgress * durationMs).toLong())
                        },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { change, _ ->
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
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
                        drawRoundRect(color = surfaceVariant, cornerRadius = CornerRadius(size.height / 2f))
                        drawRoundRect(
                            color = primary,
                            size  = Size(size.width * displayProgress, size.height),
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
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color.White.copy(alpha = 0.85f))
                            ),
                            cornerRadius = CornerRadius(size.minDimension / 2f)
                        )
                        drawRoundRect(
                            color    = Color.White.copy(alpha = 0.5f),
                            topLeft  = Offset(0f, 0f),
                            size     = Size(size.width, size.height * 0.4f),
                            cornerRadius = CornerRadius(size.minDimension / 2f)
                        )
                    }
            )

            // Left skip flash icon
            if (onSkipBackward != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .alpha(rewindAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(Icons.Rounded.FastRewind, null,
                        modifier = Modifier.size(14.dp), tint = primary)
                    Text("-${skipSeconds}s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primary)
                }
            }

            // Right skip flash icon
            if (onSkipForward != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .alpha(forwardAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("+${skipSeconds}s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primary)
                    Icon(Icons.Rounded.FastForward, null,
                        modifier = Modifier.size(14.dp), tint = primary)
                }
            }
        }

        // ── Time labels ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = MediaRepository.formatDuration(
                    if (isDragging) (dragProgress * durationMs).toLong() else positionMs
                ),
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
