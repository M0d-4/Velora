package com.velora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velora.app.util.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Scrubber with VISIBLE rewind / forward icon buttons flanking the track.
 *
 * Layout:
 *   [⏮ skip] ──────── track ──────── [skip ⏭]
 *            0:00                 3:45
 *
 * Also supports tap-zone skip on the track ends (same 22% zones as before),
 * giving double the chance to skip.
 */
@Composable
fun MediaScrubber(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    skipSeconds: Int = 10,
    onSkipForward: (() -> Unit)? = null,
    onSkipBackward: (() -> Unit)? = null,
    isVideoOverlay: Boolean = false,   // true → white tints for dark video bg
    showSkipButtons: Boolean = true    // false → hide the flanking icon buttons
) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    var isDragging   by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    // Flash states
    var rewindFlash  by remember { mutableStateOf(false) }
    var forwardFlash by remember { mutableStateOf(false) }
    val rewindScale  by animateFloatAsState(if (rewindFlash) 1.25f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "rScale")
    val forwardScale by animateFloatAsState(if (forwardFlash) 1.25f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "fScale")

    val scope = rememberCoroutineScope()

    fun doRewind() {
        if (onSkipBackward == null) return
        rewindFlash = true
        scope.launch { delay(300); rewindFlash = false }
        onSkipBackward()
    }
    fun doForward() {
        if (onSkipForward == null) return
        forwardFlash = true
        scope.launch { delay(300); forwardFlash = false }
        onSkipForward()
    }

    val tapZoneFraction = 0.22f
    val trackHeightDp by animateDpAsState(if (isDragging) 6.dp else 3.dp, label = "track")
    val thumbWidthDp  by animateDpAsState(if (isDragging) 20.dp else 12.dp, label = "thumb")
    val thumbHeightDp by animateDpAsState(if (isDragging) 20.dp else 12.dp, label = "thumbH")

    val primary        = if (isVideoOverlay) Color.White else MaterialTheme.colorScheme.primary
    val trackBg        = if (isVideoOverlay) Color.White.copy(0.25f) else MaterialTheme.colorScheme.onSurface.copy(0.18f)
    val iconTint       = if (isVideoOverlay) Color.White.copy(0.85f) else MaterialTheme.colorScheme.onSurface.copy(0.75f)
    val timeTint       = if (isVideoOverlay) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurface.copy(0.65f)

    Column(modifier = modifier) {
        // ── Row: [rewind btn] [track] [forward btn] ────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rewind button
            if (onSkipBackward != null && showSkipButtons) {
                IconButton(
                    onClick = ::doRewind,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { scaleX = rewindScale; scaleY = rewindScale }
                ) {
                    Icon(Icons.Rounded.Replay, "-${skipSeconds}s",
                        modifier = Modifier.size(22.dp), tint = iconTint)
                }
            }

            // Track
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .pointerInput(durationMs, onSkipForward, onSkipBackward) {
                        detectTapGestures { offset ->
                            val frac = offset.x / size.width
                            when {
                                frac <= tapZoneFraction && onSkipBackward != null -> doRewind()
                                frac >= (1f - tapZoneFraction) && onSkipForward != null -> doForward()
                                else -> onSeek(((frac).coerceIn(0f, 1f) * durationMs).toLong())
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
                            drawRoundRect(color = trackBg, cornerRadius = CornerRadius(size.height / 2f))
                            drawRoundRect(color = primary, size = Size(size.width * displayProgress, size.height),
                                cornerRadius = CornerRadius(size.height / 2f))
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
                                brush = Brush.verticalGradient(listOf(Color.White, Color.White.copy(0.85f))),
                                cornerRadius = CornerRadius(size.minDimension / 2f)
                            )
                            drawRoundRect(color = Color.White.copy(0.5f),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height * 0.4f),
                                cornerRadius = CornerRadius(size.minDimension / 2f))
                        }
                )
            }

            // Forward button
            if (onSkipForward != null && showSkipButtons) {
                IconButton(
                    onClick = ::doForward,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { scaleX = forwardScale; scaleY = forwardScale }
                ) {
                    Icon(Icons.Rounded.Replay, "+${skipSeconds}s",
                        modifier = Modifier.size(22.dp).graphicsLayer { scaleX = -1f }, tint = iconTint)
                }
            }
        }

        // ── Time labels ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Pad left to align under track start (skip button width + spacing)
            val sidePad = if (onSkipBackward != null && showSkipButtons) 40.dp else 0.dp
            Text(
                text = MediaRepository.formatDuration(
                    if (isDragging) (dragProgress * durationMs).toLong() else positionMs
                ),
                style = MaterialTheme.typography.labelSmall,
                color = timeTint, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                modifier = Modifier.padding(start = sidePad)
            )
            Text(
                text = MediaRepository.formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = timeTint, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                modifier = Modifier.padding(end = sidePad)
            )
        }
    }
}
