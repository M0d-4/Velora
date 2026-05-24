package com.velora.app.ui.screens

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velora.app.ui.components.LiquidGlassSurface
import com.velora.app.ui.screens.AnimatedBackground

const val SETTINGS_APP_VERSION = "1.1.1"

@Composable
fun SettingsScreen(
    useMaterialYou: Boolean,
    onMaterialYouToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val supportsM3 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(56.dp))

            // Page title
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // ── Appearance section ─────────────────────────────────────────────
            SettingsSectionHeader("Appearance")

            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.Palette,
                        title = "Material You",
                        subtitle = if (supportsM3)
                            "Use dynamic colors from your wallpaper"
                        else
                            "Requires Android 12+",
                        checked = useMaterialYou && supportsM3,
                        enabled = supportsM3,
                        onCheckedChange = { if (supportsM3) onMaterialYouToggle(it) }
                    )
                }
            }

            // ── About section ──────────────────────────────────────────────────
            SettingsSectionHeader("About")

            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsInfoRow(
                        icon = Icons.Rounded.Info,
                        title = "App Version",
                        value = SETTINGS_APP_VERSION
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Rounded.MusicNote,
                        title = "App Name",
                        value = "Velora"
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Rounded.Android,
                        title = "Build",
                        value = "Release"
                    )
                }
            }

            // ── Playback section ───────────────────────────────────────────────
            SettingsSectionHeader("Playback")

            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsInfoRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Waveform",
                        value = "Real-time audio"
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Rounded.Speed,
                        title = "Playback Speeds",
                        value = "0.5× – 2×"
                    )
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

// ── Section header ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

// ── Toggle row ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
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
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.22f)
            )
        )
    }
}

// ── Info row ───────────────────────────────────────────────────────────────────

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
    }
}

// ── Divider ────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(0.08f)
    )
}
