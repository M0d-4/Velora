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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass surface with animated shimmer highlight and glowing border.
 * Matches iOS 26 frosted-glass / Android Material You aesthetic.
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

    // Animate a slow shimmer sweep across the surface
    val shimmerAnim = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerAnim.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Animate a slow border pulse glow
    val borderPulse by shimmerAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderPulse"
    )

    val glassBase = if (isDark) alpha else (alpha + 0.22f)
    val glassColor = Color.White.copy(alpha = glassBase)
    val glassTop  = Color.White.copy(alpha = (glassBase * 1.6f).coerceAtMost(1f))

    val borderBase = if (isDark) borderAlpha else (borderAlpha + 0.18f)
    val borderAnimated = borderBase + borderPulse * 0.15f

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .liquidGlowShadow(cornerRadius = cornerRadius, isDark = isDark)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(glassTop, glassColor)
                )
            )
            // Shimmer sweep highlight drawn on top of the background
            .drawWithContent {
                drawContent()
                val sweepX = shimmerOffset * size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isDark) 0.06f else 0.10f),
                            Color.White.copy(alpha = if (isDark) 0.10f else 0.16f),
                            Color.White.copy(alpha = if (isDark) 0.06f else 0.10f),
                            Color.Transparent
                        ),
                        start = Offset(sweepX - size.width * 0.3f, 0f),
                        end   = Offset(sweepX + size.width * 0.3f, size.height)
                    )
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAnimated),
                        Color.White.copy(alpha = (borderAnimated * 0.28f).coerceAtMost(1f))
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

fun Modifier.liquidGlowShadow(
    cornerRadius: Dp,
    isDark: Boolean
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    if (isDark) 28f else 18f,
                    0f,
                    if (isDark) 10f else 5f,
                    if (isDark)
                        android.graphics.Color.argb(70, 0, 0, 0)
                    else
                        android.graphics.Color.argb(35, 0, 0, 0)
                )
            }
        }
        canvas.drawRoundRect(
            left = 0f, top = 0f,
            right = size.width, bottom = size.height,
            radiusX = cornerRadius.toPx(),
            radiusY = cornerRadius.toPx(),
            paint = paint
        )
    }
}

/**
 * Ripple-style liquid press animation modifier.
 * Wraps content with a scale+alpha press-down feel.
 */
@Composable
fun Modifier.liquidPressEffect(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.80f else 1f,
        animationSpec = tween(80),
        label = "pressAlpha"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
}
