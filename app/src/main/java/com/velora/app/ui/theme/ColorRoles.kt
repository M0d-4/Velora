package com.velora.app.ui.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.scale
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.SchemeTonalSpot

/**
 * Light + dark Material color schemes generated from a single seed color.
 * Mirrors PixelPlayer's ColorSchemePair — VeloraTheme picks whichever half
 * matches the active dark/light state.
 */
data class VeloraColorSchemePair(val light: ColorScheme, val dark: ColorScheme)

private const val SEED_EXTRACTION_MAX_DIMENSION = 112
private val FALLBACK_SEED = Color(0xFF6750A4) // Material baseline seed

/**
 * Picks a representative seed color from album art using Google's own
 * QuantizerCelebi (the same pixel-quantization algorithm Android's system
 * wallpaper-based Monet theming uses), then biases the ranking towards
 * colors with real chroma so the seed isn't just "whatever shade of grey
 * covers the most pixels" on busy/dark covers.
 */
fun extractSeedColor(bitmap: Bitmap): Color {
    return runCatching {
        val working = if (bitmap.width > SEED_EXTRACTION_MAX_DIMENSION || bitmap.height > SEED_EXTRACTION_MAX_DIMENSION) {
            val factor = SEED_EXTRACTION_MAX_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height)
            bitmap.scale(
                (bitmap.width * factor).toInt().coerceAtLeast(1),
                (bitmap.height * factor).toInt().coerceAtLeast(1)
            )
        } else bitmap

        val pixels = IntArray(working.width * working.height)
        working.getPixels(pixels, 0, working.width, 0, 0, working.width, working.height)

        Color(selectSeedArgb(pixels))
    }.getOrElse { FALLBACK_SEED }
}

private fun selectSeedArgb(pixels: IntArray): Int {
    val quantized = QuantizerCelebi.quantize(pixels, 128)
    if (quantized.isEmpty()) return FALLBACK_SEED.toArgb()

    val ranked = quantized.entries.maxByOrNull { (argb, population) ->
        val chroma = Hct.fromInt(argb).chroma.coerceIn(0.0, 100.0)
        // Weight by population but reward saturation, so a small patch of
        // vivid color can outrank a large flat near-black/white background.
        population * (1.0 + chroma / 40.0)
    }
    return ranked?.key ?: quantized.keys.first()
}

/**
 * Builds a full light + dark Material ColorScheme from a seed color using
 * the same HCT/DynamicScheme machinery Android's system Monet engine and
 * PixelPlayer use — SchemeTonalSpot is the same algorithm behind stock
 * wallpaper-based dynamic color.
 */
fun generateColorSchemeFromSeed(seed: Color): VeloraColorSchemePair {
    return runCatching {
        val sourceHct = Hct.fromInt(seed.toArgb())
        val light = SchemeTonalSpot(sourceHct, false, 0.0)
        val dark = SchemeTonalSpot(sourceHct, true, 0.0)
        VeloraColorSchemePair(
            light = light.toComposeColorScheme(dark = false),
            dark = dark.toComposeColorScheme(dark = true)
        )
    }.getOrElse {
        VeloraColorSchemePair(light = LightColorScheme, dark = DarkColorScheme)
    }
}

private val materialDynamicColors = MaterialDynamicColors()

private fun DynamicScheme.toComposeColorScheme(dark: Boolean): ColorScheme {
    val mdc = materialDynamicColors
    val primary = Color(mdc.primary().getArgb(this))
    val onPrimary = Color(mdc.onPrimary().getArgb(this))
    val primaryContainer = Color(mdc.primaryContainer().getArgb(this))
    val onPrimaryContainer = Color(mdc.onPrimaryContainer().getArgb(this))
    val secondary = Color(mdc.secondary().getArgb(this))
    val onSecondary = Color(mdc.onSecondary().getArgb(this))
    val secondaryContainer = Color(mdc.secondaryContainer().getArgb(this))
    val onSecondaryContainer = Color(mdc.onSecondaryContainer().getArgb(this))
    val tertiary = Color(mdc.tertiary().getArgb(this))
    val onTertiary = Color(mdc.onTertiary().getArgb(this))
    val tertiaryContainer = Color(mdc.tertiaryContainer().getArgb(this))
    val onTertiaryContainer = Color(mdc.onTertiaryContainer().getArgb(this))
    val background = Color(mdc.background().getArgb(this))
    val onBackground = Color(mdc.onBackground().getArgb(this))
    val surface = Color(mdc.surface().getArgb(this))
    val onSurface = Color(mdc.onSurface().getArgb(this))
    val surfaceVariant = Color(mdc.surfaceVariant().getArgb(this))
    val onSurfaceVariant = Color(mdc.onSurfaceVariant().getArgb(this))
    val outline = Color(mdc.outline().getArgb(this))
    val outlineVariant = Color(mdc.outlineVariant().getArgb(this))
    val error = Color(mdc.error().getArgb(this))
    val onError = Color(mdc.onError().getArgb(this))
    val errorContainer = Color(mdc.errorContainer().getArgb(this))
    val onErrorContainer = Color(mdc.onErrorContainer().getArgb(this))
    val inverseSurface = Color(mdc.inverseSurface().getArgb(this))
    val inverseOnSurface = Color(mdc.inverseOnSurface().getArgb(this))
    val inversePrimary = Color(mdc.inversePrimary().getArgb(this))
    val scrim = Color(mdc.scrim().getArgb(this))

    return if (dark) {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            outline = outline, outlineVariant = outlineVariant,
            error = error, onError = onError,
            errorContainer = errorContainer, onErrorContainer = onErrorContainer,
            inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary, scrim = scrim,
            surfaceTint = primary
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            outline = outline, outlineVariant = outlineVariant,
            error = error, onError = onError,
            errorContainer = errorContainer, onErrorContainer = onErrorContainer,
            inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary, scrim = scrim,
            surfaceTint = primary
        )
    }
}

/**
 * Loads the current track's artwork via Coil, extracts a seed color, and
 * generates a [VeloraColorSchemePair] from it — recomputed whenever the art
 * changes. Returns null when disabled or there's no art, so callers can fall
 * back to system Material You / static palettes.
 *
 * This is intentionally separate from the existing Palette-based
 * `rememberCoverArtColors` (used for in-player gradient backgrounds) — this
 * one drives the *global* MaterialTheme color scheme for Pixel UI mode.
 */
@Composable
fun rememberAlbumArtColorScheme(artUri: Uri?, enabled: Boolean): VeloraColorSchemePair? {
    val context = LocalContext.current
    var scheme by remember { mutableStateOf<VeloraColorSchemePair?>(null) }

    LaunchedEffect(artUri, enabled) {
        if (!enabled || artUri == null) {
            scheme = null
            return@LaunchedEffect
        }
        val nextScheme = runCatching {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(artUri).allowHardware(false).build()
            val result = loader.execute(request)
            val bitmap = (result as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap }
            bitmap?.let { generateColorSchemeFromSeed(extractSeedColor(it)) }
        }.getOrNull()
        if (nextScheme != null) scheme = nextScheme
    }

    return scheme
}
