package com.ujwal.colai.core.data

import android.content.SharedPreferences
import com.ujwal.colai.core.database.ServiceDao
import com.ujwal.colai.core.database.SessionDao
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyDataMigratorTest {

    @Test
    fun testParseAIServicesJson() {
        val json = """
        [
          {
            "id": "chatgpt",
            "name": "ChatGPT",
            "url": "https://chatgpt.com",
            "faviconUrl": "https://openai.com/favicon.ico",
            "iconPath": "https://cdn.freebiesupply.com/logos/large/2x/chatgpt-symbol.png",
            "customUserAgent": "Mozilla/5.0 (Android; Mobile; rv:130.0)",
            "customHeaders": {
              "X-Custom-Client": "ColAI-Mobile"
            },
            "widgetSessionId": "sess-default-1",
            "notificationsEnabled": true,
            "createdAt": "2026-01-15T10:30:00.000Z"
          },
          {
            "id": "claude",
            "name": "Claude",
            "url": "https://claude.ai",
            "faviconUrl": "https://claude.ai/favicon.ico",
            "createdAt": "2026-01-16T12:00:00.000Z"
          }
        ]
        """.trimIndent()

        val migrator = LegacyDataMigrator(
            serviceDao = FakeServiceDao(),
            sessionDao = FakeSessionDao(),
            encryptedStorage = EncryptedStorage.forTesting(createDummyPrefs()),
            legacyPrefs = createDummyPrefs()
        )

        val services = migrator.parseServices(json)
        assertEquals(2, services.size)

        val chatgpt = services[0]
        assertEquals("chatgpt", chatgpt.id)
        assertEquals("ChatGPT", chatgpt.name)
        assertEquals("https://chatgpt.com", chatgpt.url)
        assertEquals("https://openai.com/favicon.ico", chatgpt.faviconUrl)
        assertEquals("https://cdn.freebiesupply.com/logos/large/2x/chatgpt-symbol.png", chatgpt.iconPath)
        assertEquals("Mozilla/5.0 (Android; Mobile; rv:130.0)", chatgpt.customUserAgent)
        assertEquals("ColAI-Mobile", chatgpt.customHeaders?.get("X-Custom-Client"))
        assertEquals("sess-default-1", chatgpt.widgetSessionId)
        assertTrue(chatgpt.notificationsEnabled)
        assertEquals(0, chatgpt.sortOrder)

        val claude = services[1]
        assertEquals("claude", claude.id)
        assertEquals("Claude", claude.name)
        assertNull(claude.iconPath)
        assertNull(claude.customHeaders)
        assertEquals(1, claude.sortOrder)
    }

    @Test
    fun testParseSessionsJson() {
        val json = """
        [
          {
            "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
            "serviceId": "chatgpt",
            "accountName": "Personal Account",
            "isDefault": true,
            "lastAccessed": "2026-02-01T08:00:00.000Z",
            "cookieStorePath": "/data/user/0/com.ujwal.colai/app_data/cookies_chatgpt_1",
            "notificationsEnabled": true,
            "themeMode": "dark",
            "customColors": "#10A37F"
          }
        ]
        """.trimIndent()

        val migrator = LegacyDataMigrator(
            serviceDao = FakeServiceDao(),
            sessionDao = FakeSessionDao(),
            encryptedStorage = EncryptedStorage.forTesting(createDummyPrefs()),
            legacyPrefs = createDummyPrefs()
        )

        val sessions = migrator.parseSessions(json)
        assertEquals(1, sessions.size)

        val session = sessions[0]
        assertEquals("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", session.id)
        assertEquals("chatgpt", session.serviceId)
        assertEquals("Personal Account", session.accountName)
        assertTrue(session.isDefault)
        assertEquals("dark", session.themeMode)
        assertEquals("#10A37F", session.customColors)
    }

    private fun createDummyPrefs(): SharedPreferences {
        val map = mutableMapOf<String, Any?>()
        return object : SharedPreferences {
            override fun getAll(): MutableMap<String, *> = map
            override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
            override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = null
            override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
            override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
            override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
            override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
            override fun contains(key: String): Boolean = map.containsKey(key)
            override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
                override fun putString(k: String, v: String?) = this.also { map[k] = v }
                override fun putStringSet(k: String, v: MutableSet<String>?) = this
                override fun putInt(k: String, v: Int) = this.also { map[k] = v }
                override fun putLong(k: String, v: Long) = this.also { map[k] = v }
                override fun putFloat(k: String, v: Float) = this.also { map[k] = v }
                override fun putBoolean(k: String, v: Boolean) = this.also { map[k] = v }
                override fun remove(k: String) = this.also { map.remove(k) }
                override fun clear() = this.also { map.clear() }
                override fun commit(): Boolean = true
                override fun apply() {}
            }
            override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }
    }

    private class FakeServiceDao : ServiceDao {
        val inserted = mutableListOf<AIService>()
        override fun getAllServices(): Flow<List<AIService>> = flowOf(inserted)
        override suspend fun getAllServicesList(): List<AIService> = inserted
        override fun getServiceById(id: String): Flow<AIService?> = flowOf(inserted.find { it.id == id })
        override suspend fun getServiceByIdSync(id: String): AIService? = inserted.find { it.id == id }
        override suspend fun insertService(service: AIService) { inserted.add(service) }
        override suspend fun insertServices(services: List<AIService>) { inserted.addAll(services) }
        override suspend fun updateService(service: AIService) {}
        override suspend fun deleteService(service: AIService) { inserted.remove(service) }
        override suspend fun deleteServiceById(id: String) { inserted.removeAll { it.id == id } }
        override suspend fun countServices(): Int = inserted.size
    }

    private class FakeSessionDao : SessionDao {
        val inserted = mutableListOf<Session>()
        override fun getSessionsForService(serviceId: String): Flow<List<Session>> = flowOf(inserted.filter { it.serviceId == serviceId })
        override suspend fun getSessionsForServiceList(serviceId: String): List<Session> = inserted.filter { it.serviceId == serviceId }
        override fun getAllSessions(): Flow<List<Session>> = flowOf(inserted)
        override suspend fun getAllSessionsList(): List<Session> = inserted
        override fun getSessionById(id: String): Flow<Session?> = flowOf(inserted.find { it.id == id })
        override suspend fun getSessionByIdSync(id: String): Session? = inserted.find { it.id == id }
        override suspend fun getDefaultSessionForService(serviceId: String): Session? = inserted.find { it.serviceId == serviceId && it.isDefault }
        override suspend fun insertSession(session: Session) { inserted.add(session) }
        override suspend fun insertSessions(sessions: List<Session>) { inserted.addAll(sessions) }
        override suspend fun updateSession(session: Session) {}
        override suspend fun deleteSession(session: Session) { inserted.remove(session) }
        override suspend fun deleteSessionById(id: String) { inserted.removeAll { it.id == id } }
        override suspend fun deleteAllSessions() { inserted.clear() }
        override suspend fun clearDefaultFlagsForService(serviceId: String) {}
        override suspend fun updateLastAccessed(sessionId: String, timestamp: Long) {}
        override suspend fun countSessions(): Int = inserted.size
        override suspend fun countSessionsForService(serviceId: String): Int = inserted.count { it.serviceId == serviceId }
    }
}
