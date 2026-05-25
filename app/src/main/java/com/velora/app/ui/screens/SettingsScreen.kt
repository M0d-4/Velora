package com.velora.app.ui.screens

import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velora.app.ui.components.LiquidGlassSurface

const val SETTINGS_APP_VERSION = "1.1.2"

// Always-green regardless of theme primary color
private val SwitchGreen = Color(0xFF34C759)

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
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Settings", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp))

            SettingsSectionHeader("Appearance")
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.Palette, title = "Material You",
                        subtitle = if (supportsM3) "Use dynamic colors from your wallpaper" else "Requires Android 12+",
                        checked = useMaterialYou && supportsM3, enabled = supportsM3, alwaysGreen = true,
                        onCheckedChange = { if (supportsM3) onMaterialYouToggle(it) })
                }
            }

            SettingsSectionHeader("About")
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsInfoRow(Icons.Rounded.Info,      "App Version", SETTINGS_APP_VERSION)
                    SettingsDivider()
                    SettingsInfoRow(Icons.Rounded.MusicNote, "App Name",    "Velora")
                    SettingsDivider()
                    SettingsInfoRow(Icons.Rounded.Android,   "Build",       "Release")
                }
            }

            SettingsSectionHeader("Playback")
            LiquidGlassSurface(cornerRadius = 20.dp, alpha = 0.14f, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsInfoRow(Icons.Rounded.GraphicEq, "Waveform",          "Real-time audio")
                    SettingsDivider()
                    SettingsInfoRow(Icons.Rounded.Speed,     "Playback Speeds",   "0.5× – 2×")
                    SettingsDivider()
                    SettingsInfoRow(Icons.Rounded.Lyrics,    "Lyrics Formats",    ".lrc · .srt")
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, enabled: Boolean = true, alwaysGreen: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    val iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.35f))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.45f))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor          = Color.White,
                checkedTrackColor          = if (alwaysGreen) SwitchGreen else MaterialTheme.colorScheme.primary,
                uncheckedThumbColor        = Color.White,
                uncheckedTrackColor        = MaterialTheme.colorScheme.onSurface.copy(0.22f),
                disabledCheckedTrackColor  = if (alwaysGreen) SwitchGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(0.5f),
                disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(0.12f)
            ))
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, value: String) {
    val iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(0.08f))
}
