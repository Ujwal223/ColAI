package com.ujwal.colai.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentGeneratorTest {

    @Test
    fun testDefaultMobileUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent(null)
        assertNotNull(ua)
        assertTrue(ua.contains("Android"))
        assertTrue(ua.contains("Firefox"))
        assertFalse(ua.contains("; wv;")) // Must NOT have Android WebView flag
    }

    @Test
    fun testDesktopUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent("https://chatgpt.com", isDesktopMode = true)
        assertTrue(ua.contains("Windows") || ua.contains("Linux") || ua.contains("rv:135.0"))
        assertTrue(ua.contains("Firefox"))
    }

    @Test
    fun testGoogleAuthUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent("https://accounts.google.com/signin/v2/identifier")
        assertTrue(ua.contains("Android"))
        assertTrue(ua.contains("Firefox"))
        assertEquals(UserAgentGenerator.GOOGLE_AUTH_USER_AGENT, ua)
    }

    @Test
    fun testOpenAiUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent("https://chatgpt.com")
        assertTrue(ua.contains("Firefox"))
    }

    @Test
    fun testClaudeUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent("https://claude.ai")
        assertTrue(ua.contains("Firefox"))
    }

    @Test
    fun testDeepSeekUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent("https://chat.deepseek.com")
        assertTrue(ua.contains("Firefox"))
    }

    @Test
    fun testGrokUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent("https://grok.com")
        assertTrue(ua.contains("Firefox"))
    }

    @Test
    fun testPerplexityUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent("https://www.perplexity.ai")
        assertTrue(ua.contains("Firefox"))
    }

    @Test
    fun testGeminiUserAgent() {
        val ua = UserAgentGenerator.getOptimizedUserAgent("https://gemini.google.com/app")
        assertTrue(ua.contains("Firefox"))
        assertEquals(UserAgentGenerator.GOOGLE_AUTH_USER_AGENT, ua)
    }

    @Test
    fun testAntiDetectionHeaders() {
        val headers = UserAgentGenerator.getAntiDetectionHeaders("https://chatgpt.com")
        assertEquals("?1", headers["Sec-CH-UA-Mobile"])
        assertEquals("\"Android\"", headers["Sec-CH-UA-Platform"])
        assertEquals("1", headers["Sec-GPC"])
        assertEquals("1", headers["DNT"])
        assertEquals("1", headers["Upgrade-Insecure-Requests"])
        assertEquals("en-US,en;q=0.9", headers["Accept-Language"])
    }

    @Test
    fun testDesktopAntiDetectionHeaders() {
        val headers = UserAgentGenerator.getAntiDetectionHeaders("https://chatgpt.com", isDesktopMode = true)
        assertEquals("?0", headers["Sec-CH-UA-Mobile"])
        assertEquals("\"Windows\"", headers["Sec-CH-UA-Platform"])
        assertEquals("1", headers["Sec-GPC"])
        assertEquals("1", headers["DNT"])
    }
}
