package com.example

import com.example.security.SecuritySettings
import com.example.service.GithubUpdateChecker
import com.example.service.MengiTexNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun versionComparisonAndRepoNormalizationWork() {
        assertTrue(GithubUpdateChecker.isRemoteVersionNewer("3.0.0", "v3.1.0"))
        assertFalse(GithubUpdateChecker.isRemoteVersionNewer("3.1.0", "v3.1.0"))
        assertFalse(GithubUpdateChecker.isRemoteVersionNewer("3.1.0", "v3.0.0"))
        assertEquals(
            "mengikimaro-debug/mengitex",
            GithubUpdateChecker.normalizeRepoSlug("https://github.com/mengikimaro-debug/mengitex")
        )
        assertTrue(GithubUpdateChecker.isDirectApkUrl("https://github.com/a/b/releases/download/v3.1.0/MengiTex.apk"))
        assertFalse(GithubUpdateChecker.isDirectApkUrl("mengikimaro-debug/mengitex"))
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
}
