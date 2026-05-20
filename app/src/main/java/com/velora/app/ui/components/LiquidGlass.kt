package com.velora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurMaskFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass surface — translucent frosted card matching iOS 26 / Android Material You aesthetic
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
    val glassColor = if (isDark)
        Color.White.copy(alpha = alpha)
    else
        Color.White.copy(alpha = alpha + 0.25f)

    val borderColor = if (isDark)
        Color.White.copy(alpha = borderAlpha)
    else
        Color.White.copy(alpha = borderAlpha + 0.2f)

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .liquidGlowShadow(cornerRadius = cornerRadius, isDark = isDark)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glassColor.copy(alpha = glassColor.alpha * 1.5f),
                        glassColor
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = borderColor.alpha * 0.3f)
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
                    if (isDark) 24f else 16f,
                    0f,
                    if (isDark) 8f else 4f,
                    if (isDark)
                        android.graphics.Color.argb(60, 0, 0, 0)
                    else
                        android.graphics.Color.argb(30, 0, 0, 0)
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
