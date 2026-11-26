package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.LocalAppStrings
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun VaultLockScreen(
    errorMessage: String?,
    failedAttempts: Int = 0,
    lockoutUntilMillis: Long = 0L,
    wipeOnIntruderEnabled: Boolean = false,
    onUnlock: (String) -> Unit
) {
    val s = LocalAppStrings.current
    var passcode by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    // Live cooldown countdown synced to persisted lockout timestamp
    var remainingCooldownSec by remember(lockoutUntilMillis) {
        mutableIntStateOf(
            ((lockoutUntilMillis - System.currentTimeMillis()) / 1000L)
                .coerceAtLeast(0L)
                .toInt()
        )
    }
    LaunchedEffect(lockoutUntilMillis) {
        while (true) {
            val rem = ((lockoutUntilMillis - System.currentTimeMillis()) / 1000L)
                .coerceAtLeast(0L)
                .toInt()
            remainingCooldownSec = rem
            if (rem <= 0) break
            delay(500L)
        }
    }

    // Tactile horizontal shake animation whenever an invalid passcode is submitted
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(errorMessage, failedAttempts) {
        if (errorMessage != null) {
            passcode = ""
            listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f).forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(40))
            }
        }
    }

    val isInCooldown = remainingCooldownSec > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MengiTexLogoMark(
                    size = 78.dp,
                    cornerRadius = 22.dp,
                    showGlow = false,
                    modifier = Modifier.testTag("vault_lock_brand_logo")
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AES-256-GCM FORTRESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = s.lockScreenTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = s.lockScreenDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                AnimatedVisibility(visible = isInCooldown) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GppMaybe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = s.lockCooldownBanner(remainingCooldownSec),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (wipeOnIntruderEnabled && failedAttempts > 0) {
                    Text(
                        text = s.lockAttemptsWarning(failedAttempts, 10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    enabled = !isInCooldown,
                    label = { Text(s.lockScreenPassLabel) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (passcode.isNotEmpty() && !isInCooldown) onUnlock(passcode) }
                    ),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_lock_passcode_input"),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = { onUnlock(passcode) },
                    enabled = passcode.isNotEmpty() && !isInCooldown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("vault_unlock_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (isInCooldown) "${remainingCooldownSec}s" else s.lockScreenUnlockBtn,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Full-screen dark privacy shield overlay shown instantly whenever MengiTex loses window focus
 * (Recent Apps switcher, notification pull-down, or app switch) so onlookers never glimpse chats.
 */
@Composable
fun PrivacyShieldCurtainOverlay() {
    val s = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111318)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            MengiTexLogoMark(
                size = 84.dp,
                cornerRadius = 24.dp,
                showGlow = false
            )
            Text(
                text = s.privacyCurtainTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
            Text(
                text = s.privacyCurtainSub,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF93C5FD),
                textAlign = TextAlign.Center
            )
        }
    }
}
