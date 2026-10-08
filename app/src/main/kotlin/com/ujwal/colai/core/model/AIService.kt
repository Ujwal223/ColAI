package com.ujwal.colai.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an AI service configured in ColAI (e.g. ChatGPT, Claude, DeepSeek).
 *
 * Each service defines:
 * - Unique identifier and user-facing name
 * - Target web URL and associated icon/favicon references
 * - Optional custom User-Agent and HTTP header overrides
 * - Widget linking preferences
 */
@Entity(tableName = "ai_services")
data class AIService(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val faviconUrl: String,
    val iconPath: String? = null,
    val customUserAgent: String? = null,
    val customHeaders: Map<String, String>? = null,
    val widgetSessionId: String? = null,
    val notificationsEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
