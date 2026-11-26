package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

// Android 17 / Material 3 Expressive Dark Palette (100% Solid Matte Tonal Surfaces, Zero Glow)
val ObsidianBg = Color(0xFF111413)
val ObsidianSurface = Color(0xFF181C1B)
val ObsidianSurfaceVariant = Color(0xFF202523)
val ObsidianSurfaceElevated = Color(0xFF292F2D)
val ObsidianSurfaceHighest = Color(0xFF333A37)
val ObsidianOutline = Color(0xFF434A47)

val EmeraldPrimary = Color(0xFF72D1BC)
val EmeraldOnPrimary = Color(0xFF06372E)
val EmeraldContainer = Color(0xFF154E43)
val EmeraldOnContainer = Color(0xFFC2EDE2)

val CyberCyan = Color(0xFF8CC2E3)
val CyberCyanOn = Color(0xFF072B3F)
val CyberCyanContainer = Color(0xFF1B4259)
val CyberCyanOnContainer = Color(0xFFCDE8FA)

val ShieldAmber = Color(0xFFE0B45A)
val ShieldAmberOn = Color(0xFF2B1D04)
val ShieldAmberContainer = Color(0xFF4B3710)
val ShieldAmberOnContainer = Color(0xFFFDE0A0)

val AlertCrimson = Color(0xFFF2B8B5)
val AlertCrimsonOn = Color(0xFF330C0E)
val AlertCrimsonContainer = Color(0xFF8C1D18)
val AlertCrimsonOnContainer = Color(0xFFF9DEDC)

val SoftTextPrimary = Color(0xFFE3E5E4)
val SoftTextSecondary = Color(0xFFBFC4C1)

// Android 17 / Material 3 Expressive Warm Off-White Light Palette (100% Solid Matte Surfaces, Zero Glare)
val LightBg = Color(0xFFF6F4EE)
val LightSurface = Color(0xFFFAF8F3)
val LightSurfaceLowest = Color(0xFFFDFCF9)
val LightSurfaceLow = Color(0xFFF0EDE4)
val LightSurfaceVariant = Color(0xFFE9E4D8)
val LightSurfaceElevated = Color(0xFFE1DBCD)
val LightSurfaceHighest = Color(0xFFD6CFC0)
val LightOutline = Color(0xFFB5AEA0)
val LightOutlineVariant = Color(0xFFDDD7C9)

val LightPrimary = Color(0xFF0E6B5E)
val LightOnPrimary = Color(0xFFFAF8F3)
val LightPrimaryContainer = Color(0xFFCFEBE3)
val LightOnPrimaryContainer = Color(0xFF06322B)

val LightSecondary = Color(0xFF2B5B78)
val LightOnSecondary = Color(0xFFFAF8F3)
val LightSecondaryContainer = Color(0xFFD7E7F0)
val LightOnSecondaryContainer = Color(0xFF102A3A)

val LightTertiary = Color(0xFF945814)
val LightOnTertiary = Color(0xFFFAF8F3)
val LightTertiaryContainer = Color(0xFFF5E3C5)
val LightOnTertiaryContainer = Color(0xFF3E2000)

val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFAF8F3)
val LightErrorContainer = Color(0xFFF5D8D6)
val LightOnErrorContainer = Color(0xFF410E0B)

val LightTextPrimary = Color(0xFF191C1B)
val LightTextSecondary = Color(0xFF525754)

// Distinct Conversation List Card Palette (clearly differentiated from Off-White canvas & dark slate)
val LightConvRowBg = Color(0xFFE5EDE9)
val LightConvRowUnreadBg = Color(0xFFD4E6DF)
val LightConvRowSelectedBg = Color(0xFFBEE0D5)
val LightConvRowAdBg = Color(0xFFEFE4E1)
val LightConvRowBorder = Color(0xFFD2DDD8)

val DarkConvRowBg = Color(0xFF1B2321)
val DarkConvRowUnreadBg = Color(0xFF1E2F2B)
val DarkConvRowSelectedBg = Color(0xFF154E43)
val DarkConvRowAdBg = Color(0xFF281D1E)
val DarkConvRowBorder = Color(0xFF273330)

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
    1 -> Color(0xFF5A6260)
    2 -> Color(0xFF2B5B78)
    3 -> Color(0xFF0E6B5E)
    4 -> Color(0xFF1A6B3C)
    else -> Color(0xFFB3261E)
}

/**
 * Android 17 Solid Tonal Avatar Pair (containerColor to contentColor) — zero gradients, zero glow.
 */
fun android17AvatarColors(key: String, isDark: Boolean): Pair<Color, Color> {
    val lightPairs = listOf(
        Color(0xFFCFEBE3) to Color(0xFF06322B), // Sage Emerald
        Color(0xFFD7E7F0) to Color(0xFF102A3A), // Slate Mist
        Color(0xFFE6DFF2) to Color(0xFF26174A), // Soft Lavender
        Color(0xFFF5E3C5) to Color(0xFF3E2000), // Warm Sand
        Color(0xFFF4DCD8) to Color(0xFF410E0B), // Muted Clay
        Color(0xFFD5EBD6) to Color(0xFF0D2E15)  // Eucalyptus
    )
    val darkPairs = listOf(
        Color(0xFF154E43) to Color(0xFFC2EDE2), // Sage Emerald
        Color(0xFF1B4259) to Color(0xFFCDE8FA), // Slate Mist
        Color(0xFF3F3169) to Color(0xFFE6DFF2), // Soft Lavender
        Color(0xFF523B13) to Color(0xFFFDE0A0), // Warm Sand
        Color(0xFF6B2622) to Color(0xFFF9DEDC), // Muted Clay
        Color(0xFF194B27) to Color(0xFFC9EDD0)  // Eucalyptus
    )
    val list = if (isDark) darkPairs else lightPairs
    return list[abs(key.hashCode()) % list.size]
}
