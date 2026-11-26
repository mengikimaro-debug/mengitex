package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

// Android 17 / Material 3 Expressive Dark Palette (100% Solid Tonal Surfaces, Zero Glow)
val ObsidianBg = Color(0xFF111318)
val ObsidianSurface = Color(0xFF191C22)
val ObsidianSurfaceVariant = Color(0xFF21242B)
val ObsidianSurfaceElevated = Color(0xFF2B2F38)
val ObsidianSurfaceHighest = Color(0xFF353A45)
val ObsidianOutline = Color(0xFF44474E)

val EmeraldPrimary = Color(0xFFA8C7FA)
val EmeraldOnPrimary = Color(0xFF062E6F)
val EmeraldContainer = Color(0xFF0842A0)
val EmeraldOnContainer = Color(0xFFD3E3FD)

val CyberCyan = Color(0xFF7FD6CA)
val CyberCyanContainer = Color(0xFF005048)
val CyberCyanOnContainer = Color(0xFFA8F2E6)

val ShieldAmber = Color(0xFFE5B85C)
val ShieldAmberContainer = Color(0xFF4C3910)
val ShieldAmberOnContainer = Color(0xFFFFE09E)

val AlertCrimson = Color(0xFFF2B8B5)
val AlertCrimsonContainer = Color(0xFF8C1D18)
val AlertCrimsonOnContainer = Color(0xFFF9DEDC)

val SoftTextPrimary = Color(0xFFE3E4E8)
val SoftTextSecondary = Color(0xFFC4C6CE)

// Android 17 / Material 3 Expressive Light Palette (100% Solid Tonal Surfaces, Zero Glow)
val LightBg = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF2F8)
val LightSurfaceElevated = Color(0xFFE4E9F2)
val LightSurfaceHighest = Color(0xFFD8E0EC)
val LightOutline = Color(0xFFC4C7CE)
val LightPrimary = Color(0xFF0B57D0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD3E3FD)
val LightOnPrimaryContainer = Color(0xFF041E49)
val LightSecondary = Color(0xFF006A60)
val LightSecondaryContainer = Color(0xFFCBECE6)
val LightTextPrimary = Color(0xFF1A1C20)
val LightTextSecondary = Color(0xFF44474E)

// Solid Dim Light overlay base
val DimLightScrimBase = Color(0xFF000000)

fun securityLevelColor(level: Int): Color = when (level) {
    1 -> Color(0xFF5F6368)
    2 -> Color(0xFF0B57D0)
    3 -> Color(0xFF006A60)
    4 -> Color(0xFF146C2E)
    else -> Color(0xFFB3261E)
}

/**
 * Android 17 Solid Tonal Avatar Pair (containerColor to contentColor) — zero gradients, zero glow.
 */
fun android17AvatarColors(key: String, isDark: Boolean): Pair<Color, Color> {
    val lightPairs = listOf(
        Color(0xFFD3E3FD) to Color(0xFF041E49), // Pixel Blue
        Color(0xFFCBECE6) to Color(0xFF003731), // Sage Teal
        Color(0xFFEADDFF) to Color(0xFF21005D), // Expressive Lavender
        Color(0xFFFFE09E) to Color(0xFF3E2C00), // Warm Amber
        Color(0xFFFFDAD6) to Color(0xFF410002), // Terracotta Rose
        Color(0xFFC4EED0) to Color(0xFF072711)  // Mint Green
    )
    val darkPairs = listOf(
        Color(0xFF0842A0) to Color(0xFFD3E3FD), // Pixel Blue
        Color(0xFF005048) to Color(0xFFA8F2E6), // Sage Teal
        Color(0xFF4F378B) to Color(0xFFEADDFF), // Expressive Lavender
        Color(0xFF5C4300) to Color(0xFFFFE09E), // Warm Amber
        Color(0xFF7D2A24) to Color(0xFFFFDAD6), // Terracotta Rose
        Color(0xFF0F5223) to Color(0xFFC4EED0)  // Mint Green
    )
    val list = if (isDark) darkPairs else lightPairs
    return list[abs(key.hashCode()) % list.size]
}
