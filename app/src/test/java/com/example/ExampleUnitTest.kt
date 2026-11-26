package com.example

import com.example.security.AppThemeMode
import com.example.security.SecuritySettings
import com.example.service.GithubUpdateChecker
import com.example.service.MengiTexNotificationManager
import com.example.ui.theme.DarkConvRowBg
import com.example.ui.theme.LightBg
import com.example.ui.theme.LightConvRowBg
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceLow
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.conversationRowContainerColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun versionComparisonAndRepoNormalizationWork() {
        assertTrue(GithubUpdateChecker.isRemoteVersionNewer("3.0.0", "v3.1.0"))
        assertTrue(GithubUpdateChecker.isRemoteVersionNewer("3.1.0", "v3.2.0"))
        assertFalse(GithubUpdateChecker.isRemoteVersionNewer("3.2.0", "v3.2.0"))
        assertFalse(GithubUpdateChecker.isRemoteVersionNewer("3.2.0", "v3.1.0"))
        assertEquals(
            "mengikimaro-debug/mengitex",
            GithubUpdateChecker.normalizeRepoSlug("https://github.com/mengikimaro-debug/mengitex")
        )
        assertTrue(GithubUpdateChecker.isDirectApkUrl("https://github.com/a/b/releases/download/v3.2.0/MengiTex.apk"))
        assertFalse(GithubUpdateChecker.isDirectApkUrl("mengikimaro-debug/mengitex"))
    }

    @Test
    fun apkUpgradeVersionCodeAndSigningCompatibilityWork() {
        assertTrue(GithubUpdateChecker.isUpgradeVersionCompatible(installedVersionCode = 5L, candidateVersionCode = 6L))
        assertTrue(GithubUpdateChecker.isUpgradeVersionCompatible(installedVersionCode = 6L, candidateVersionCode = 6L))
        assertFalse(GithubUpdateChecker.isUpgradeVersionCompatible(installedVersionCode = 6L, candidateVersionCode = 5L))

        val unifiedCert = "C1FE4DDFB5CF133172F291754F9833B6F663FFF1808F154570EBA35A7A63B36B"
        val otherCert = "AA11BB22CC33DD44EE55FF667788990011223344556677889900AABBCCDDEEFF"
        assertEquals(true, GithubUpdateChecker.areSigningCertificatesCompatible(setOf(unifiedCert), setOf(unifiedCert)))
        assertEquals(false, GithubUpdateChecker.areSigningCertificatesCompatible(setOf(otherCert), setOf(unifiedCert)))
        assertNull(GithubUpdateChecker.areSigningCertificatesCompatible(emptySet(), setOf(unifiedCert)))
    }

    @Test
    fun notificationQuietHoursAndOtpExtractionWork() {
        val disabled = SecuritySettings(quietHoursEnabled = false)
        assertFalse(MengiTexNotificationManager.isCurrentlyInQuietHours(disabled))

        val alwaysQuiet = SecuritySettings(
            quietHoursEnabled = true,
            quietHoursStartHour = 0,
            quietHoursEndHour = 0
        )
        assertTrue(MengiTexNotificationManager.isCurrentlyInQuietHours(alwaysQuiet))

        assertEquals("482910", MengiTexNotificationManager.extractOtpCode("Your MengiTex verification code is 482910"))
        assertNull(MengiTexNotificationManager.extractOtpCode("Habari za asubuhi rafiki 2026"))
    }

    @Test
    fun lightThemeOffWhiteAndConversationListColorsAreDistinct() {
        // Off-White light background and surface should be warm off-white, not pure stark white
        assertNotEquals(Color.White, LightBg)
        assertNotEquals(Color.White, LightSurface)
        assertTrue(LightBg.luminance() > 0.85f)

        // Conversation list row colors must differ from general background/surface tokens
        val lightNormal = conversationRowContainerColor(isDark = false, isSelected = false, hasUnread = false)
        val lightUnread = conversationRowContainerColor(isDark = false, isSelected = false, hasUnread = true)
        val lightSelected = conversationRowContainerColor(isDark = false, isSelected = true, hasUnread = false)
        assertEquals(LightConvRowBg, lightNormal)
        assertNotEquals(LightBg, lightNormal)
        assertNotEquals(LightSurfaceLow, lightNormal)
        assertNotEquals(lightNormal, lightUnread)
        assertNotEquals(lightUnread, lightSelected)

        val darkNormal = conversationRowContainerColor(isDark = true, isSelected = false, hasUnread = false)
        val darkUnread = conversationRowContainerColor(isDark = true, isSelected = false, hasUnread = true)
        assertEquals(DarkConvRowBg, darkNormal)
        assertNotEquals(ObsidianBg, darkNormal)
        assertNotEquals(darkNormal, darkUnread)

        val defaultSettings = SecuritySettings()
        assertEquals(AppThemeMode.LIGHT, defaultSettings.themeMode)
        assertFalse(defaultSettings.resolveIsDarkTheme(systemIsDark = true))
    }
}
