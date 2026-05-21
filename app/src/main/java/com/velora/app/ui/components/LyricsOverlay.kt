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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velora.app.model.LyricLine
import kotlinx.coroutines.launch

/**
 * Lyrics display — no background, transparent, uses the system font.
 * Active line is bright and larger; surrounding lines are dimmer.
 * Auto-scrolls to keep the active line visible.
 */
@Composable
fun LyricsOverlay(
    lyrics: List<LyricLine>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) return

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
                isActive -> 1f
                distance == 1 -> 0.55f
                distance == 2 -> 0.35f
                else -> 0.2f
            }

            val fontSize = if (isActive) 18.sp else 15.sp
            val fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal

            AnimatedContent(
                targetState = isActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
                },
                label = "lyric_$index"
            ) {
                Text(
                    text = line.text,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Center,
                    // No fontFamily — inherits system font via MaterialTheme.typography
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
