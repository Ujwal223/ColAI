package com.ujwal.colai.core.data

import android.content.SharedPreferences
import com.ujwal.colai.core.database.ServiceDao
import com.ujwal.colai.core.database.SessionDao
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultServicesRepositoryTest {

    @Test
    fun testDefaultServicesListIntegrity() {
        val services = DefaultServicesRepository.getDefaultServices()
        assertEquals(6, services.size)

        val ids = services.map { it.id }.toSet()
        assertTrue(ids.contains(DefaultServicesRepository.ID_CHATGPT))
        assertTrue(ids.contains(DefaultServicesRepository.ID_CLAUDE))
        assertTrue(ids.contains(DefaultServicesRepository.ID_DEEPSEEK))
        assertTrue(ids.contains(DefaultServicesRepository.ID_GROK))
        assertTrue(ids.contains(DefaultServicesRepository.ID_GEMINI))
        assertTrue(ids.contains(DefaultServicesRepository.ID_PERPLEXITY))

        for (service in services) {
            assertTrue(service.name.isNotBlank())
            assertTrue(service.url.startsWith("https://"))
            assertTrue(service.faviconUrl.isNotBlank())
        }
    }

    @Test
    fun testSeedIfEmptyPopulatesDatabase() = runBlocking {
        val serviceDao = FakeServiceDao()
        val sessionDao = FakeSessionDao()
        val prefs = createDummyPrefs()
        val storage = EncryptedStorage.forTesting(prefs)

        val repo = DefaultServicesRepository(
            serviceDao = serviceDao,
            sessionDao = sessionDao,
            encryptedStorage = storage
        )

        val seededCount = repo.seedIfEmpty()
        assertEquals(6, seededCount)
        assertEquals(6, serviceDao.inserted.size)
        assertEquals(6, sessionDao.inserted.size)

        assertEquals("chatgpt", storage.getActiveServiceId())
        assertNotNull(storage.getActiveSessionId())

        // Verify every seeded session is linked to its service and set as default
        for (session in sessionDao.inserted) {
            assertTrue(session.isDefault)
            assertEquals("Personal", session.accountName)
            val parentService = serviceDao.inserted.find { it.id == session.serviceId }
            if (parentService?.id == DefaultServicesRepository.ID_CHATGPT) {
                assertEquals(session.id, parentService?.widgetSessionId)
            } else {
                assertEquals(null, parentService?.widgetSessionId)
            }
        }

        // Running again when already populated should return 0
        val secondSeed = repo.seedIfEmpty()
        assertEquals(0, secondSeed)
        assertEquals(6, serviceDao.inserted.size)
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
