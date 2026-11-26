package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.MainTab

/**
 * Lightweight M3 tactile clickable modifier without Modifier.composed allocation overhead.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.premiumBounceClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    onClick: () -> Unit
): Modifier = this.clickable(enabled = enabled, onClick = onClick)

@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

@Composable
fun PulsingStatusDot(
    color: Color,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_dot")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_scale"
    )
    Box(modifier = modifier.size(size * 1.8f), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun AnimatedStatNumber(
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        modifier = modifier,
        label = "animated_stat_number"
    ) { target ->
        Text(
            text = target.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VoiceWaveformVisualizer(
    rmsLevel: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val multipliers = remember { listOf(0.55f, 0.85f, 1.0f, 0.78f, 0.92f, 0.62f, 0.45f) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        multipliers.forEachIndexed { idx, base ->
            val waveOffset = ((kotlin.math.sin(phase + idx * 0.85f) + 1f) * 0.5f).toFloat()
            val targetHeight = (6f + (rmsLevel.coerceIn(0.12f, 1f) * 20f * base) + (waveOffset * 6f)).dp
            val animatedHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "bar_$idx"
            )
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

/**
 * Authentic Full-Width Android 17 Material 3 Expressive NavigationBar (Messages & Contacts).
 * Solid surfaceContainer dock, M3 pill indicator above label, zero floating borders or shadows.
 */
@Composable
fun PremiumBottomNavBar(
    selectedTab: MainTab,
    messagesLabel: String,
    contactsLabel: String,
    onSelectTab: (MainTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Android17NavBarItem(
                    selected = selectedTab == MainTab.MESSAGES,
                    label = messagesLabel,
                    selectedIcon = Icons.Filled.Sms,
                    unselectedIcon = Icons.Outlined.Sms,
                    testTag = "bottom_nav_messages",
                    onClick = { onSelectTab(MainTab.MESSAGES) },
                    modifier = Modifier.weight(1f)
                )
                Android17NavBarItem(
                    selected = selectedTab == MainTab.CONTACTS,
                    label = contactsLabel,
                    selectedIcon = Icons.Filled.Contacts,
                    unselectedIcon = Icons.Outlined.Contacts,
                    testTag = "bottom_nav_contacts",
                    onClick = { onSelectTab(MainTab.CONTACTS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Android17NavBarItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(20),
        label = "a17_nav_indicator"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(20),
        label = "a17_nav_icon"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(20),
        label = "a17_nav_label"
    )

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(indicatorColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1
        )
    }
}
