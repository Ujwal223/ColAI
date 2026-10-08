package com.ujwal.colai.core.security

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EncryptedStorageTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var storage: EncryptedStorage

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        storage = EncryptedStorage.forTesting(fakePrefs)
    }

    @Test
    fun testStringStorageAndRetrieval() {
        storage.putString("test_key", "secret_value")
        assertEquals("secret_value", storage.getString("test_key"))
        assertEquals("default", storage.getString("non_existent", "default"))
    }

    @Test
    fun testBooleanStorageAndRetrieval() {
        storage.putBoolean("flag_active", true)
        assertTrue(storage.getBoolean("flag_active"))
        assertFalse(storage.getBoolean("flag_missing", false))
    }

    @Test
    fun testIntAndLongStorage() {
        storage.putInt("count", 42)
        assertEquals(42, storage.getInt("count"))

        storage.putLong("timestamp", 1700000000L)
        assertEquals(1700000000L, storage.getLong("timestamp"))
    }

    @Test
    fun testRemoveAndContains() {
        storage.putString("removable", "to_delete")
        assertTrue(storage.contains("removable"))

        storage.remove("removable")
        assertFalse(storage.contains("removable"))
        assertNull(storage.getString("removable"))
    }

    @Test
    fun testClear() {
        storage.putString("k1", "v1")
        storage.putBoolean("k2", true)
        storage.clear()

        assertFalse(storage.contains("k1"))
        assertFalse(storage.contains("k2"))
    }

    @Test
    fun testHighLevelPreferenceDefaults() {
        assertEquals("system", storage.getThemeMode())
        assertEquals("standard", storage.getContrastLevel())
        assertTrue(storage.isSsoEnabled())
        assertFalse(storage.isOnboardingCompleted())
        assertNull(storage.getActiveServiceId())
        assertNull(storage.getActiveSessionId())

        storage.setThemeMode("dark")
        assertEquals("dark", storage.getThemeMode())

        storage.setOnboardingCompleted(true)
        assertTrue(storage.isOnboardingCompleted())

        storage.setActiveServiceId("chatgpt")
        assertEquals("chatgpt", storage.getActiveServiceId())

        storage.setActiveSessionId("session-uuid-1")
        assertEquals("session-uuid-1", storage.getActiveSessionId())
    }

    @Test
    fun testSessionPinLockAndVerification() {
        val sessionId = "session-123"
        assertFalse(storage.isSessionPinLocked(sessionId))
        assertTrue(storage.verifySessionPin(sessionId, "0000")) // Unlocked sessions always return true

        storage.setSessionPin(sessionId, "1234")
        assertTrue(storage.isSessionPinLocked(sessionId))
        assertTrue(storage.verifySessionPin(sessionId, "1234"))
        assertFalse(storage.verifySessionPin(sessionId, "9999"))

        // Update PIN
        storage.setSessionPin(sessionId, "4321")
        assertTrue(storage.verifySessionPin(sessionId, "4321"))
        assertFalse(storage.verifySessionPin(sessionId, "1234"))

        // Remove PIN
        storage.removeSessionPin(sessionId)
        assertFalse(storage.isSessionPinLocked(sessionId))
    }

    @Test
    fun testMasterRecoveryKeyFlow() {
        assertFalse(storage.hasMasterRecoveryKey())
        assertNull(storage.getMasterRecoveryKey())

        storage.setMasterRecoveryKey("SECRET-RECOVERY-KEY-123")
        assertTrue(storage.hasMasterRecoveryKey())
        assertEquals("SECRET-RECOVERY-KEY-123", storage.getMasterRecoveryKey())

        assertTrue(storage.verifyMasterRecoveryKey("SECRET-RECOVERY-KEY-123"))
        assertFalse(storage.verifyMasterRecoveryKey("WRONG-KEY"))
    }

    @Test
    fun testPerSessionContentBlocking() {
        val s1 = "session-a"
        val s2 = "session-b"

        // Default should be false to prevent strict tracker blocking breaking AI web apps
        assertFalse(storage.isSessionContentBlockingEnabled(s1))
        assertFalse(storage.isSessionContentBlockingEnabled(s2))

        // Enable only for s1
        storage.setSessionContentBlockingEnabled(s1, true)
        assertTrue(storage.isSessionContentBlockingEnabled(s1))
        assertFalse(storage.isSessionContentBlockingEnabled(s2)) // s2 remains disabled
    }

    @Test
    fun testSessionStatePersistence() {
        val sessionId = "session-state-1"
        assertNull(storage.getSessionState(sessionId))

        val mockState = "{\"history\":[{\"url\":\"https://chatgpt.com\"}]}"
        storage.saveSessionState(sessionId, mockState)
        assertEquals(mockState, storage.getSessionState(sessionId))

        storage.clearSessionState(sessionId)
        assertNull(storage.getSessionState(sessionId))
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data

        override fun getString(key: String, defValue: String?): String? =
            data[key] as? String ?: defValue

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String> ?: defValues)

        override fun getInt(key: String, defValue: Int): Int =
            data[key] as? Int ?: defValue

        override fun getLong(key: String, defValue: Long): Long =
            data[key] as? Long ?: defValue

        override fun getFloat(key: String, defValue: Float): Float =
            data[key] as? Float ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean =
            data[key] as? Boolean ?: defValue

        override fun contains(key: String): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(data)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val data: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var cleared = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                temp[key] = value
                return this
            }

            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
                temp[key] = values
                return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                temp[key] = value
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                temp[key] = value
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                temp[key] = value
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                temp[key] = value
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                removed.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                cleared = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (cleared) {
                    data.clear()
                }
                for (key in removed) {
                    data.remove(key)
                }
                data.putAll(temp)
            }
        }
    }
}
