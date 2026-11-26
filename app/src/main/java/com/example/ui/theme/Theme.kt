package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SiriDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = CyberCyan,
    onSecondary = Color(0xFF06201E),
    secondaryContainer = CyberCyanContainer,
    onSecondaryContainer = CyberCyanOnContainer,
    tertiary = ShieldAmber,
    onTertiary = Color(0xFF261A04),
    tertiaryContainer = ShieldAmberContainer,
    onTertiaryContainer = ShieldAmberOnContainer,
    error = AlertCrimson,
    onError = Color(0xFF2B0B0E),
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = AlertCrimsonOnContainer,
    background = ObsidianBg,
    onBackground = SoftTextPrimary,
    surface = ObsidianSurface,
    onSurface = SoftTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SoftTextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceElevated,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianSurfaceElevated
)

private val SiriLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightSurface,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightTextPrimary,
    tertiary = ShieldAmber,
    onTertiary = LightTextPrimary,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFB3261E),
    onError = LightSurface,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = LightSurfaceElevated,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightSurfaceElevated
)

@Composable
fun SiriSmsTheme(
    darkTheme: Boolean = true,
    dimLightEnabled: Boolean = false,
    dimLightLevel: Float = 0.35f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SiriDarkColorScheme else SiriLightColorScheme
    val dimOverlayAlpha by animateFloatAsState(
        targetValue = if (dimLightEnabled) dimLightLevel.coerceIn(0.15f, 0.60f) else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "dim_light_alpha"
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SiriSmsTypography
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            if (dimOverlayAlpha > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DimLightScrimBase.copy(alpha = dimOverlayAlpha))
                )
            }
        }
    }
}
