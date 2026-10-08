package com.ujwal.colai.feature.sessions

import android.app.Application
import android.content.SharedPreferences
import com.ujwal.colai.core.database.SessionDao
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var sessionDao: FakeSessionDao
    private lateinit var encryptedStorage: EncryptedStorage

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionDao = FakeSessionDao()
        val prefs = createDummyPrefs()
        encryptedStorage = EncryptedStorage.forTesting(prefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadSessionsForService() = runTest {
        val app = Application()
        val s1 = Session("id-1", "chatgpt", "Personal", isDefault = true)
        val s2 = Session("id-2", "chatgpt", "Work", isDefault = false)
        sessionDao.inserted.addAll(listOf(s1, s2))

        val vm = SessionsViewModel(app, sessionDao, encryptedStorage, testDispatcher)
        vm.loadSessionsForService("chatgpt")

        assertEquals(2, vm.sessions.value.size)
        assertEquals("id-1", vm.activeSessionId.value)
    }

    @Test
    fun testCreateNewSessionAddsContainer() = runTest {
        val app = Application()
        val vm = SessionsViewModel(app, sessionDao, encryptedStorage, testDispatcher)

        vm.createNewSession("claude", "Research Lab")

        assertEquals(1, vm.sessions.value.size)
        val created = vm.sessions.value.first()
        assertEquals("Research Lab", created.accountName)
        assertEquals("claude", created.serviceId)
        assertEquals(created.id, vm.activeSessionId.value)
        assertEquals(created.id, encryptedStorage.getActiveSessionId())
    }

    @Test
    fun testRenameSessionUpdatesName() = runTest {
        val app = Application()
        val s1 = Session("id-1", "deepseek", "Account 1", isDefault = true)
        sessionDao.inserted.add(s1)

        val vm = SessionsViewModel(app, sessionDao, encryptedStorage, testDispatcher)
        vm.loadSessionsForService("deepseek")

        vm.renameSession(s1, "Renamed Account")

        val updated = vm.sessions.value.find { it.id == "id-1" }
        assertEquals("Renamed Account", updated?.accountName)
    }

    @Test
    fun testDeleteSessionKeepsAtLeastOneDefault() = runTest {
        val app = Application()
        val s1 = Session("id-1", "grok", "Only Account", isDefault = true)
        sessionDao.inserted.add(s1)

        val vm = SessionsViewModel(app, sessionDao, encryptedStorage, testDispatcher)
        vm.loadSessionsForService("grok")

        vm.deleteSession("grok", "id-1")

        // Should have automatically generated a fallback Personal container
        assertEquals(1, vm.sessions.value.size)
        assertTrue(vm.sessions.value.first().isDefault)
        assertEquals("Personal", vm.sessions.value.first().accountName)
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
        override suspend fun insertSession(session: Session) {
            inserted.removeAll { it.id == session.id }
            inserted.add(session)
            sessionsFlow.value = inserted.toList()
        }
        override suspend fun insertSessions(sessions: List<Session>) {
            inserted.addAll(sessions)
            sessionsFlow.value = inserted.toList()
        }
        override suspend fun updateSession(session: Session) {
            val idx = inserted.indexOfFirst { it.id == session.id }
            if (idx >= 0) inserted[idx] = session
            sessionsFlow.value = inserted.toList()
        }
        override suspend fun deleteSession(session: Session) {
            inserted.remove(session)
            sessionsFlow.value = inserted.toList()
        }
        override suspend fun deleteSessionById(id: String) {
            inserted.removeAll { it.id == id }
            sessionsFlow.value = inserted.toList()
        }
        override suspend fun deleteAllSessions() {
            inserted.clear()
            sessionsFlow.value = emptyList()
        }
        override suspend fun clearDefaultFlagsForService(serviceId: String) {
            for (i in inserted.indices) {
                if (inserted[i].serviceId == serviceId) {
                    inserted[i] = inserted[i].copy(isDefault = false)
                }
            }
            sessionsFlow.value = inserted.toList()
        }
        override suspend fun updateLastAccessed(sessionId: String, timestamp: Long) {}
        override suspend fun countSessions(): Int = inserted.size
        override suspend fun countSessionsForService(serviceId: String): Int = inserted.count { it.serviceId == serviceId }
    }
}
