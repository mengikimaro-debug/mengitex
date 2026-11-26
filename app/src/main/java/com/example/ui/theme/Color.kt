package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

// Android 17 / Material 3 Expressive Dark Palette (100% Solid Matte Charcoal & Desaturated Slate — Zero Green, Zero Glow)
val ObsidianBg = Color(0xFF121316)
val ObsidianSurface = Color(0xFF191B1F)
val ObsidianSurfaceVariant = Color(0xFF212329)
val ObsidianSurfaceElevated = Color(0xFF292C33)
val ObsidianSurfaceHighest = Color(0xFF32363E)
val ObsidianOutline = Color(0xFF454954)

val EmeraldPrimary = Color(0xFF9FB0C9)
val EmeraldOnPrimary = Color(0xFF182130)
val EmeraldContainer = Color(0xFF2B3547)
val EmeraldOnContainer = Color(0xFFD6DFED)

val CyberCyan = Color(0xFFB0B6C2)
val CyberCyanOn = Color(0xFF20242C)
val CyberCyanContainer = Color(0xFF30343E)
val CyberCyanOnContainer = Color(0xFFDCE0E8)

val ShieldAmber = Color(0xFFC7AD96)
val ShieldAmberOn = Color(0xFF2D2016)
val ShieldAmberContainer = Color(0xFF453629)
val ShieldAmberOnContainer = Color(0xFFEBDDD0)

val AlertCrimson = Color(0xFFD29996)
val AlertCrimsonOn = Color(0xFF341616)
val AlertCrimsonContainer = Color(0xFF5C2624)
val AlertCrimsonOnContainer = Color(0xFFF2D4D2)

val SoftTextPrimary = Color(0xFFE2E4E8)
val SoftTextSecondary = Color(0xFFB3B8C2)

// Android 17 / Material 3 Expressive Warm Off-White Light Palette (100% Solid Matte Surfaces — Zero Green, Zero Glare)
val LightBg = Color(0xFFF7F5EF)
val LightSurface = Color(0xFFFAF8F3)
val LightSurfaceLowest = Color(0xFFFDFCF9)
val LightSurfaceLow = Color(0xFFF0EDE4)
val LightSurfaceVariant = Color(0xFFE7E3D9)
val LightSurfaceElevated = Color(0xFFDFDAD0)
val LightSurfaceHighest = Color(0xFFD4CEC2)
val LightOutline = Color(0xFFB4AEA3)
val LightOutlineVariant = Color(0xFFDFDAD0)

val LightPrimary = Color(0xFF33496E)
val LightOnPrimary = Color(0xFFFAF8F3)
val LightPrimaryContainer = Color(0xFFDCE3EE)
val LightOnPrimaryContainer = Color(0xFF162238)

val LightSecondary = Color(0xFF555D6E)
val LightOnSecondary = Color(0xFFFAF8F3)
val LightSecondaryContainer = Color(0xFFE1E4EC)
val LightOnSecondaryContainer = Color(0xFF1E2430)

val LightTertiary = Color(0xFF7D5342)
val LightOnTertiary = Color(0xFFFAF8F3)
val LightTertiaryContainer = Color(0xFFEFDFD7)
val LightOnTertiaryContainer = Color(0xFF331D14)

val LightError = Color(0xFFA8322D)
val LightOnError = Color(0xFFFAF8F3)
val LightErrorContainer = Color(0xFFF3DAD8)
val LightOnErrorContainer = Color(0xFF411210)

val LightTextPrimary = Color(0xFF1A1C20)
val LightTextSecondary = Color(0xFF535761)

// Distinct Conversation List Card Palette (clearly differentiated from Off-White canvas & dark charcoal — zero green)
val LightConvRowBg = Color(0xFFE9E6DF)
val LightConvRowUnreadBg = Color(0xFFDCE1EB)
val LightConvRowSelectedBg = Color(0xFFCAD4E4)
val LightConvRowAdBg = Color(0xFFEEE3E1)
val LightConvRowBorder = Color(0xFFD8D4CA)

val DarkConvRowBg = Color(0xFF1C1E24)
val DarkConvRowUnreadBg = Color(0xFF232834)
val DarkConvRowSelectedBg = Color(0xFF2B3547)
val DarkConvRowAdBg = Color(0xFF281F21)
val DarkConvRowBorder = Color(0xFF2C3039)

fun conversationRowContainerColor(
    isDark: Boolean,
    isSelected: Boolean,
    hasUnread: Boolean,
    isBlockedAd: Boolean = false
): Color = when {
    isSelected -> if (isDark) DarkConvRowSelectedBg else LightConvRowSelectedBg
    isBlockedAd -> if (isDark) DarkConvRowAdBg else LightConvRowAdBg
    hasUnread -> if (isDark) DarkConvRowUnreadBg else LightConvRowUnreadBg
    else -> if (isDark) DarkConvRowBg else LightConvRowBg
}

fun conversationRowBorderColor(
    isDark: Boolean,
    isSelected: Boolean,
    hasUnread: Boolean,
    isBlockedAd: Boolean = false
): Color = when {
    isSelected -> if (isDark) EmeraldPrimary else LightPrimary
    isBlockedAd -> if (isDark) AlertCrimsonContainer else LightErrorContainer
    hasUnread -> if (isDark) EmeraldContainer else LightPrimaryContainer
    else -> if (isDark) DarkConvRowBorder else LightConvRowBorder
}

// Solid Dim Light overlay base
val DimLightScrimBase = Color(0xFF000000)

fun securityLevelColor(level: Int): Color = when (level) {
    1 -> Color(0xFF5B606A)
    2 -> Color(0xFF475874)
    3 -> Color(0xFF33496E)
    4 -> Color(0xFF2B3D5E)
    else -> Color(0xFFA8322D)
}

/**
 * Android 17 Solid Tonal Avatar Pair (containerColor to contentColor) — matte, zero green, zero glow.
 */
fun android17AvatarColors(key: String, isDark: Boolean): Pair<Color, Color> {
    val lightPairs = listOf(
        Color(0xFFDCE3EE) to Color(0xFF162238), // Slate Navy
        Color(0xFFE3E0EC) to Color(0xFF261F3E), // Soft Indigo
        Color(0xFFEAE3D8) to Color(0xFF342A1B), // Warm Stone
        Color(0xFFEFDFD7) to Color(0xFF331D14), // Muted Terracotta
        Color(0xFFF0DCDF) to Color(0xFF401920), // Dusty Rose
        Color(0xFFE1E5EB) to Color(0xFF1F2633)  // Cool Pewter
    )
    val darkPairs = listOf(
        Color(0xFF2B3547) to Color(0xFFD6DFED), // Matte Slate Navy
        Color(0xFF332E47) to Color(0xFFE1DCF2), // Matte Indigo
        Color(0xFF3C352C) to Color(0xFFEBE3D7), // Matte Warm Stone
        Color(0xFF453629) to Color(0xFFEBDDD0), // Matte Terracotta
        Color(0xFF482C31) to Color(0xFFF0DADD), // Matte Dusty Rose
        Color(0xFF2F343E) to Color(0xFFDCE0E8)  // Matte Pewter
    )
    val list = if (isDark) darkPairs else lightPairs
    return list[abs(key.hashCode()) % list.size]
}
