package com.ujwal.colai.core.data

import android.content.Context
import com.ujwal.colai.core.database.ServiceDao
import com.ujwal.colai.core.model.AIService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LogoCacheManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var fakeContext: Context
    private lateinit var fakeDao: FakeServiceDao

    @Before
    fun setUp() {
        fakeDao = FakeServiceDao()
        val mockFilesDir = tempFolder.newFolder("files")
        fakeContext = object : android.content.ContextWrapper(null) {
            override fun getFilesDir(): File = mockFilesDir
        }
    }

    @Test
    fun testGetCachedLogoFileReturnsNullWhenNotCached() {
        val manager = LogoCacheManager(fakeContext, fakeDao)
        val file = manager.getCachedLogoFile("chatgpt")
        assertNull(file)
    }

    @Test
    fun testGetCachedLogoFileReturnsFileWhenPresent() {
        val manager = LogoCacheManager(fakeContext, fakeDao)
        val logosDir = File(fakeContext.filesDir, "logos")
        logosDir.mkdirs()
        val logoFile = File(logosDir, "chatgpt.png")
        logoFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        val retrieved = manager.getCachedLogoFile("chatgpt")
        assertNotNull(retrieved)
        assertEquals(logoFile.absolutePath, retrieved?.absolutePath)
    }

    @Test
    fun testClearCacheRemovesAllLogos() = runBlocking {
        val manager = LogoCacheManager(fakeContext, fakeDao)
        val logosDir = File(fakeContext.filesDir, "logos")
        logosDir.mkdirs()
        val logoFile = File(logosDir, "chatgpt.png")
        logoFile.writeBytes(byteArrayOf(1, 2, 3))

        assertTrue(logoFile.exists())
        val cleared = manager.clearCache()
        assertTrue(cleared)
        assertNull(manager.getCachedLogoFile("chatgpt"))
    }

    @Test
    fun testCacheAllServicesProgressCallback() = runBlocking {
        val manager = LogoCacheManager(fakeContext, fakeDao)
        val services = listOf(
            AIService("service1", "Service 1", "https://s1.ai", "invalid-url-1"),
            AIService("service2", "Service 2", "https://s2.ai", "invalid-url-2")
        )

        val progressReports = mutableListOf<String>()
        manager.cacheAllServices(services) { current, total, name ->
            progressReports.add("$current/$total: $name")
        }

        assertEquals(2, progressReports.size)
        assertEquals("1/2: Service 1", progressReports[0])
        assertEquals("2/2: Service 2", progressReports[1])
    }

    private class FakeServiceDao : ServiceDao {
        val services = mutableListOf<AIService>()
        override fun getAllServices(): Flow<List<AIService>> = MutableStateFlow(services)
        override suspend fun getAllServicesList(): List<AIService> = services
        override fun getServiceById(id: String): Flow<AIService?> = MutableStateFlow(services.find { it.id == id })
        override suspend fun getServiceByIdSync(id: String): AIService? = services.find { it.id == id }
        override suspend fun insertService(service: AIService) { services.add(service) }
        override suspend fun insertServices(services: List<AIService>) { this.services.addAll(services) }
        override suspend fun updateService(service: AIService) {
            val idx = services.indexOfFirst { it.id == service.id }
            if (idx >= 0) services[idx] = service
        }
        override suspend fun deleteService(service: AIService) { services.remove(service) }
        override suspend fun deleteServiceById(id: String) { services.removeAll { it.id == id } }
        override suspend fun countServices(): Int = services.size
    }
}
