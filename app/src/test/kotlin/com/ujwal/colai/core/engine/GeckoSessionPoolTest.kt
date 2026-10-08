package com.ujwal.colai.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mozilla.geckoview.GeckoSessionSettings

class GeckoSessionPoolTest {

    @Test
    fun testContextIdIsolationInSettings() {
        val contextId1 = "session-account-personal-uuid"
        val contextId2 = "session-account-work-uuid"

        val settings1 = GeckoSessionSettings.Builder()
            .contextId(contextId1)
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .userAgentOverride(UserAgentGenerator.getOptimizedUserAgent("https://chatgpt.com"))
            .build()

        val settings2 = GeckoSessionSettings.Builder()
            .contextId(contextId2)
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .userAgentOverride(UserAgentGenerator.getOptimizedUserAgent("https://chatgpt.com"))
            .build()

        // Verify contextIds are separate and accurately preserved
        assertEquals(contextId1, settings1.contextId)
        assertEquals(contextId2, settings2.contextId)
        assertNotEquals(settings1.contextId, settings2.contextId)
    }

    @Test
    fun testPoolConstants() {
        assertEquals(5, GeckoSessionPool.MAX_POOL_CAPACITY)
    }
}
