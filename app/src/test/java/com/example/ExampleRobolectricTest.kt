package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.HiddenVaultLogger
import com.example.security.VaultCryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
        val crypto = VaultCryptoManager()
        val logger = HiddenVaultLogger(context, crypto)

        logger.appendLog("KIMYA_GENI", "SMS geni kimya", "Hakuna mlio wala arifa")
        val logs = logger.readAllLogs()
        assertTrue(logs.any { it.category == "KIMYA_GENI" && it.title == "SMS geni kimya" })
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
