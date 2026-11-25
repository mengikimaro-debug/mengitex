package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Matte Carbon & Soft Graphite Dark Palette (Zero Neon Glare, Eye-Comfortable)
val ObsidianBg = Color(0xFF111214)
val ObsidianSurface = Color(0xFF181A1E)
val ObsidianSurfaceVariant = Color(0xFF22252B)
val ObsidianSurfaceElevated = Color(0xFF2B2F37)
val ObsidianOutline = Color(0xFF363B45)

val EmeraldPrimary = Color(0xFF3D5A73)
val EmeraldOnPrimary = Color(0xFFF3F1EC)
val EmeraldContainer = Color(0xFF1F2B38)
val EmeraldOnContainer = Color(0xFFD6E2EE)

val CyberCyan = Color(0xFF6E9E95)
val CyberCyanContainer = Color(0xFF1A2C29)
val CyberCyanOnContainer = Color(0xFFC8E2DC)

val ShieldAmber = Color(0xFFC8A055)
val ShieldAmberContainer = Color(0xFF2D2414)
val ShieldAmberOnContainer = Color(0xFFEBDCBA)

val AlertCrimson = Color(0xFFC96F6F)
val AlertCrimsonContainer = Color(0xFF311B1D)
val AlertCrimsonOnContainer = Color(0xFFF0D0D0)

val SoftTextPrimary = Color(0xFFECEAE4)
val SoftTextSecondary = Color(0xFFA0A4AB)

// Warm Matte Off-White Light Palette (Zero Glare, Zero Neon Glow)
val LightBg = Color(0xFFF3F1EC)
val LightSurface = Color(0xFFFAF8F4)
val LightSurfaceVariant = Color(0xFFEBE8E0)
val LightSurfaceElevated = Color(0xFFE3DFD6)
val LightOutline = Color(0xFFD5D0C5)
val LightPrimary = Color(0xFF334E68)
val LightOnPrimary = Color(0xFFFAF8F4)
val LightPrimaryContainer = Color(0xFFE2E8EE)
val LightOnPrimaryContainer = Color(0xFF1F3244)
val LightSecondary = Color(0xFF4A6B62)
val LightSecondaryContainer = Color(0xFFE3ECE9)
val LightTextPrimary = Color(0xFF1E2022)
val LightTextSecondary = Color(0xFF5D6168)

// Soft neutral Dim Light overlay base
val DimLightScrimBase = Color(0xFF050608)

fun securityLevelColor(level: Int): Color = when (level) {
    1 -> Color(0xFF607D8B)
    2 -> Color(0xFF4A6B82)
    3 -> Color(0xFF4E7A72)
    4 -> Color(0xFF4B7A5A)
    else -> Color(0xFFB35C5C)
}
