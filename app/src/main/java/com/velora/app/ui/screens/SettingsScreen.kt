package com.velora.app.ui.screens

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velora.app.ui.components.LiquidGlassSurface
import com.velora.app.ui.components.pixelAwareShape
import com.velora.app.ui.theme.VeloraMotion

const val SETTINGS_APP_VERSION = "1.1.2"

private val SwitchGreen = Color(0xFF34C759)

@Composable
fun SettingsScreen(
    useMaterialYou: Boolean,
    onMaterialYouToggle: (Boolean) -> Unit,
    usePixelUi: Boolean = false,
    onPixelUiToggle: (Boolean) -> Unit = {},
    useFrostedBlur: Boolean = false,
    onFrostedBlurToggle: (Boolean) -> Unit = {},
    centerLyrics: Boolean = true,
    onCenterLyricsToggle: (Boolean) -> Unit = {},
    useWhiteIcons: Boolean = false,
    onWhiteIconsToggle: (Boolean) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val supportsM3 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Mutual exclusion: if Pixel UI is on, Frosted Blur is disabled (greyed). Vice versa.
    val frostedBlurEnabled = !usePixelUi
    val pixelUiEnabled = !useFrostedBlur

    Box(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header ────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.Close, "Close Settings", modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                    }
                }
            }

            // ── Appearance ────────────────────────────────────────────────
            SettingsSectionHeader("Appearance")
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Material You
                    DraggableToggleRow(
                        icon     = Icons.Rounded.Palette,
                        title    = "Material You",
                        subtitle = if (supportsM3) "Use dynamic colors from your wallpaper" else "Requires Android 12+",
                        checked  = useMaterialYou && supportsM3,
                        enabled  = supportsM3,
                        onCheckedChange = { if (supportsM3) onMaterialYouToggle(it) }
                    )
                    SettingsDivider()
                    // Pixel UI — greyed out when Frosted Blur is active
                    DraggableToggleRow(
                        icon     = Icons.Rounded.PhoneAndroid,
                        title    = "Pixel UI",
                        subtitle = if (!pixelUiEnabled) "Disabled while Frosted Blur is on"
                                   else "Clean flat Material style, disables Liquid Glass",
                        checked  = usePixelUi,
                        enabled  = pixelUiEnabled,
                        onCheckedChange = { if (pixelUiEnabled) onPixelUiToggle(it) }
                    )
                    SettingsDivider()
                    // Frosted Blur — greyed out when Pixel UI is active
                    DraggableToggleRow(
                        icon     = Icons.Rounded.BlurOn,
                        title    = "Frosted Blur",
                        subtitle = if (!frostedBlurEnabled) "Disabled while Pixel UI is on"
                                   else "Frosted glass panels, disables Liquid Glass",
                        checked  = useFrostedBlur,
                        enabled  = frostedBlurEnabled,
                        onCheckedChange = { if (frostedBlurEnabled) onFrostedBlurToggle(it) }
                    )
                }
            }

            // ── Lyrics ────────────────────────────────────────────────────
            SettingsSectionHeader("Lyrics")
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    DraggableToggleRow(
                        icon     = Icons.Rounded.Lyrics,
                        title    = "Center & Highlight",
                        subtitle = if (centerLyrics) "Active lyric centered and enlarged"
                                   else "All lyrics shown at equal size, no auto-scroll",
                        checked  = centerLyrics,
                        onCheckedChange = { onCenterLyricsToggle(it) }
                    )
                }
            }

            // ── Player ────────────────────────────────────────────────────
            SettingsSectionHeader("Player")
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    DraggableToggleRow(
                        icon     = Icons.Rounded.Brightness7,
                        title    = "White Icons",
                        subtitle = "Use white instead of accent colour for player icons and text",
                        checked  = useWhiteIcons,
                        onCheckedChange = { onWhiteIconsToggle(it) }
                    )
                }
            }

            // ── Info ──────────────────────────────────────────────────────
            SettingsSectionHeader("Info")
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsInfoRow(Icons.Rounded.Lyrics,     "Lyrics Formats", ".lrc · .srt")
                    SettingsDivider()
                    SettingsInfoRow(Icons.Rounded.AudioFile,  "Audio Formats",  "mp3 · flac · ogg · m4a · aac · wav")
                    SettingsDivider()
                    SettingsInfoRow(Icons.Rounded.VideoFile,  "Video Formats",  "mp4 · mkv · avi · webm")
                }
            }

            // ── Version stamp ─────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Velora $SETTINGS_APP_VERSION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}

/**
 * Toggle row where:
 * • Tapping anywhere in the row toggles the switch.
 * • Dragging a finger horizontally over the switch area also toggles it —
 *   drag right = turn on, drag left = turn off.
 */
@Composable
internal fun DraggableToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    var dragConsumed by remember { mutableStateOf(false) }
    var rowPressed by remember { mutableStateOf(false) }
    val rowScale by animateFloatAsState(
        targetValue = if (rowPressed) 0.97f else 1f,
        animationSpec = VeloraMotion.standardSpatialSlow(),
        label = "rowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
            .pointerInput(checked, enabled) {
                detectDragGestures(
                    onDragStart  = { dragConsumed = false; rowPressed = true },
                    onDragEnd    = { rowPressed = false },
                    onDragCancel = { rowPressed = false },
                    onDrag = { _, dragAmount ->
                        if (!dragConsumed && enabled) {
                            val dx = dragAmount.x
                            if (dx > 40f && !checked)  { onCheckedChange(true);  dragConsumed = true }
                            else if (dx < -40f && checked) { onCheckedChange(false); dragConsumed = true }
                        }
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.size(40.dp).clip(pixelAwareShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(0.35f))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(0.45f))
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
        }
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor            = Color.White,
                checkedTrackColor            = SwitchGreen,
                checkedBorderColor           = SwitchGreen,
                uncheckedThumbColor          = Color.White,
                uncheckedTrackColor          = MaterialTheme.colorScheme.onSurface.copy(0.22f),
                uncheckedBorderColor         = MaterialTheme.colorScheme.onSurface.copy(0.22f),
                disabledCheckedTrackColor    = SwitchGreen.copy(alpha = 0.5f),
                disabledCheckedBorderColor   = SwitchGreen.copy(alpha = 0.5f),
                disabledUncheckedTrackColor  = MaterialTheme.colorScheme.onSurface.copy(0.12f),
                disabledUncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.12f)
            )
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, value: String) {
    val iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(pixelAwareShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(0.55f),
            modifier = Modifier.widthIn(max = 180.dp))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(0.08f))
}
