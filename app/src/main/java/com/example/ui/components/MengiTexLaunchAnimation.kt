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
    val bgColor = MaterialTheme.colorScheme.background
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val logoScale = remember(sessionKey) { Animatable(0.86f) }
    val contentAlpha = remember(sessionKey) { Animatable(0.65f) }
    val barProgress = remember(sessionKey) { Animatable(0.35f) }
    val overlayAlpha = remember(sessionKey) { Animatable(1f) }

    LaunchedEffect(sessionKey) {
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(70, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentAlpha.animateTo(1f, tween(60, easing = FastOutSlowInEasing))
        }
        launch {
            barProgress.animateTo(1f, tween(75, easing = FastOutSlowInEasing))
        }

        delay(65)
        overlayAlpha.animateTo(0f, tween(50, easing = FastOutSlowInEasing))
        onFinished()
    }

    val haloScaleValue = 0.92f + 0.08f * contentAlpha.value
    val titleOffsetYValue = (1f - contentAlpha.value) * 6f
    val logoRotationValue = 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha.value
                val s = 1f + (1f - overlayAlpha.value) * 0.02f
                scaleX = s
                scaleY = s
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
                            scaleX = haloScaleValue
                            scaleY = haloScaleValue
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
                            rotationZ = logoRotationValue
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

            Text(
                text = "MengiTex",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                ),
                color = onBgColor,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = titleOffsetYValue
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(trackColor)
                    .alpha(contentAlpha.value)
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
