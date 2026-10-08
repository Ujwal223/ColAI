package com.ujwal.colai.feature.widget

import com.ujwal.colai.core.model.AIService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ServiceWidgetMediumProviderTest {

    @Test
    fun testMediumActionConstants() {
        assertEquals("com.ujwal.colai.action.WIDGET_CLICK", ServiceWidgetMediumProvider.ACTION_WIDGET_CLICK)
        assertEquals("com.ujwal.colai.action.WIDGET_MIC", ServiceWidgetMediumProvider.ACTION_WIDGET_MIC)
        assertEquals("com.ujwal.colai.action.WIDGET_SEARCH", ServiceWidgetMediumProvider.ACTION_WIDGET_SEARCH)
        assertEquals("com.ujwal.colai.action.REFRESH_WIDGET", ServiceWidgetMediumProvider.ACTION_REFRESH_WIDGET)
        assertEquals("extra_service_id", ServiceWidgetMediumProvider.EXTRA_SERVICE_ID)
        assertEquals("extra_action", ServiceWidgetMediumProvider.EXTRA_ACTION)
    }

    @Test
    fun testMediumServiceWidgetDataIntegration() {
        val service = AIService(
            id = "claude",
            name = "Claude",
            url = "https://claude.ai",
            faviconUrl = "https://claude.ai/favicon.ico",
            widgetSessionId = "session_claude"
        )
        assertNotNull(service)
        assertEquals("claude", service.id)
        assertEquals("Claude", service.name)
        assertEquals("session_claude", service.widgetSessionId)
    }
}
