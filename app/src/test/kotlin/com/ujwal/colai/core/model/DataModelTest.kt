package com.ujwal.colai.core.model

import com.ujwal.colai.core.database.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelTest {

    @Test
    fun testAIServiceCreationAndDefaults() {
        val service = AIService(
            id = "chatgpt",
            name = "ChatGPT",
            url = "https://chatgpt.com",
            faviconUrl = "https://openai.com/favicon.ico"
        )

        assertEquals("chatgpt", service.id)
        assertEquals("ChatGPT", service.name)
        assertEquals("https://chatgpt.com", service.url)
        assertEquals("https://openai.com/favicon.ico", service.faviconUrl)
        assertNull(service.iconPath)
        assertNull(service.customUserAgent)
        assertNull(service.customHeaders)
        assertNull(service.widgetSessionId)
        assertTrue(service.notificationsEnabled)
        assertEquals(0, service.sortOrder)
        assertTrue(service.createdAt > 0)
    }

    @Test
    fun testSessionCreationAndContainerId() {
        val sessionUuid = "b845e227-6a97-4089-8d14-38cce8e00123"
        val session = Session(
            id = sessionUuid,
            serviceId = "chatgpt",
            accountName = "Work Account",
            isDefault = true
        )

        assertEquals(sessionUuid, session.id)
        assertEquals("chatgpt", session.serviceId)
        assertEquals("Work Account", session.accountName)
        assertTrue(session.isDefault)
        assertTrue(session.notificationsEnabled)
        assertNull(session.themeMode)
        assertNull(session.customColors)
        assertTrue(session.lastAccessed > 0)
    }

    @Test
    fun testTypeConvertersStringMap() {
        val converters = Converters()
        val originalMap = mapOf(
            "Authorization" to "Bearer test-token",
            "X-Custom-Header" to "CustomValue"
        )

        val serialized = converters.fromStringMap(originalMap)
        assertNotNull(serialized)

        val deserialized = converters.toStringMap(serialized)
        assertNotNull(deserialized)
        assertEquals("Bearer test-token", deserialized?.get("Authorization"))
        assertEquals("CustomValue", deserialized?.get("X-Custom-Header"))

        assertNull(converters.fromStringMap(null))
        assertNull(converters.toStringMap(null))
        assertNull(converters.toStringMap(""))
    }
}
