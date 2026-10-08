package com.ujwal.colai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ujwal.colai.core.engine.GeckoSessionPool
import com.ujwal.colai.core.engine.UserAgentGenerator
import com.ujwal.colai.core.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoSessionSettings
import java.util.UUID

/**
 * Automated end-to-end and instrumented verification testing for:
 * 1. Mozilla GeckoView native multi-account container isolation (contextId partitioning).
 * 2. Google OAuth, Apple ID, and Microsoft SSO compatibility (preventing 'disallowed_useragent').
 * 3. Client hints and anti-detection header validation across AI service domains.
 */
@RunWith(AndroidJUnit4::class)
class ContainerIsolationTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ujwal.colai", appContext.packageName)
    }

    @Test
    fun testContainerContextIdIsolation() {
        // Generate distinct container session IDs for multiple isolated accounts
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

        // 1. Verify all contextIds are non-null and correctly assigned
        assertEquals(personalContainerId, personalSettings.contextId)
        assertEquals(workContainerId, workSettings.contextId)
        assertEquals(researchContainerId, researchSettings.contextId)

        // 2. Verify all contextIds are mutually unique and completely isolated
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
        // Google OAuth endpoints reject standard Android System WebViews with 'disallowed_useragent' (403).
        // ColAI must provide a clean mobile Firefox user agent without 'wv' (WebView) or 'Version/4.0' tokens.
        val googleAuthUrls = listOf(
            "https://accounts.google.com/signin/v2/identifier",
            "https://accounts.google.com/o/oauth2/auth",
            "https://gemini.google.com",
            "https://gemini.google.com/app"
        )

        for (url in googleAuthUrls) {
            val userAgent = UserAgentGenerator.getOptimizedUserAgent(url)
            assertNotNull("User agent must not be null for $url", userAgent)

            // Must identify as standard Firefox browser
            assertTrue("User agent must contain 'Firefox' for $url: $userAgent", userAgent.contains("Firefox"))
            assertTrue("User agent must contain 'Android' for $url: $userAgent", userAgent.contains("Android"))

            // Must NOT contain Android WebView signature tokens
            assertFalse("User agent must NOT contain '; wv;' for $url", userAgent.contains("; wv;"))
            assertFalse("User agent must NOT contain 'Version/4.0' for $url", userAgent.contains("Version/4.0"))
            assertFalse("User agent must NOT contain 'wv' for $url", userAgent.contains(" wv "))
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

            assertEquals("Mobile client hint must be '?1'", "?1", headers["Sec-CH-UA-Mobile"])
            assertEquals("Platform must be '\"Android\"'", "\"Android\"", headers["Sec-CH-UA-Platform"])
            assertEquals("Sec-GPC must be active ('1')", "1", headers["Sec-GPC"])
            assertEquals("DNT must be active ('1')", "1", headers["DNT"])
            assertEquals("Upgrade-Insecure-Requests must be '1'", "1", headers["Upgrade-Insecure-Requests"])
            assertTrue("Accept-Language must include en-US", headers["Accept-Language"]?.contains("en-US") == true)
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
