package com.velora.app.ui.components

import android.os.Build
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

/** When true, all LiquidGlass surfaces render as flat Pixel-style Material surfaces. */
val LocalUsePixelUi = compositionLocalOf { false }

/** When true, all LiquidGlass surfaces render as frosted-blur panels (Pixel UI must be off). */
val LocalUseFrostedBlur = compositionLocalOf { false }

/**
 * When true, player icons/text use Color.White instead of primary colour.
 * Only affects audio and video player composables that read this local.
 */
val LocalUseWhiteIcons = compositionLocalOf { false }

/**
 * Liquid Glass surface — frosted glass look with animated border glow.
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
    val usePixelUi = LocalUsePixelUi.current
    val useFrostedBlur = LocalUseFrostedBlur.current

    // ── Frosted blur mode ─────────────────────────────────────────────────────
    if (useFrostedBlur && !usePixelUi) {
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val shape = RoundedCornerShape(cornerRadius)
        val panelAlpha = if (isDark) (alpha * 2.2f).coerceIn(0.18f, 0.55f)
                         else        (alpha * 2.5f + 0.20f).coerceIn(0.30f, 0.72f)
        val panelColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = panelAlpha)
                         else        Color(0xFFFFFFFF).copy(alpha = panelAlpha)
        val borderColor = if (isDark) Color.White.copy(alpha = 0.18f)
                          else        Color.White.copy(alpha = 0.55f)
        Box(
            modifier = modifier
                .clip(shape)
                .background(panelColor)
                .border(width = 0.8.dp, color = borderColor, shape = shape),
            content = content
        )
        return
    }

    // ── Pixel UI mode ─────────────────────────────────────────────────────────
    if (usePixelUi) {
        // Flat Google Pixel style: solid surface colour with a very subtle primary tint.
        // No border, no glow, no glass effect — matches Material You / Pixel launcher aesthetic.
        val shape = RoundedCornerShape(cornerRadius)
        val surface = MaterialTheme.colorScheme.surfaceVariant
        val primary = MaterialTheme.colorScheme.primary
        // Higher alpha = slightly more tinted, but always fully opaque
        val tintStrength = (alpha * 0.55f).coerceIn(0.04f, 0.28f)
        val bg = lerp(surface, primary, tintStrength)
        Box(
            modifier = modifier
                .clip(shape)
                .background(bg, shape),
            content = content
        )
        return
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Animate a slow border pulse glow only (no moving shimmer)
    val borderAnim = rememberInfiniteTransition(label = "borderPulse")
    val borderPulse by borderAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
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
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.78f else 1f,
        animationSpec = tween(60), label = "pressAlpha"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
}

/** Bouncy spring press — used on nav items and larger buttons */
@Composable
fun Modifier.bouncePressEffect(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bounceScale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
