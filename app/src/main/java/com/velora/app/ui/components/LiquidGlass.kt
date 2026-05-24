package com.velora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass surface — frosted glass look with animated border glow.
 * No shimmer sweep (removed per user request).
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    alpha: Float = 0.18f,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Animate a slow border pulse glow only (no moving shimmer)
    val borderAnim = rememberInfiniteTransition(label = "borderPulse")
    val borderPulse by borderAnim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "borderPulse"
    )

    val glassBase = if (isDark) alpha else (alpha + 0.22f)
    val glassColor = Color.White.copy(alpha = glassBase)
    val glassTop   = Color.White.copy(alpha = (glassBase * 1.6f).coerceAtMost(1f))

    val borderBase     = if (isDark) borderAlpha else (borderAlpha + 0.18f)
    val borderAnimated = borderBase + borderPulse * 0.12f

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .liquidGlowShadow(cornerRadius = cornerRadius, isDark = isDark)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(glassTop, glassColor)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(
                    Color.White.copy(alpha = borderAnimated),
                    Color.White.copy(alpha = (borderAnimated * 0.28f).coerceAtMost(1f))
                )),
                shape = shape
            ),
        content = content
    )
}

fun Modifier.liquidGlowShadow(cornerRadius: Dp, isDark: Boolean): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    if (isDark) 28f else 18f, 0f, if (isDark) 10f else 5f,
                    if (isDark) android.graphics.Color.argb(70, 0, 0, 0)
                    else        android.graphics.Color.argb(35, 0, 0, 0)
                )
            }
        }
        canvas.drawRoundRect(0f, 0f, size.width, size.height,
            cornerRadius.toPx(), cornerRadius.toPx(), paint)
    }
}

@Composable
fun Modifier.liquidPressEffect(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.80f else 1f,
        animationSpec = tween(80), label = "pressAlpha"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
}
