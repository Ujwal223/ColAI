package com.ujwal.colai.core.engine

import android.net.Uri

/**
 * Generator and manager for client User-Agent strings.
 *
 * Provides latest stable Firefox for Android (rv:135.0) and Desktop strings
 * matching the underlying Mozilla GeckoView engine to guarantee seamless compatibility
 * with AI service providers (ChatGPT, Claude, Google Gemini, DeepSeek, Grok, Perplexity)
 * and unblock Google OAuth/SSO flows without triggering "This browser or app may not be secure".
 */
object UserAgentGenerator {

    /**
     * Firefox on Android 15 (Mobile) - Latest Stable User-Agent.
     * Matches the underlying GeckoView/Gecko engine and Android OS,
     * ensuring Google OAuth/SSO flows succeed without "This browser or app may not be secure" errors.
     */
    const val FIREFOX_MOBILE_DEFAULT: String =
        "Mozilla/5.0 (Android 15; Mobile; rv:135.0) Gecko/135.0 Firefox/135.0"

    /**
     * Firefox on Windows (Desktop) - Latest Stable User-Agent for desktop mode.
     */
    const val FIREFOX_DESKTOP_DEFAULT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0"

    /**
     * Apple Safari on iOS 18.3 (iPhone) maintained for legacy references.
     */
    const val APPLE_SAFARI_MOBILE_DEFAULT: String =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Mobile/15E148 Safari/604.1"

    /**
     * Apple Safari on macOS maintained for legacy references.
     */
    const val APPLE_SAFARI_DESKTOP_DEFAULT: String =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15"

    /**
     * Specific User-Agent optimized for Google OAuth login endpoints and Gemini.
     * Uses Firefox on Android matching GeckoView to ensure seamless Google Sign-In.
     */
    const val GOOGLE_AUTH_USER_AGENT: String = FIREFOX_MOBILE_DEFAULT

    /**
     * Generates the optimal User-Agent string for the given target URL and mode.
     *
     * @param url The destination web address, or null.
     * @param isDesktopMode Whether desktop rendering is requested.
     * @return The user agent string to set on [org.mozilla.geckoview.GeckoSessionSettings].
     */
    fun getOptimizedUserAgent(url: String?, isDesktopMode: Boolean = false): String {
        if (isDesktopMode) {
            return FIREFOX_DESKTOP_DEFAULT
        }

        if (url.isNullOrBlank()) {
            return FIREFOX_MOBILE_DEFAULT
        }

        val host = try {
            java.net.URI(url).host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: try {
            Uri.parse(url)?.host?.lowercase()
        } catch (_: Exception) {
            null
        } ?: ""

        return when {
            // Google Accounts / OAuth & Gemini
            host.contains("accounts.google.com") || host.contains("gemini.google.com") -> GOOGLE_AUTH_USER_AGENT
            // OpenAI / ChatGPT
            host.contains("chatgpt.com") || host.contains("openai.com") -> FIREFOX_MOBILE_DEFAULT
            // Anthropic Claude
            host.contains("claude.ai") || host.contains("anthropic.com") -> FIREFOX_MOBILE_DEFAULT
            // DeepSeek
            host.contains("deepseek.com") -> FIREFOX_MOBILE_DEFAULT
            // X / Grok
            host.contains("grok.com") || host.contains("x.ai") -> FIREFOX_MOBILE_DEFAULT
            // Perplexity AI
            host.contains("perplexity.ai") -> FIREFOX_MOBILE_DEFAULT
            // Default fallback
            else -> FIREFOX_MOBILE_DEFAULT
        }
    }

    /**
     * Produces standard anti-detection and privacy HTTP headers.
     *
     * Injects standard client hints, Sec-GPC (Global Privacy Control),
     * and DNT (Do Not Track) headers to mitigate Cloudflare Turnstile,
     * bot scoring, and OAuth detection challenges.
     *
     * @param url The destination web address, or null.
     * @param isDesktopMode Whether desktop rendering is requested.
     * @return Map of header names to header values.
     */
    fun getAntiDetectionHeaders(url: String? = null, isDesktopMode: Boolean = false): Map<String, String> {
        val platform = if (isDesktopMode) "\"Windows\"" else "\"Android\""
        val mobile = if (isDesktopMode) "?0" else "?1"

        return mapOf(
            "Sec-CH-UA-Mobile" to mobile,
            "Sec-CH-UA-Platform" to platform,
            "Sec-GPC" to "1",
            "DNT" to "1",
            "Upgrade-Insecure-Requests" to "1",
            "Accept-Language" to "en-US,en;q=0.9"
        )
    }
}
