package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Zero-delay Android 17 Material 3 Expressive launch splash overlay.
 * Uses crisp solid tonal surfaces with zero glow, zero gradients, and zero transparency effects.
 */
@Composable
fun MengiTexLaunchAnimationOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    sessionKey: Int = 1,
    isDarkOverride: Boolean? = null,
    taglineOverride: String? = null
) {
    val fallbackStrings = LocalAppStrings.current
    val tagline = taglineOverride ?: fallbackStrings.launchSplashTagline
    val isDark = isDarkOverride ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)

    val bgColor = if (isDark) Color(0xFF111318) else MaterialTheme.colorScheme.background
    val containerColor = if (isDark) Color(0xFF21242B) else MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = if (isDark) Color(0xFFD6E2FF) else MaterialTheme.colorScheme.onPrimaryContainer
    val primary = if (isDark) Color(0xFFA8C7FA) else MaterialTheme.colorScheme.primary
    val onBgColor = if (isDark) Color(0xFFE3E2E6) else MaterialTheme.colorScheme.onBackground
    val trackColor = if (isDark) Color(0xFF2B2F38) else MaterialTheme.colorScheme.surfaceContainerHighest

    val logoScale = remember(sessionKey) { Animatable(0.76f) }
    val logoRotation = remember(sessionKey) { Animatable(-8f) }
    val haloScale = remember(sessionKey) { Animatable(0.82f) }
    val titleAlpha = remember(sessionKey) { Animatable(0.45f) }
    val titleOffsetY = remember(sessionKey) { Animatable(10f) }
    val badgeAlpha = remember(sessionKey) { Animatable(0.35f) }
    val barProgress = remember(sessionKey) { Animatable(0.12f) }
    val overlayAlpha = remember(sessionKey) { Animatable(1f) }
    val overlayScale = remember(sessionKey) { Animatable(1f) }

    LaunchedEffect(sessionKey) {
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            logoRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            haloScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            titleAlpha.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
        }
        launch {
            titleOffsetY.animateTo(0f, tween(160, easing = FastOutSlowInEasing))
        }
        launch {
            badgeAlpha.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
        }
        launch {
            barProgress.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }

        delay(420)
        launch {
            overlayScale.animateTo(1.03f, tween(140, easing = FastOutSlowInEasing))
        }
        overlayAlpha.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha.value
                scaleX = overlayScale.value
                scaleY = overlayScale.value
            }
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            )
            .testTag("launch_logo_animation_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .graphicsLayer {
                            scaleX = haloScale.value
                            scaleY = haloScale.value
                        }
                        .clip(CircleShape)
                        .background(containerColor)
                )

                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            rotationZ = logoRotation.value
                        }
                        .clip(RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    MengiTexLogoMark(
                        size = 104.dp,
                        cornerRadius = 28.dp,
                        showGlow = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffsetY.value
                }
            ) {
                Text(
                    text = "MengiTex",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    ),
                    color = onBgColor
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = containerColor
                ) {
                    Text(
                        text = "v${com.example.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = onContainerColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = containerColor,
                modifier = Modifier.alpha(badgeAlpha.value)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = onContainerColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onContainerColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(trackColor)
                    .alpha(badgeAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .width((128f * barProgress.value).dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(primary)
                )
            }
        }
    }
}
