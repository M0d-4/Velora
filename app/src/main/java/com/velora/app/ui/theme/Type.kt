package com.velora.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.velora.app.R

/**
 * App-wide typography: Google Sans Flex, bundled directly from the official
 * open release (res/font/gflex_variable.ttf, SIL OFL 1.1) with its ROND axis
 * pinned to 100 — the same trick PixelPlayer uses to get the "Google Sans
 * Rounded" look, since Google doesn't ship a separate static "Rounded"
 * family — Flex's ROND axis *is* the rounded variant, dialled in at runtime.
 *
 * Applies to every theme/mode (Pixel UI, Frosted Blur, default glass) since
 * it's set once on the root Typography passed into MaterialTheme.
 */
private const val GoogleSansFlexRond = 100f

@OptIn(ExperimentalTextApi::class)
val GoogleSansRoundedFamily = FontFamily(
    Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Light.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
    Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Normal.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
    Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Medium.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
    Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
    Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
            FontVariation.Setting("ROND", GoogleSansFlexRond)
        )
    ),
)

/**
 * Same type scale (sizes / line-heights / letter-spacing / per-role weights)
 * as Compose Material3's default Typography() baseline — only the font
 * family changes — so swapping this in doesn't reflow or overflow any
 * existing layouts that were tuned against the default metrics.
 */
val VeloraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansRoundedFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    )
)
