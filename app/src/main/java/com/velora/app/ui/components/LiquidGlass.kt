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
import com.velora.app.ui.theme.VeloraMotion
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

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
 * Squircle math gets visually unstable for huge "pill sentinel" radii like
 * 999.dp (a common idiom to force a fully rounded pill/circle with
 * RoundedCornerShape, which clamps automatically) — cap requested radii here
 * instead. 56dp comfortably reads as a full pill/circle on every element
 * size currently used across the app (34dp–72dp).
 */
private val PixelUiMaxSquircleRadius = 56.dp

/**
 * Returns a squircle (smooth-corner) shape when Pixel UI mode is active,
 * otherwise a plain RoundedCornerShape with the same radius. Drop this in
 * anywhere a bare `RoundedCornerShape(x.dp)` was hardcoded so those surfaces
 * stay visually consistent with LiquidGlassSurface's Pixel UI squircles.
 */
@Composable
fun pixelAwareShape(cornerRadius: Dp): Shape {
    val usePixelUi = LocalUsePixelUi.current
    return if (usePixelUi) {
        val safeCornerRadius = if (cornerRadius > PixelUiMaxSquircleRadius) PixelUiMaxSquircleRadius else cornerRadius
        remember(safeCornerRadius) {
            AbsoluteSmoothCornerShape(cornerRadius = safeCornerRadius, smoothnessAsPercent = 60)
        }
    } else {
        remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    }
}

/**
 * Liquid Glass surface — frosted glass look with animated border glow.
 *
 * @param forceGlass When true, always renders in glass mode even when Pixel UI
 *   is enabled. Use this for player overlays that sit on top of media content
 *   where you always want translucency regardless of the UI style setting.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    alpha: Float = 0.18f,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 0.dp,
    forceGlass: Boolean = false,
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
    if (usePixelUi && !forceGlass) {
        // Real Pixel UI look: flat Material 3 surface using tonal
        // surface-container roles (no gradient, no translucency) clipped to
        // a squircle ("smooth corner" / superellipse) shape — same shape
        // language Pixel/PixelPlayer use instead of plain rounded rects.
        // `alpha` (originally a glass-opacity knob) is repurposed here as a
        // prominence/elevation tier so existing call sites don't need to
        // change: higher alpha == call site wanted a more "elevated" panel.
        val shape = pixelAwareShape(cornerRadius)
        val colorScheme = MaterialTheme.colorScheme
        val surfaceTone = when {
            alpha >= 0.30f -> colorScheme.surfaceContainerHighest
            alpha >= 0.22f -> colorScheme.surfaceContainerHigh
            alpha >= 0.14f -> colorScheme.surfaceContainer
            else            -> colorScheme.surfaceContainerLow
        }
        Box(
            modifier = modifier
                .clip(shape)
                .background(surfaceTone, shape)
                .border(width = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.35f), shape = shape),
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
            animation = tween(durationMillis = VeloraMotion.ambientMedium, easing = FastOutSlowInEasing),
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
        animationSpec = VeloraMotion.expressiveSpatialDefault(),
        label = "pressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.78f else 1f,
        animationSpec = VeloraMotion.effectsFast(), label = "pressAlpha"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
}

/** Bouncy spring press — used on nav items and larger buttons */
@Composable
fun Modifier.bouncePressEffect(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = VeloraMotion.expressiveSpatialSlow(),
        label = "bounceScale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Translucent surface for player controls — simpler and less shiny than LiquidGlass.
 * Uses a semi-transparent dark/light fill with a subtle border, no glow or shimmer.
 * Replaces LiquidGlassSurface in audio/video player control buttons.
 */
@Composable
fun TranslucentSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    alpha: Float = 0.18f,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(cornerRadius)
    val panelColor = if (isDark)
        Color.White.copy(alpha = (alpha * 0.9f).coerceIn(0.08f, 0.40f))
    else
        Color.Black.copy(alpha = (alpha * 0.55f).coerceIn(0.05f, 0.28f))
    val borderColor = if (isDark)
        Color.White.copy(alpha = 0.12f)
    else
        Color.White.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(panelColor)
            .then(
                Modifier.drawBehind {
                    drawIntoCanvas { canvas ->
                        val strokePaint = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.TRANSPARENT
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 1.dp.toPx()
                                this.color = android.graphics.Color.argb(
                                    (borderColor.alpha * 255).toInt(),
                                    (borderColor.red * 255).toInt(),
                                    (borderColor.green * 255).toInt(),
                                    (borderColor.blue * 255).toInt()
                                )
                            }
                        }
                        canvas.drawRoundRect(
                            0f, 0f, size.width, size.height,
                            cornerRadius.toPx(), cornerRadius.toPx(), strokePaint
                        )
                    }
                }
            ),
        content = content
    )
}
