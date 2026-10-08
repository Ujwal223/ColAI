package com.ujwal.colai.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.AEADBadTagException

class BackupManagerTest {

    @Test
    fun testAes256GcmRoundTripEncryptionAndDecryption() {
        val originalPlaintext = "{\"version\":1,\"app\":\"ColAI\",\"services\":[{\"id\":\"chatgpt\"}]}"
        val password = "StrongMasterPassword123!"

        val encryptedEnvelope = BackupManager.encryptString(originalPlaintext, password)
        assertTrue(encryptedEnvelope.isNotEmpty())
        assertNotEquals(originalPlaintext, encryptedEnvelope)

        val decryptedString = BackupManager.decryptString(encryptedEnvelope, password)
        assertEquals(originalPlaintext, decryptedString)
    }

    @Test
    fun testDecryptionFailsWithWrongPassword() {
        val originalPlaintext = "{\"secret\":\"session_tokens\"}"
        val correctPassword = "CorrectPassword456"
        val wrongPassword = "WrongPassword789"

        val encryptedEnvelope = BackupManager.encryptString(originalPlaintext, correctPassword)

        try {
            BackupManager.decryptString(encryptedEnvelope, wrongPassword)
            fail("Expected decryption with wrong password to throw an Exception")
        } catch (e: Exception) {
            // Tampered tag or incorrect password triggers authentication tag verification failure
            assertTrue(e is AEADBadTagException || e is java.security.GeneralSecurityException || e.message?.contains("tag", ignoreCase = true) == true)
        }
    }

    @Test
    fun testCorruptedPayloadFailsDecryption() {
        val originalPlaintext = "ValidPayload"
        val password = "Password"

        val encryptedEnvelope = BackupManager.encryptString(originalPlaintext, password)
        // Corrupt the ciphertext body inside the JSON envelope (handle both compact and spaced JSON)
        val corruptedEnvelope = encryptedEnvelope
            .replace("\"ciphertext\":\"", "\"ciphertext\":\"AAAA")
            .replace("\"ciphertext\": \"", "\"ciphertext\": \"AAAA\"")

        try {
            BackupManager.decryptString(corruptedEnvelope, password)
            fail("Expected corrupted payload to fail decryption")
        } catch (_: Exception) {
            // Successfully detected tampering/corruption
        }
    }

    @Test
    fun testDecryptionHandlesAccidentalTrailingAndLeadingWhitespace() {
        val originalPlaintext = "{\"test\":\"user_session_state\"}"
        val basePassword = "Ujwal@2065"

        // 1. Encrypted without space, decrypted with accidental trailing space from mobile keyboard
        val encryptedNormal = BackupManager.encryptString(originalPlaintext, basePassword)
        val decryptedWithTrailing = BackupManager.decryptString(encryptedNormal, "$basePassword ")
        assertEquals(originalPlaintext, decryptedWithTrailing)

        // 2. Encrypted with accidental trailing space, decrypted without space
        val encryptedWithSpace = BackupManager.encryptString(originalPlaintext, "$basePassword ")
        val decryptedNormal = BackupManager.decryptString(encryptedWithSpace, basePassword)
        assertEquals(originalPlaintext, decryptedNormal)

        // 3. Decrypted with accidental leading space
        val decryptedWithLeading = BackupManager.decryptString(encryptedNormal, " $basePassword")
        assertEquals(originalPlaintext, decryptedWithLeading)
    }

    @Test
    fun testSpecialCharacterPasswordsRoundTrip() {
        val originalPlaintext = "{\"test\":\"complex_passwords\"}"
        val complexPassword = "Ujwal@2065#$!%&*_-=+~`"

        val encrypted = BackupManager.encryptString(originalPlaintext, complexPassword)
        val decrypted = BackupManager.decryptString(encrypted, complexPassword)
        assertEquals(originalPlaintext, decrypted)
    }
}
