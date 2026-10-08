package com.ujwal.colai.feature.widget

import com.ujwal.colai.core.model.AIService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ServiceWidgetProviderTest {

    @Test
    fun testActionConstants() {
        assertEquals("com.ujwal.colai.action.WIDGET_CLICK", ServiceWidgetProvider.ACTION_WIDGET_CLICK)
        assertEquals("com.ujwal.colai.action.WIDGET_MIC", ServiceWidgetProvider.ACTION_WIDGET_MIC)
        assertEquals("com.ujwal.colai.action.WIDGET_SEARCH", ServiceWidgetProvider.ACTION_WIDGET_SEARCH)
        assertEquals("com.ujwal.colai.action.REFRESH_WIDGET", ServiceWidgetProvider.ACTION_REFRESH_WIDGET)
        assertEquals("extra_service_id", ServiceWidgetProvider.EXTRA_SERVICE_ID)
        assertEquals("extra_action", ServiceWidgetProvider.EXTRA_ACTION)
    }

    @Test
    fun testServiceWidgetDataModelIntegration() {
        val service = AIService(
            id = "chatgpt",
            name = "ChatGPT",
            url = "https://chatgpt.com",
            faviconUrl = "https://chatgpt.com/favicon.ico",
            widgetSessionId = "session_1"
        )
        assertNotNull(service)
        assertEquals("chatgpt", service.id)
        assertEquals("ChatGPT", service.name)
        assertEquals("session_1", service.widgetSessionId)
    }
}
