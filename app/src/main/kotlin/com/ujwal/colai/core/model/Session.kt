package com.ujwal.colai.core.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an isolated container session for an [AIService].
 *
 * Each session is an isolated browser profile backed by Mozilla Gecko's native
 * `contextId = session.id`.
 *
 * Properties:
 * - [id]: Immutable container UUID passed to `GeckoSessionSettings.Builder().contextId(id)`.
 * - [serviceId]: Foreign key referencing the parent [AIService].
 * - [accountName]: User-customizable label (e.g., "Personal", "Work", "Account 2").
 * - [isDefault]: When true, this session is automatically activated when launching the service.
 * - [lastAccessed]: Timestamp in epoch milliseconds for LRU tracking.
 * - [themeMode]: Optional per-account theme preference ("light", "dark", "system").
 * - [customColors]: Optional custom brand accent color (HEX string).
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = AIService::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["serviceId"]),
        Index(value = ["serviceId", "isDefault"])
    ]
)
data class Session(
    @PrimaryKey
    val id: String,
    val serviceId: String,
    val accountName: String,
    val isDefault: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis(),
    val cookieStorePath: String? = null,
    val notificationsEnabled: Boolean = true,
    val themeMode: String? = null,
    val customColors: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
