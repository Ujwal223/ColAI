package com.ujwal.colai.core.engine

import com.ujwal.colai.core.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.GeckoSessionSettings
import java.util.UUID

/**
 * JVM verification for Mozilla GeckoView multi-account container isolation,
 * Google/Apple/Microsoft SSO compatibility, and anti-detection headers.
 */
class ContainerIsolationVerificationTest {

    @Test
    fun testContainerContextIdIsolation() {
        val personalContainerId = UUID.randomUUID().toString()
        val workContainerId = UUID.randomUUID().toString()
        val researchContainerId = UUID.randomUUID().toString()

        val personalSettings = GeckoSessionSettings.Builder()
            .contextId(personalContainerId)
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .userAgentOverride(UserAgentGenerator.getOptimizedUserAgent("https://chatgpt.com"))
            .build()

        val workSettings = GeckoSessionSettings.Builder()
            .contextId(workContainerId)
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .userAgentOverride(UserAgentGenerator.getOptimizedUserAgent("https://chatgpt.com"))
            .build()

        val researchSettings = GeckoSessionSettings.Builder()
            .contextId(researchContainerId)
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .userAgentOverride(UserAgentGenerator.getOptimizedUserAgent("https://claude.ai"))
            .build()

        // 1. Verify contextIds are properly assigned
        assertEquals(personalContainerId, personalSettings.contextId)
        assertEquals(workContainerId, workSettings.contextId)
        assertEquals(researchContainerId, researchSettings.contextId)

        // 2. Verify complete isolation
        assertNotEquals(personalSettings.contextId, workSettings.contextId)
        assertNotEquals(workSettings.contextId, researchSettings.contextId)
        assertNotEquals(personalSettings.contextId, researchSettings.contextId)

        // 3. Verify private mode is disabled so container data persists within its context
        assertFalse(personalSettings.usePrivateMode)
        assertFalse(workSettings.usePrivateMode)
        assertFalse(researchSettings.usePrivateMode)
    }

    @Test
    fun testGoogleAuthUserAgentBypassesDisallowedUserAgent() {
        val googleAuthUrls = listOf(
            "https://accounts.google.com/signin/v2/identifier",
            "https://accounts.google.com/o/oauth2/auth",
            "https://gemini.google.com",
            "https://gemini.google.com/app"
        )

        for (url in googleAuthUrls) {
            val userAgent = UserAgentGenerator.getOptimizedUserAgent(url)
            assertNotNull("User agent must not be null for $url", userAgent)
            assertTrue("User agent must contain 'Firefox' for $url: $userAgent", userAgent.contains("Firefox"))
            assertTrue("User agent must contain 'Android' for $url: $userAgent", userAgent.contains("Android"))
            assertFalse("User agent must NOT contain '; wv;' for $url", userAgent.contains("; wv;"))
            assertFalse("User agent must NOT contain 'Version/4.0' for $url", userAgent.contains("Version/4.0"))
        }
    }

    @Test
    fun testAppleAndMicrosoftSSOCompatibility() {
        val ssoUrls = listOf(
            "https://appleid.apple.com/auth/authorize",
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
            "https://auth.openai.com/authorize"
        )

        for (url in ssoUrls) {
            val userAgent = UserAgentGenerator.getOptimizedUserAgent(url)
            assertNotNull("User agent must not be null for $url", userAgent)
            assertTrue("User agent must identify as Firefox for $url", userAgent.contains("Firefox"))
            assertFalse("User agent must not have embedded WebView flag for $url", userAgent.contains("; wv;"))
        }
    }

    @Test
    fun testAntiDetectionAndPrivacyHeaders() {
        val aiServices = listOf(
            "https://chatgpt.com",
            "https://claude.ai",
            "https://deepseek.com",
            "https://grok.com",
            "https://gemini.google.com",
            "https://perplexity.ai"
        )

        for (url in aiServices) {
            val headers = UserAgentGenerator.getAntiDetectionHeaders(url)
            assertEquals("?1", headers["Sec-CH-UA-Mobile"])
            assertEquals("\"Android\"", headers["Sec-CH-UA-Platform"])
            assertEquals("1", headers["Sec-GPC"])
            assertEquals("1", headers["DNT"])
            assertEquals("1", headers["Upgrade-Insecure-Requests"])
            assertTrue(headers["Accept-Language"]?.contains("en-US") == true)
        }
    }

    @Test
    fun testMultipleSessionsPerServiceEntity() {
        val serviceId = "chatgpt"
        val session1 = Session(
            id = "sess_chatgpt_account1",
            serviceId = serviceId,
            accountName = "Personal Account",
            isDefault = true
        )
        val session2 = Session(
            id = "sess_chatgpt_account2",
            serviceId = serviceId,
            accountName = "Work Account",
            isDefault = false
        )

        assertEquals(serviceId, session1.serviceId)
        assertEquals(serviceId, session2.serviceId)
        assertNotEquals(session1.id, session2.id)
        assertNotEquals(session1.accountName, session2.accountName)
        assertTrue(session1.isDefault)
        assertFalse(session2.isDefault)
    }
}
