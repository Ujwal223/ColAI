package com.ujwal.colai.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkResolverTest {

    @Test
    fun testResolveServiceIdFromHostChatGPT() {
        assertEquals("chatgpt", DeepLinkResolver.resolveServiceIdFromHost("chatgpt.com"))
        assertEquals("chatgpt", DeepLinkResolver.resolveServiceIdFromHost("www.chatgpt.com"))
        assertEquals("chatgpt", DeepLinkResolver.resolveServiceIdFromHost("chatgpt.com/c/123"))
        assertEquals("chatgpt", DeepLinkResolver.resolveServiceIdFromHost("chat.openai.com"))
        assertEquals("chatgpt", DeepLinkResolver.resolveServiceIdFromHost("openai.com"))
    }

    @Test
    fun testResolveServiceIdFromHostClaude() {
        assertEquals("claude", DeepLinkResolver.resolveServiceIdFromHost("claude.ai"))
        assertEquals("claude", DeepLinkResolver.resolveServiceIdFromHost("www.claude.ai"))
        assertEquals("claude", DeepLinkResolver.resolveServiceIdFromHost("claude.ai/chat/new"))
    }

    @Test
    fun testResolveServiceIdFromHostDeepSeek() {
        assertEquals("deepseek", DeepLinkResolver.resolveServiceIdFromHost("deepseek.com"))
        assertEquals("deepseek", DeepLinkResolver.resolveServiceIdFromHost("chat.deepseek.com"))
        assertEquals("deepseek", DeepLinkResolver.resolveServiceIdFromHost("www.deepseek.com"))
    }

    @Test
    fun testResolveServiceIdFromHostGrok() {
        assertEquals("grok", DeepLinkResolver.resolveServiceIdFromHost("grok.com"))
        assertEquals("grok", DeepLinkResolver.resolveServiceIdFromHost("www.grok.com"))
        assertEquals("grok", DeepLinkResolver.resolveServiceIdFromHost("x.ai"))
    }

    @Test
    fun testResolveServiceIdFromHostGemini() {
        assertEquals("gemini", DeepLinkResolver.resolveServiceIdFromHost("gemini.google.com"))
        assertEquals("gemini", DeepLinkResolver.resolveServiceIdFromHost("bard.google.com"))
    }

    @Test
    fun testResolveServiceIdFromHostPerplexity() {
        assertEquals("perplexity", DeepLinkResolver.resolveServiceIdFromHost("perplexity.ai"))
        assertEquals("perplexity", DeepLinkResolver.resolveServiceIdFromHost("www.perplexity.ai"))
    }

    @Test
    fun testResolveServiceIdFromUnknownHostReturnsNull() {
        assertNull(DeepLinkResolver.resolveServiceIdFromHost("google.com"))
        assertNull(DeepLinkResolver.resolveServiceIdFromHost("randomai.org"))
        assertNull(DeepLinkResolver.resolveServiceIdFromHost(null))
        assertNull(DeepLinkResolver.resolveServiceIdFromHost(""))
    }

    @Test
    fun testExtractHost() {
        assertEquals("chatgpt.com", DeepLinkResolver.extractHost("https://chatgpt.com/c/12345"))
        assertEquals("chat.openai.com", DeepLinkResolver.extractHost("https://chat.openai.com/g/g-abc"))
        assertEquals("claude.ai", DeepLinkResolver.extractHost("http://claude.ai:443/chat?foo=bar#section"))
        assertEquals("gemini.google.com", DeepLinkResolver.extractHost("https://gemini.google.com"))
        assertNull(DeepLinkResolver.extractHost("ftp://example.com"))
        assertNull(DeepLinkResolver.extractHost("not_a_url"))
    }

    @Test
    fun testResolveUrlWebDomains() {
        // ChatGPT
        val chatGptTarget = DeepLinkResolver.resolveUrl("https://chatgpt.com/c/chat-123")
        assertTrue(chatGptTarget is DeepLinkTarget.OpenService)
        assertEquals("chatgpt", (chatGptTarget as DeepLinkTarget.OpenService).serviceId)
        assertEquals("https://chatgpt.com/c/chat-123", chatGptTarget.targetUrl)

        // ChatGPT legacy subdomain
        val openAiTarget = DeepLinkResolver.resolveUrl("https://chat.openai.com")
        assertTrue(openAiTarget is DeepLinkTarget.OpenService)
        assertEquals("chatgpt", (openAiTarget as DeepLinkTarget.OpenService).serviceId)

        // Claude
        val claudeTarget = DeepLinkResolver.resolveUrl("https://claude.ai/chat/new")
        assertTrue(claudeTarget is DeepLinkTarget.OpenService)
        assertEquals("claude", (claudeTarget as DeepLinkTarget.OpenService).serviceId)

        // DeepSeek
        val deepseekTarget = DeepLinkResolver.resolveUrl("https://chat.deepseek.com")
        assertTrue(deepseekTarget is DeepLinkTarget.OpenService)
        assertEquals("deepseek", (deepseekTarget as DeepLinkTarget.OpenService).serviceId)

        // Grok
        val grokTarget = DeepLinkResolver.resolveUrl("https://grok.com")
        assertTrue(grokTarget is DeepLinkTarget.OpenService)
        assertEquals("grok", (grokTarget as DeepLinkTarget.OpenService).serviceId)

        // Grok x.ai
        val xAiTarget = DeepLinkResolver.resolveUrl("https://x.ai")
        assertTrue(xAiTarget is DeepLinkTarget.OpenService)
        assertEquals("grok", (xAiTarget as DeepLinkTarget.OpenService).serviceId)

        // Gemini
        val geminiTarget = DeepLinkResolver.resolveUrl("https://gemini.google.com/app")
        assertTrue(geminiTarget is DeepLinkTarget.OpenService)
        assertEquals("gemini", (geminiTarget as DeepLinkTarget.OpenService).serviceId)

        // Perplexity
        val perplexityTarget = DeepLinkResolver.resolveUrl("https://perplexity.ai/search?q=kotlin")
        assertTrue(perplexityTarget is DeepLinkTarget.OpenService)
        assertEquals("perplexity", (perplexityTarget as DeepLinkTarget.OpenService).serviceId)
    }

    @Test
    fun testResolveUrlCustomSchemeColai() {
        // Widget service launch
        val widgetService = DeepLinkResolver.resolveUrl("colai://widget/service/claude")
        assertTrue(widgetService is DeepLinkTarget.OpenService)
        assertEquals("claude", (widgetService as DeepLinkTarget.OpenService).serviceId)
        assertNull(widgetService.sessionId)

        // Widget service launch with specific session
        val widgetSessionService = DeepLinkResolver.resolveUrl("colai://widget/service/claude/sess-456")
        assertTrue(widgetSessionService is DeepLinkTarget.OpenService)
        assertEquals("claude", (widgetSessionService as DeepLinkTarget.OpenService).serviceId)
        assertEquals("sess-456", widgetSessionService.sessionId)

        // Widget mic / voice
        val widgetMic = DeepLinkResolver.resolveUrl("colai://widget/mic")
        assertTrue(widgetMic is DeepLinkTarget.Voice)

        // Widget search
        val widgetSearch = DeepLinkResolver.resolveUrl("colai://widget/search")
        assertTrue(widgetSearch is DeepLinkTarget.Search)

        // Quick open by ID
        val openDeepSeek = DeepLinkResolver.resolveUrl("colai://open/deepseek")
        assertTrue(openDeepSeek is DeepLinkTarget.OpenService)
        assertEquals("deepseek", (openDeepSeek as DeepLinkTarget.OpenService).serviceId)

        // Open with target URL
        val openWithUrl = DeepLinkResolver.resolveUrl("colai://open/chatgpt?url=https%3A%2F%2Fchatgpt.com%2Fc%2Fabc")
        assertTrue(openWithUrl is DeepLinkTarget.OpenService)
        assertEquals("chatgpt", (openWithUrl as DeepLinkTarget.OpenService).serviceId)
        assertEquals("https://chatgpt.com/c/abc", (openWithUrl as DeepLinkTarget.OpenService).targetUrl)

        // Search with query
        val searchWithQuery = DeepLinkResolver.resolveUrl("colai://search?q=architecture")
        assertTrue(searchWithQuery is DeepLinkTarget.Search)
        assertEquals("architecture", (searchWithQuery as DeepLinkTarget.Search).query)

        // Direct voice
        val directMic = DeepLinkResolver.resolveUrl("colai://mic")
        assertTrue(directMic is DeepLinkTarget.Voice)
    }

    @Test
    fun testResolveUrlEmbeddedInText() {
        val sharedText = "Check out this AI response: https://claude.ai/chat/abc123xyz"
        val resolved = DeepLinkResolver.resolveUrl(sharedText)
        assertNotNull(resolved)
        assertTrue(resolved is DeepLinkTarget.OpenService)
        assertEquals("claude", (resolved as DeepLinkTarget.OpenService).serviceId)
        assertEquals("https://claude.ai/chat/abc123xyz", resolved.targetUrl)
    }

    @Test
    fun testResolveUrlInvalid() {
        assertNull(DeepLinkResolver.resolveUrl(null))
        assertNull(DeepLinkResolver.resolveUrl(""))
        assertNull(DeepLinkResolver.resolveUrl("https://unsupported-site.com/foo"))
        assertNull(DeepLinkResolver.resolveUrl("plain text without url"))
    }

    @Test
    fun testConstants() {
        assertEquals("colai", DeepLinkResolver.SCHEME_COLAI)
        assertEquals("widget", DeepLinkResolver.HOST_WIDGET)
        assertEquals("open", DeepLinkResolver.HOST_OPEN)
        assertEquals("search", DeepLinkResolver.HOST_SEARCH)
        assertEquals("mic", DeepLinkResolver.HOST_MIC)
        assertEquals("com.ujwal.colai.action.WIDGET_CLICK", DeepLinkResolver.ACTION_WIDGET_CLICK)
        assertEquals("com.ujwal.colai.action.WIDGET_MIC", DeepLinkResolver.ACTION_WIDGET_MIC)
        assertEquals("com.ujwal.colai.action.WIDGET_SEARCH", DeepLinkResolver.ACTION_WIDGET_SEARCH)
    }
}
