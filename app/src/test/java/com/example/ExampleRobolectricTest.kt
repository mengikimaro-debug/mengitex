package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.HiddenVaultLogger
import com.example.security.SecuritySettings
import com.example.security.VaultCryptoManager
import com.example.service.GithubUpdateChecker
import com.example.service.MengiTexNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches MengiTex`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MengiTex", appName)
    }

    @Test
    fun `aes256 encryption and pbkdf2 passcode verification work`() {
        val crypto = VaultCryptoManager()
        val secret = "Ujumbe mrefu sana wa siri bila kikomo cha herufi 1234567890"
        val encrypted = crypto.encryptString(secret)
        assertNotEquals(secret, encrypted)
        assertEquals(secret, crypto.decryptString(encrypted))

        val salt = crypto.generateSalt()
        val hash = crypto.hashPasscode("2468", salt)
        assertTrue(crypto.verifyPasscode("2468", salt, hash))
        assertFalse(crypto.verifyPasscode("0000", salt, hash))
    }

    @Test
    fun `hidden vault logger stores and decrypts encrypted logs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = VaultCryptoManager(context)
        val logger = HiddenVaultLogger(context, crypto)

        logger.appendLog("KIMYA_GENI", "SMS geni kimya", "Hakuna mlio wala arifa")
        val logs = logger.readAllLogs()
        assertTrue(logs.any { it.category == "KIMYA_GENI" && it.title == "SMS geni kimya" })
    }

    @Test
    fun `updater rejects invalid apk files and checks semantic versions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val badFile = File(context.cacheDir, "invalid_html_response.apk").apply {
            writeText("<html>404 Not Found</html>")
        }
        val verifyRes = GithubUpdateChecker.verifyApkArchive(context, badFile)
        assertTrue(verifyRes.isFailure)
        badFile.delete()

        assertTrue(GithubUpdateChecker.isRemoteVersionNewer("3.0.0", "v3.1.0"))
        assertTrue(GithubUpdateChecker.isRemoteVersionNewer("3.5.0", "v3.6.0"))
        assertFalse(GithubUpdateChecker.isRemoteVersionNewer("3.6.0", "v3.6.0"))
        assertTrue(GithubUpdateChecker.isUpgradeVersionCompatible(30500L, 3_060_000L))
        assertEquals("mengikimaro-debug/mengitex", GithubUpdateChecker.normalizeRepoSlug("https://github.com/mengikimaro-debug/mengitex"))
    }

    @Test
    fun `notification manager enforces master and category toggles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val masterOff = SecuritySettings(notificationsMasterEnabled = false)
        assertFalse(
            MengiTexNotificationManager.postIncomingSmsNotification(
                context = context,
                senderTitle = "Amina",
                phoneNumber = "+255712345678",
                plainBody = "Habari",
                isProtectedTxn = false,
                isSecretBurn = false,
                settings = masterOff
            )
        )
        val personalOff = SecuritySettings(notificationsMasterEnabled = true, notifyPersonalSms = false)
        assertFalse(
            MengiTexNotificationManager.postIncomingSmsNotification(
                context = context,
                senderTitle = "Amina",
                phoneNumber = "+255712345678",
                plainBody = "Habari",
                isProtectedTxn = false,
                isSecretBurn = false,
                settings = personalOff
            )
        )
    }

    @Test
    fun `message reaction store toggles sets removes and clears emoji reactions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.MessageReactionStore.clearAll(context)
        assertTrue(com.example.data.MessageReactionStore.reactions.value.isEmpty())

        val first = com.example.data.MessageReactionStore.toggleReaction(context, 42L, "❤️")
        assertEquals("❤️", first)
        assertEquals("❤️", com.example.data.MessageReactionStore.reactions.value[42L]?.emoji)

        val swapped = com.example.data.MessageReactionStore.toggleReaction(context, 42L, "🔥")
        assertEquals("🔥", swapped)
        assertEquals("🔥", com.example.data.MessageReactionStore.reactions.value[42L]?.emoji)

        val removed = com.example.data.MessageReactionStore.toggleReaction(context, 42L, "🔥")
        assertEquals(null, removed)
        assertFalse(com.example.data.MessageReactionStore.reactions.value.containsKey(42L))

        com.example.data.MessageReactionStore.setReaction(context, 99L, "👍")
        assertEquals("👍", com.example.data.MessageReactionStore.reactions.value[99L]?.emoji)

        com.example.data.MessageReactionStore.removeReaction(context, 99L)
        assertFalse(com.example.data.MessageReactionStore.reactions.value.containsKey(99L))

        com.example.data.MessageReactionStore.setReaction(context, 100L, "😂")
        com.example.data.MessageReactionStore.clearAll(context)
        assertTrue(com.example.data.MessageReactionStore.reactions.value.isEmpty())
    }

    @Test
    fun `main activity launches without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
        val activity = controller.get()
        assertTrue(activity != null && !activity.isFinishing)
        controller.pause().stop().destroy()
    }
}
