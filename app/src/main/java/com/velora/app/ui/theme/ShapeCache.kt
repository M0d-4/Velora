package com.velora.app.ui.theme

import androidx.compose.ui.unit.dp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Cached "squircle" (continuous-corner / superellipse) shapes used by Pixel
 * UI mode. AbsoluteSmoothCornerShape computes its corners analytically
 * (cubic Bézier), which is noticeably more expensive than RoundedCornerShape
 * — reuse singleton instances for the handful of common radii rather than
 * recomputing the path on every recomposition.
 *
 * Usage:
 *   Modifier.clip(ShapeCache.smooth16)
 *   Modifier.background(color, ShapeCache.smooth16)
 */
object ShapeCache {
    /** 8dp smooth corners — chips, small surfaces */
    val smooth8 = AbsoluteSmoothCornerShape(cornerRadius = 8.dp, smoothnessAsPercent = 60)

    /** 12dp smooth corners — list items, small cards */
    val smooth12 = AbsoluteSmoothCornerShape(cornerRadius = 12.dp, smoothnessAsPercent = 60)

    /** 16dp smooth corners — album art, playlist items */
    val smooth16 = AbsoluteSmoothCornerShape(cornerRadius = 16.dp, smoothnessAsPercent = 60)

    /** 20dp smooth corners — larger cards */
    val smooth20 = AbsoluteSmoothCornerShape(cornerRadius = 20.dp, smoothnessAsPercent = 60)

    /** 24dp smooth corners — dialog surfaces */
    val smooth24 = AbsoluteSmoothCornerShape(cornerRadius = 24.dp, smoothnessAsPercent = 60)

    /** 32dp smooth corners — bottom sheets, floating panels */
    val smooth32 = AbsoluteSmoothCornerShape(cornerRadius = 32.dp, smoothnessAsPercent = 60)

    /** Fully smooth (pill) — used for buttons and chips */
    val smoothPill = AbsoluteSmoothCornerShape(cornerRadius = 50.dp, smoothnessAsPercent = 60)
}
