package com.velora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velora.app.model.LyricLine

enum class LyricsSlideDirection { LEFT_TO_RIGHT, UP }

/**
 * [singleLine] = true  → landscape audio: one line at a time, slides left→right slowly.
 * [singleLine] = false → full scrolling list.
 * [slideDirection] = UP → each new line slides upward (video player style).
 * [slideDirection] = LEFT_TO_RIGHT → line slides in from right (audio portrait style).
 */
@Composable
fun LyricsOverlay(
    lyrics: List<LyricLine>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    slideDirection: LyricsSlideDirection = LyricsSlideDirection.LEFT_TO_RIGHT
) {
    if (lyrics.isEmpty()) return

    if (singleLine) {
        // Landscape audio: one line at a time, slow left→right slide
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = activeIndex,
                transitionSpec = {
                    // Enter from left, exit to right  
                    (slideInHorizontally(tween(600)) { -it / 2 } + fadeIn(tween(600))) togetherWith
                    (slideOutHorizontally(tween(400)) { it / 2 } + fadeOut(tween(300)))
                },
                label = "lyric_single"
            ) { idx ->
                val line = lyrics.getOrNull(idx)
                if (line != null) {
                    Text(
                        text = line.text,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }
        }
    } else {
        val listState = rememberLazyListState()

        // Scroll so the active lyric line appears centered in the visible area
        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0) {
                val visibleInfo = listState.layoutInfo
                val viewportHeight = visibleInfo.viewportEndOffset - visibleInfo.viewportStartOffset
                val itemInfo = visibleInfo.visibleItemsInfo.firstOrNull { it.index == activeIndex }
                if (itemInfo != null) {
                    // Item already visible — scroll so it's centered
                    val offset = itemInfo.offset - (viewportHeight / 2) + (itemInfo.size / 2)
                    listState.animateScrollToItem(
                        index = activeIndex,
                        scrollOffset = -(viewportHeight / 2 - itemInfo.size / 2)
                    )
                } else {
                    // Item not yet visible — jump to it with centering offset
                    listState.animateScrollToItem(
                        index = activeIndex,
                        scrollOffset = -viewportHeight / 2
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isActive = index == activeIndex
                val distance = kotlin.math.abs(index - activeIndex)

                val textAlpha = when {
                    isActive      -> 1f
                    distance == 1 -> 0.55f
                    distance == 2 -> 0.35f
                    else          -> 0.2f
                }
                val fontSize   = if (isActive) 18.sp else 15.sp
                val fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal

                AnimatedContent(
                    targetState = isActive,
                    transitionSpec = {
                        if (targetState) {
                            when (slideDirection) {
                                LyricsSlideDirection.UP ->
                                    (slideInVertically(tween(350)) { it / 2 } + fadeIn(tween(300))) togetherWith
                                    (slideOutVertically(tween(250)) { -it / 2 } + fadeOut(tween(200)))
                                LyricsSlideDirection.LEFT_TO_RIGHT ->
                                    // Line enters from left (positive = enters from right, negative = from left)
                                    (slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(300))) togetherWith
                                    (slideOutHorizontally(tween(250)) { it / 3 } + fadeOut(tween(200)))
                            }
                        } else {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                        }
                    },
                    label = "lyric_$index"
                ) {
                    Text(
                        text = line.text,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
