package com.ujwal.colai.util

import android.content.Intent
import android.net.Uri

/**
 * Resolved deep link action destination.
 */
sealed interface DeepLinkTarget {
    data class OpenService(
        val serviceId: String,
        val sessionId: String? = null,
        val targetUrl: String? = null,
        val initialAction: String? = null
    ) : DeepLinkTarget

    data class Search(val query: String? = null) : DeepLinkTarget

    data class Voice(
        val serviceId: String? = null,
        val sessionId: String? = null
    ) : DeepLinkTarget
}

/**
 * Resolves external HTTP/HTTPS AI URLs, custom scheme intents (colai://),
 * shared text intents, and widget broadcasts into structured [DeepLinkTarget] destinations.
 */
object DeepLinkResolver {

    const val SCHEME_COLAI = "colai"

    const val HOST_WIDGET = "widget"
    const val HOST_OPEN = "open"
    const val HOST_SEARCH = "search"
    const val HOST_MIC = "mic"

    const val ACTION_WIDGET_CLICK = "com.ujwal.colai.action.WIDGET_CLICK"
    const val ACTION_WIDGET_MIC = "com.ujwal.colai.action.WIDGET_MIC"
    const val ACTION_WIDGET_SEARCH = "com.ujwal.colai.action.WIDGET_SEARCH"

    const val EXTRA_SERVICE_ID = "extra_service_id"
    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_ACTION = "extra_action"

