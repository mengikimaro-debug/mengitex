package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Zero-delay MengiTex Launch Logo Animation played immediately on Frame 0 whenever opening the app.
 * Every visual motion layer (elastic M emblem spring, dual expanding sonar rings, luminous orbiting
 * sparks, sweeping orbital arc, glass sheen glint, and wordmark reveal) starts simultaneously at 0ms.
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

    // Crisp palette that works instantly even on Frame 0 before theme/DataStore settles
    val primary = if (isDark) Color(0xFF4F8BFF) else MaterialTheme.colorScheme.primary
    val secondary = if (isDark) Color(0xFF2DD4BF) else MaterialTheme.colorScheme.secondary
    val onBgColor = if (isDark) Color(0xFFF3F5FA) else MaterialTheme.colorScheme.onBackground
    val trackColor = if (isDark) Color(0xFF2B313E) else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    // Frame-0 live initial values — already visible and in motion immediately (0ms wait)
    val logoScale = remember(sessionKey) { Animatable(0.72f) }
    val logoRotation = remember(sessionKey) { Animatable(-14f) }
    val ring1Progress = remember(sessionKey) { Animatable(0.08f) }
    val ring2Progress = remember(sessionKey) { Animatable(0.01f) }
    val orbitAngle = remember(sessionKey) { Animatable(0f) }
    val sheenProgress = remember(sessionKey) { Animatable(-0.35f) }
    val titleAlpha = remember(sessionKey) { Animatable(0.45f) }
    val titleOffsetY = remember(sessionKey) { Animatable(12f) }
    val badgeAlpha = remember(sessionKey) { Animatable(0.35f) }
    val barProgress = remember(sessionKey) { Animatable(0.08f) }
    val overlayAlpha = remember(sessionKey) { Animatable(1f) }
    val overlayScale = remember(sessionKey) { Animatable(1f) }

    LaunchedEffect(sessionKey) {
        // ALL motion layers fire simultaneously at t = 0ms (ZERO startup delays)
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.48f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            logoRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.60f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            ring1Progress.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
        launch {
            ring2Progress.animateTo(1f, tween(440, easing = FastOutSlowInEasing))
        }
        launch {
            orbitAngle.animateTo(360f, tween(480, easing = LinearEasing))
        }
        launch {
            sheenProgress.animateTo(1.55f, tween(360, easing = FastOutSlowInEasing))
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
            barProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }

        // Ultra-crisp 6G-speed dissolve straight into the live app (~420ms)
        delay(420)
        launch {
            overlayScale.animateTo(1.04f, tween(140, easing = FastOutSlowInEasing))
        }
        overlayAlpha.animateTo(0f, tween(140, easing = FastOutSlowInEasing))
        onFinished()
    }

    val bgBrush = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF141C30),
                Color(0xFF0B0C10),
                Color(0xFF07080B)
            )
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFE7F0FF),
                Color(0xFFF4F6FC),
                Color(0xFFFFFFFF)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha.value
                scaleX = overlayScale.value
                scaleY = overlayScale.value
            }
            .background(bgBrush)
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
            // Animated Logo Stage with Dual Sonar Rings, Sweeping Arc & Orbiting Sparks
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension * 0.48f

                    // Ambient luminous aura behind M emblem
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = if (isDark) 0.30f else 0.20f),
                                secondary.copy(alpha = if (isDark) 0.12f else 0.08f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * 0.94f
                        ),
                        radius = maxRadius * 0.94f,
                        center = center
                    )

                    // Sonar Ring 1 (active from Frame 0)
                    val r1 = ring1Progress.value
                    if (r1 > 0f && r1 < 1f) {
                        val radius1 = maxRadius * (0.46f + 0.54f * r1)
                        drawCircle(
                            color = primary.copy(alpha = (1f - r1) * 0.72f),
                            radius = radius1,
                            center = center,
                            style = Stroke(width = (3.6f * (1f - r1 * 0.55f)).dp.toPx())
                        )
                    }

                    // Sonar Ring 2 (active from Frame 0 at a complementary wave speed)
                    val r2 = ring2Progress.value
                    if (r2 > 0f && r2 < 1f) {
                        val radius2 = maxRadius * (0.40f + 0.60f * r2)
                        drawCircle(
                            color = secondary.copy(alpha = (1f - r2) * 0.60f),
                            radius = radius2,
                            center = center,
                            style = Stroke(width = (2.8f * (1f - r2 * 0.55f)).dp.toPx())
                        )
                    }

                    // Sweeping glowing orbital arc around the M mark
                    val arcRadius = maxRadius * 0.66f
                    val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                    val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                primary.copy(alpha = 0.75f),
                                secondary.copy(alpha = 0.9f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = orbitAngle.value,
                        sweepAngle = 115f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 6 Orbiting luminous particles around the M logo
                    val orbitRadius = maxRadius * 0.74f
                    val baseDeg = orbitAngle.value
                    for (i in 0 until 6) {
                        val angleRad = ((baseDeg + i * 60f) * PI / 180.0)
                        val px = center.x + (orbitRadius * cos(angleRad)).toFloat()
                        val py = center.y + (orbitRadius * sin(angleRad)).toFloat()
                        val dotColor = if (i % 2 == 0) primary else secondary
                        val dotRadius = if (i % 2 == 0) 3.8.dp.toPx() else 2.7.dp.toPx()
                        drawCircle(
                            color = dotColor.copy(alpha = 0.85f),
                            radius = dotRadius,
                            center = Offset(px, py)
                        )
                    }
                }

                // Core MengiTex M Logo Mark — visible immediately on Frame 0 with elastic spring & glass sheen
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            rotationZ = logoRotation.value
                        }
                        .clip(RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    MengiTexLogoMark(
                        size = 106.dp,
                        cornerRadius = 28.dp,
                        showGlow = true
                    )

                    // Diagonal specular glass shimmer sweeping across the M emblem immediately
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val p = sheenProgress.value
                        if (p in -0.5f..1.55f) {
                            val startX = size.width * p
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.56f),
                                        Color.Transparent
                                    ),
                                    start = Offset(startX - size.width * 0.35f, 0f),
                                    end = Offset(startX + size.width * 0.35f, size.height)
                                ),
                                start = Offset(startX - size.width * 0.25f, -12f),
                                end = Offset(startX + size.width * 0.25f, size.height + 12f),
                                strokeWidth = 28.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Brand Name Reveal (instant, zero delay) with v2.0.0 upgrade pill
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
                        letterSpacing = 0.8.sp
                    ),
                    color = onBgColor
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                ) {
                    Text(
                        text = "v${com.example.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Encrypted + Auto Ad Shield Tagline Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = primary.copy(alpha = if (isDark) 0.16f else 0.10f),
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
                        tint = primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sleek animated progress bar (active from 0ms)
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
                        .background(
                            Brush.horizontalGradient(listOf(primary, secondary))
                        )
                )
            }
        }
    }
}
