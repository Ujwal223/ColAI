package com.ujwal.colai.feature.home

import android.app.Application
import android.content.SharedPreferences
import com.ujwal.colai.core.data.DefaultServicesRepository
import com.ujwal.colai.core.database.ServiceDao
import com.ujwal.colai.core.database.SessionDao
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var serviceDao: FakeServiceDao
    private lateinit var sessionDao: FakeSessionDao
    private lateinit var encryptedStorage: EncryptedStorage
    private lateinit var repository: DefaultServicesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        serviceDao = FakeServiceDao()
        sessionDao = FakeSessionDao()
        val prefs = createDummyPrefs()
        encryptedStorage = EncryptedStorage.forTesting(prefs)
        repository = DefaultServicesRepository(serviceDao, sessionDao, encryptedStorage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchFilteringWorksCorrectly() = runTest {
        val app = Application()
        val service1 = AIService("chatgpt", "ChatGPT", "https://chatgpt.com", "https://chatgpt.com/fav.ico")
        val service2 = AIService("claude", "Claude", "https://claude.ai", "https://claude.ai/fav.ico")
        serviceDao.inserted.add(service1)
        serviceDao.inserted.add(service2)
        serviceDao.servicesFlow.value = listOf(service1, service2)

        val viewModel = HomeViewModel(
            application = app,
            serviceDao = serviceDao,
            sessionDao = sessionDao,
            encryptedStorage = encryptedStorage,
            defaultServicesRepository = repository,
            ioDispatcher = testDispatcher
        )

        viewModel.onSearchQueryChanged("claude")
        assertEquals("claude", viewModel.searchQuery.value)

        viewModel.clearSearch()
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun testSelectServiceUpdatesActiveStorage() = runTest {
        val app = Application()
        val service = AIService("deepseek", "DeepSeek", "https://chat.deepseek.com", "fav")
        val session = Session("sess-1", "deepseek", "Personal", isDefault = true)
        serviceDao.inserted.add(service)
        sessionDao.inserted.add(session)
        serviceDao.servicesFlow.value = listOf(service)
        sessionDao.sessionsFlow.value = listOf(session)

        val viewModel = HomeViewModel(
            application = app,
            serviceDao = serviceDao,
            sessionDao = sessionDao,
            encryptedStorage = encryptedStorage,
            defaultServicesRepository = repository,
            ioDispatcher = testDispatcher
        )

        viewModel.selectService(service, session)

        assertEquals("deepseek", encryptedStorage.getActiveServiceId())
        assertEquals("sess-1", encryptedStorage.getActiveSessionId())
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
        val servicesFlow = MutableStateFlow<List<AIService>>(emptyList())

        override fun getAllServices(): Flow<List<AIService>> = servicesFlow
        override suspend fun getAllServicesList(): List<AIService> = inserted
        override fun getServiceById(id: String): Flow<AIService?> = MutableStateFlow(inserted.find { it.id == id })
        override suspend fun getServiceByIdSync(id: String): AIService? = inserted.find { it.id == id }
        override suspend fun insertService(service: AIService) { inserted.add(service); servicesFlow.value = inserted.toList() }
        override suspend fun insertServices(services: List<AIService>) { inserted.addAll(services); servicesFlow.value = inserted.toList() }
        override suspend fun updateService(service: AIService) {}
        override suspend fun deleteService(service: AIService) { inserted.remove(service); servicesFlow.value = inserted.toList() }
        override suspend fun deleteServiceById(id: String) { inserted.removeAll { it.id == id }; servicesFlow.value = inserted.toList() }
        override suspend fun countServices(): Int = inserted.size
    }

    private class FakeSessionDao : SessionDao {
        val inserted = mutableListOf<Session>()
        val sessionsFlow = MutableStateFlow<List<Session>>(emptyList())

        override fun getSessionsForService(serviceId: String): Flow<List<Session>> = MutableStateFlow(inserted.filter { it.serviceId == serviceId })
        override suspend fun getSessionsForServiceList(serviceId: String): List<Session> = inserted.filter { it.serviceId == serviceId }
        override fun getAllSessions(): Flow<List<Session>> = sessionsFlow
        override suspend fun getAllSessionsList(): List<Session> = inserted
        override fun getSessionById(id: String): Flow<Session?> = MutableStateFlow(inserted.find { it.id == id })
        override suspend fun getSessionByIdSync(id: String): Session? = inserted.find { it.id == id }
        override suspend fun getDefaultSessionForService(serviceId: String): Session? = inserted.find { it.serviceId == serviceId && it.isDefault }
        override suspend fun insertSession(session: Session) { inserted.add(session); sessionsFlow.value = inserted.toList() }
        override suspend fun insertSessions(sessions: List<Session>) { inserted.addAll(sessions); sessionsFlow.value = inserted.toList() }
        override suspend fun updateSession(session: Session) {}
        override suspend fun deleteSession(session: Session) { inserted.remove(session); sessionsFlow.value = inserted.toList() }
        override suspend fun deleteSessionById(id: String) { inserted.removeAll { it.id == id }; sessionsFlow.value = inserted.toList() }
        override suspend fun deleteAllSessions() { inserted.clear(); sessionsFlow.value = emptyList() }
        override suspend fun clearDefaultFlagsForService(serviceId: String) {}
        override suspend fun updateLastAccessed(sessionId: String, timestamp: Long) {}
        override suspend fun countSessions(): Int = inserted.size
        override suspend fun countSessionsForService(serviceId: String): Int = inserted.count { it.serviceId == serviceId }
    }
}
