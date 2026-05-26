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
import kotlinx.coroutines.launch

/**
 * Lyrics display.
 *
 * [singleLine] = true  → landscape audio mode: one line at a time, slides left→right.
 * [singleLine] = false → full scrolling list (portrait / video).
 *
 * Active line always slides in from the right (left→right direction).
 */
@Composable
fun LyricsOverlay(
    lyrics: List<LyricLine>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    if (lyrics.isEmpty()) return

    if (singleLine) {
        // Landscape: one line at a time, animated left→right
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = activeIndex,
                transitionSpec = {
                    // Slide in from right → exits to left (left-to-right reading motion)
                    (slideInHorizontally { it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally { -it / 2 } + fadeOut(tween(200)))
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
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }
        }
    } else {
        // Portrait / video: scrolling list, active line larger, others fade
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0) {
                scope.launch {
                    listState.animateScrollToItem(
                        index = (activeIndex - 1).coerceAtLeast(0),
                        scrollOffset = 0
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
                val distance = (index - activeIndex).let { if (it < 0) -it else it }

                val textAlpha = when {
                    isActive   -> 1f
                    distance == 1 -> 0.55f
                    distance == 2 -> 0.35f
                    else          -> 0.2f
                }
                val fontSize   = if (isActive) 18.sp else 15.sp
                val fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal

                // Each line slides in from right (left→right) when becoming active
                AnimatedContent(
                    targetState = isActive,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally { it / 3 } + fadeIn(tween(300))) togetherWith
                            fadeOut(tween(200))
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
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize,
                            fontWeight = fontWeight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