    private val URL_REGEX = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)

    /**
     * Extracts host name from a raw URL string without requiring Android Uri stub in JVM tests.
     */
    fun extractHost(url: String): String? {
        val trimmed = url.trim()
        val withoutProtocol = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
            else -> return null
        }
        val hostPart = withoutProtocol.substringBefore('/').substringBefore('?').substringBefore('#')
        val hostWithoutPort = hostPart.substringBefore(':').lowercase().trim()
        return hostWithoutPort.ifBlank { null }
    }

    /**
     * Maps an external web host or URL to a canonical ColAI service ID.
     */
    fun resolveServiceIdFromHost(host: String?): String? {
        val normalizedHost = host?.lowercase()?.trim() ?: return null
        return when {
            normalizedHost.contains("chatgpt.com") ||
                normalizedHost.contains("chat.openai.com") ||
                normalizedHost == "openai.com" ||
                normalizedHost.endsWith(".openai.com") -> "chatgpt"

            normalizedHost.contains("claude.ai") ||
                normalizedHost.endsWith(".claude.ai") -> "claude"

            normalizedHost.contains("deepseek.com") ||
                normalizedHost.endsWith(".deepseek.com") -> "deepseek"

            normalizedHost.contains("grok.com") ||
                normalizedHost.endsWith(".grok.com") ||
                normalizedHost == "x.ai" ||
                normalizedHost.endsWith(".x.ai") -> "grok"

            normalizedHost.contains("gemini.google.com") ||
                normalizedHost.contains("bard.google.com") -> "gemini"

            normalizedHost.contains("perplexity.ai") ||
                normalizedHost.endsWith(".perplexity.ai") -> "perplexity"

            else -> null
        }
    }

    /**
     * Resolves a raw URL string or custom scheme string into a [DeepLinkTarget].
     */
    fun resolveUrl(rawUrl: String?): DeepLinkTarget? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim()

        // 1. Custom scheme: colai://
        if (trimmed.startsWith("$SCHEME_COLAI://", ignoreCase = true)) {
            val withoutScheme = trimmed.substring("$SCHEME_COLAI://".length)
            val parts = withoutScheme.split("?", limit = 2)
            val pathPart = parts[0].trim('/')
            val queryPart = if (parts.size > 1) parts[1] else null

            val segments = pathPart.split("/").filter { it.isNotBlank() }
            val firstSegment = segments.getOrNull(0)?.lowercase()

            return when (firstSegment) {
                HOST_WIDGET -> {
                    val subAction = segments.getOrNull(1)?.lowercase()
                    when (subAction) {
                        "service" -> {
                            val serviceId = segments.getOrNull(2)
                            val sessionId = segments.getOrNull(3)
                            if (!serviceId.isNullOrBlank()) {
                                DeepLinkTarget.OpenService(serviceId = serviceId, sessionId = sessionId)
                            } else null
                        }
                        "input" -> {
                            val serviceId = segments.getOrNull(2)
                            val sessionId = segments.getOrNull(3)
                            if (!serviceId.isNullOrBlank()) {
                                DeepLinkTarget.OpenService(
                                    serviceId = serviceId,
                                    sessionId = sessionId,
                                    initialAction = "input"
                                )
                            } else null
                        }
                        "mic" -> {
                            val serviceId = segments.getOrNull(2)
                            val sessionId = segments.getOrNull(3)
                            DeepLinkTarget.Voice(serviceId = serviceId, sessionId = sessionId)
                        }
                        "search" -> DeepLinkTarget.Search()
                        else -> null
                    }
                }
                HOST_OPEN -> {
                    val serviceId = segments.getOrNull(1)
                    val queryUrl = extractQueryParam(queryPart, "url")
                    if (!serviceId.isNullOrBlank()) {
                        DeepLinkTarget.OpenService(
                            serviceId = serviceId,
                            targetUrl = queryUrl
                        )
                    } else if (!queryUrl.isNullOrBlank()) {
                        val host = extractHost(queryUrl)
                        val resolvedId = resolveServiceIdFromHost(host) ?: "chatgpt"
                        DeepLinkTarget.OpenService(
                            serviceId = resolvedId,
                            targetUrl = queryUrl
                        )
                    } else null
                }
                HOST_MIC -> DeepLinkTarget.Voice()
                HOST_SEARCH -> {
                    val query = extractQueryParam(queryPart, "q")
                    DeepLinkTarget.Search(query = query)
                }
                else -> null
            }
        }

        // 2. HTTP/HTTPS Web URL
        val match = URL_REGEX.find(trimmed)
        if (match != null) {
            val matchedUrl = match.value
            val host = extractHost(matchedUrl)
            val serviceId = resolveServiceIdFromHost(host)
            if (serviceId != null) {
                return DeepLinkTarget.OpenService(
                    serviceId = serviceId,
                    targetUrl = matchedUrl
                )
            }
        }

        return null
    }

    private fun extractQueryParam(queryPart: String?, paramName: String): String? {
        if (queryPart.isNullOrBlank()) return null
        return queryPart.split("&")
            .firstOrNull { it.startsWith("$paramName=") || it.startsWith("${paramName.lowercase()}=") }
            ?.substringAfter("=")
            ?.let {
                try {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } catch (e: Exception) {
                    it
                }
            }
    }

    /**
     * Resolves an incoming Android [Intent] into a [DeepLinkTarget].
     */
    fun resolveIntent(intent: Intent?): DeepLinkTarget? {
        if (intent == null) return null

        val action: String? = intent.action

        // 1. Check explicit custom action broadcasts
        if (action == ACTION_WIDGET_MIC) {
            val serviceId = intent.getStringExtra(EXTRA_SERVICE_ID)
            val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
            return DeepLinkTarget.Voice(serviceId, sessionId)
        }
        if (action == ACTION_WIDGET_SEARCH) {
            return DeepLinkTarget.Search()
        }
        if (action == ACTION_WIDGET_CLICK) {
            val serviceId = intent.getStringExtra(EXTRA_SERVICE_ID)
            val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
            val extraAction = intent.getStringExtra(EXTRA_ACTION)
            if (!serviceId.isNullOrBlank()) {
                return DeepLinkTarget.OpenService(
                    serviceId = serviceId,
                    sessionId = sessionId,
                    initialAction = extraAction
                )
            }
        }

        // 2. Check text sharing (Intent.ACTION_SEND)
        if (action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val target = resolveUrl(sharedText)
                if (target != null) return target
                return DeepLinkTarget.Search(query = sharedText.trim())
            }
        }

        // 3. Check Intent URI Data via dataString
        val dataString: String? = intent.dataString
        if (!dataString.isNullOrBlank()) {
            val target = resolveUrl(dataString)
            if (target != null) return target
        }

        // 4. Check Uri object fallback
        val data: Uri? = intent.data
        if (data != null) {
            val scheme = data.scheme?.lowercase()
            if (scheme == "https" || scheme == "http") {
                val host = data.host
                val serviceId = resolveServiceIdFromHost(host)
                if (serviceId != null) {
                    return DeepLinkTarget.OpenService(
                        serviceId = serviceId,
                        targetUrl = data.toString()
                    )
                }
            }
        }

        // 5. Check extra fallback
        val extraServiceId = intent.getStringExtra(EXTRA_SERVICE_ID)
        val extraSessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val extraAction = intent.getStringExtra(EXTRA_ACTION)
        if (!extraServiceId.isNullOrBlank()) {
            return when (extraAction) {
                "mic" -> DeepLinkTarget.Voice(extraServiceId, extraSessionId)
                "search" -> DeepLinkTarget.Search()
                else -> DeepLinkTarget.OpenService(
                    extraServiceId,
                    sessionId = extraSessionId,
                    initialAction = extraAction
                )
            }
        }

        return null
    }
}
