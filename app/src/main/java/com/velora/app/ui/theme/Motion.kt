package com.velora.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Centralized motion timing, modeled on Material 3 Expressive's MotionScheme
 * concept (standard vs expressive, spatial vs effects, each with
 * fast/default/slow tiers) — Velora's Compose BOM predates the real M3
 * `MotionScheme` API, so this is a lightweight hand-rolled equivalent.
 * Applies to every theme/mode (Pixel UI, Frosted Blur, default glass) since
 * every animated composable pulls its spec from here instead of hand-tuning
 * its own duration/spring per call site.
 *
 * Categories:
 * - **Effects** — non-spatial property changes (alpha/color crossfades).
 *   Simple eased tweens, never bouncy.
 * - **Standard spatial** — position/size/scale/rotation changes that
 *   shouldn't overshoot. Critically damped springs (no bounce). This is
 *   what most press-scale, selection, and toggle feedback should use.
 * - **Expressive spatial** — same idea but with a visible bounce, reserved
 *   for emphasis moments (favorite/like bursts, big press feedback).
 * - **Ambient** — slow decorative loops (glow pulses, gradient drifts,
 *   shimmer). Intentionally *not* unified with interaction feedback above —
 *   these just centralize the magic numbers so they're named/discoverable.
 */
object VeloraMotion {

    // ── Effects motion ──────────────────────────────────────────────────
    fun <T> effectsFast(): FiniteAnimationSpec<T> =
        tween(durationMillis = 100, easing = FastOutSlowInEasing)

    fun <T> effectsDefault(): FiniteAnimationSpec<T> =
        tween(durationMillis = 200, easing = FastOutSlowInEasing)

    fun <T> effectsSlow(): FiniteAnimationSpec<T> =
        tween(durationMillis = 300, easing = FastOutSlowInEasing)

    // ── Standard spatial motion (no overshoot) ──────────────────────────
    fun <T> standardSpatialFast(): FiniteAnimationSpec<T> =
        spring(stiffness = Spring.StiffnessHigh)

    fun <T> standardSpatialDefault(): FiniteAnimationSpec<T> =
        spring(stiffness = Spring.StiffnessMedium)

    fun <T> standardSpatialSlow(): FiniteAnimationSpec<T> =
        spring(stiffness = Spring.StiffnessMediumLow)

    // ── Expressive spatial motion (visible bounce) ──────────────────────
    fun <T> expressiveSpatialFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)

    fun <T> expressiveSpatialDefault(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    fun <T> expressiveSpatialSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    // ── Ambient motion (decorative loops, intentionally untouched timing) ─
    const val ambientFast = 2200
    const val ambientMedium = 2400
    const val ambientSlow = 8200
    const val ambientSlower = 9000
    const val ambientSlowest = 11000
}
