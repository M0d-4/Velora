package com.velora.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val GlassWhite      = Color(0xCCFFFFFF)
val GlassWhiteLight = Color(0x55FFFFFF)
val GlassBorder     = Color(0x40FFFFFF)
val GlassShadow     = Color(0x22000000)

val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF9ECAFF),
    secondary        = Color(0xFFBBC7DB),
    tertiary         = Color(0xFFD8BEF8),
    background       = Color(0xFF0E1116),
    surface          = Color(0xFF1A1F27),
    surfaceVariant   = Color(0xFF2A303C),
    onPrimary        = Color(0xFF003061),
    onSecondary      = Color(0xFF253240),
    onBackground     = Color(0xFFE2E8F4),
    onSurface        = Color(0xFFE2E8F4),
)

val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF0061A4),
    secondary        = Color(0xFF546E8A),
    tertiary         = Color(0xFF6B548E),
    background       = Color(0xFFF5F9FF),
    surface          = Color(0xFFFFFFFF),
    surfaceVariant   = Color(0xFFDDE3EF),
    onPrimary        = Color(0xFFFFFFFF),
    onSecondary      = Color(0xFFFFFFFF),
    onBackground     = Color(0xFF181C22),
    onSurface        = Color(0xFF181C22),
)

@Composable
fun VeloraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useMaterialYou: Boolean = true,
    /**
     * Per-track Material You scheme generated from the current album art
     * (see ui/theme/ColorRoles.kt). When non-null this takes priority over
     * both system dynamic color and the static palettes — this is what
     * drives Pixel UI mode's per-song theming.
     */
    albumArtScheme: VeloraColorSchemePair? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        albumArtScheme != null -> if (darkTheme) albumArtScheme.dark else albumArtScheme.light
        useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val ctrl = WindowCompat.getInsetsController(window, view)
            ctrl.isAppearanceLightStatusBars    = !darkTheme
            ctrl.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = VeloraTypography, content = content)
}
