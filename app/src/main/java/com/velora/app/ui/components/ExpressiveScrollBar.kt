package com.velora.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velora.app.ui.theme.VeloraMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One section breakpoint: a label (usually a letter) and the index of its first item. */
data class ScrollBarSection(val label: String, val firstIndex: Int)

/**
 * Builds section breakpoints (first letter -> first item index) from a list,
 * in display order. "#" is used for items that don't start with a letter.
 */
fun <T> buildScrollBarSections(items: List<T>, labelOf: (T) -> String): List<ScrollBarSection> {
    val sections = mutableListOf<ScrollBarSection>()
    var lastLetter: String? = null
    items.forEachIndexed { index, item ->
        val raw = labelOf(item).trim()
        val firstChar = raw.firstOrNull()
        val letter = if (firstChar != null && firstChar.isLetter()) firstChar.uppercaseChar().toString() else "#"
        if (letter != lastLetter) {
            sections.add(ScrollBarSection(letter, index))
            lastLetter = letter
        }
    }
    return sections
}

/**
 * Fast-scroll index for long lists, in the style of Material 3 Expressive /
 * PixelPlayer's ExpressiveScrollBar — two interaction modes on one strip:
 *
 * - **Drag normally**: continuous, fine-grained scrubbing. Scrolls the list
 *   to any proportional position, not snapped to letters.
 * - **Press and hold**: after a short delay, switches to letter-jump mode —
 *   your touch position snaps to the nearest section letter and the list
 *   jumps straight to that section, with a big floating letter bubble next
 *   to your finger.
 *
 * Releasing returns the strip to its resting state either way.
 */
@Composable
fun ExpressiveScrollBar(
    listState: LazyListState,
    sections: List<ScrollBarSection>,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (sections.isEmpty() || itemCount == 0) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var trackHeightPx by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isLetterMode by remember { mutableStateOf(false) }
    var activeLabel by remember { mutableStateOf<String?>(null) }
    var bubbleYPx by remember { mutableStateOf(0f) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    val railAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.5f,
        animationSpec = VeloraMotion.effectsDefault(), label = "scrollbarAlpha"
    )
    val bubbleScale by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.5f,
        animationSpec = VeloraMotion.expressiveSpatialDefault(), label = "scrollbarBubbleScale"
    )

    fun jumpTo(yPx: Float) {
        if (trackHeightPx <= 0f) return
        val progress = (yPx / trackHeightPx).coerceIn(0f, 1f)
        if (isLetterMode) {
            // Letter-snap mode: quantize to the nearest section.
            val sectionIndex = (progress * sections.size).toInt().coerceIn(0, sections.size - 1)
            val section = sections[sectionIndex]
            activeLabel = section.label
            scope.launch { listState.scrollToItem(section.firstIndex) }
        } else {
            // Continuous mode: scroll to any point proportionally.
            val targetIndex = (progress * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
            activeLabel = sections.lastOrNull { it.firstIndex <= targetIndex }?.label ?: sections.first().label
            scope.launch { listState.scrollToItem(targetIndex) }
        }
        bubbleYPx = yPx
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(20.dp)
            .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
            .pointerInput(sections, itemCount) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        isLetterMode = false
                        longPressJob?.cancel()
                        longPressJob = scope.launch {
                            delay(320L)
                            isLetterMode = true
                        }
                        jumpTo(offset.y)
                    },
                    onDrag = { change, _ -> jumpTo(change.position.y) },
                    onDragEnd = {
                        isDragging = false
                        isLetterMode = false
                        longPressJob?.cancel()
                    },
                    onDragCancel = {
                        isDragging = false
                        isLetterMode = false
                        longPressJob?.cancel()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // The letter rail itself — always fills height, letters spread evenly
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 2.dp)
                .alpha(railAlpha),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            sections.forEach { section ->
                val isActive = isDragging && section.label == activeLabel
                Text(
                    text = section.label,
                    fontSize = if (isActive) 9.sp else 7.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Floating section-letter bubble shown while actively dragging
        if (isDragging && activeLabel != null) {
            val bubbleShape = pixelAwareShape(20.dp)
            Box(
                modifier = Modifier
                    .offset(
                        x = (-48).dp,
                        y = with(density) { bubbleYPx.toDp() } - 28.dp
                    )
                    .size(56.dp)
                    .graphicsLayer { scaleX = bubbleScale; scaleY = bubbleScale }
                    .clip(bubbleShape)
                    .background(MaterialTheme.colorScheme.primary, bubbleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    activeLabel ?: "",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
