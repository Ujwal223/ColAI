package com.ujwal.colai.core.data

import android.content.Context
import android.util.Log
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.database.ServiceDao
import com.ujwal.colai.core.database.SessionDao
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import java.util.UUID

/**
 * Repository responsible for initial database seeding of the 6 canonical AI services:
 * - ChatGPT
 * - Claude
 * - DeepSeek
 * - Grok
 * - Gemini
 * - Perplexity
 *
 * Automatically provisions an isolated default session for each service on clean installation.
 */
class DefaultServicesRepository(
    private val serviceDao: ServiceDao,
    private val sessionDao: SessionDao,
    private val encryptedStorage: EncryptedStorage? = null
) {

    constructor(
        database: AppDatabase,
        encryptedStorage: EncryptedStorage? = null
    ) : this(
        serviceDao = database.serviceDao(),
        sessionDao = database.sessionDao(),
        encryptedStorage = encryptedStorage
    )

    companion object {
        private const val TAG = "DefaultServicesRepo"

        const val ID_CHATGPT = "chatgpt"
        const val ID_CLAUDE = "claude"
        const val ID_DEEPSEEK = "deepseek"
        const val ID_GROK = "grok"
        const val ID_GEMINI = "gemini"
        const val ID_PERPLEXITY = "perplexity"

        fun getDefaultIconResource(serviceId: String): Int? {
            return when (serviceId.lowercase()) {
                ID_CHATGPT -> com.ujwal.colai.R.drawable.ic_service_chatgpt
                ID_CLAUDE -> com.ujwal.colai.R.drawable.ic_service_claude
                ID_DEEPSEEK -> com.ujwal.colai.R.drawable.ic_service_deepseek
                ID_GROK -> com.ujwal.colai.R.drawable.ic_service_grok
                ID_GEMINI -> com.ujwal.colai.R.drawable.ic_service_gemini
                ID_PERPLEXITY -> com.ujwal.colai.R.drawable.ic_service_perplexity
                else -> null
            }
        }

        fun getDefaultIconUrl(serviceId: String): String? {
            return when (serviceId.lowercase()) {
                ID_CHATGPT -> "https://chatgpt.com/favicon.ico"
                ID_CLAUDE -> "https://claude.ai/favicon.ico"
                ID_DEEPSEEK -> "https://www.deepseek.com/favicon.ico"
                ID_GROK -> "https://grok.com/favicon.ico"
                ID_GEMINI -> "https://www.gstatic.com/lamda/images/favicon_v1_150160d13f3925574452.png"
                ID_PERPLEXITY -> "https://www.perplexity.ai/favicon.ico"
                else -> null
            }
        }

        /**
         * Returns the canonical static list of default AI services.
         */
        fun getDefaultServices(): List<AIService> {
            val now = System.currentTimeMillis()
            return listOf(
                AIService(
                    id = ID_CHATGPT,
                    name = "ChatGPT",
                    url = "https://chatgpt.com",
                    faviconUrl = "https://chatgpt.com/favicon.ico",
                    iconPath = null,
                    sortOrder = 0,
                    createdAt = now
                ),
                AIService(
                    id = ID_CLAUDE,
                    name = "Claude",
                    url = "https://claude.ai",
                    faviconUrl = "https://claude.ai/favicon.ico",
                    iconPath = null,
                    sortOrder = 1,
                    createdAt = now
                ),
                AIService(
                    id = ID_DEEPSEEK,
                    name = "DeepSeek",
                    url = "https://chat.deepseek.com",
                    faviconUrl = "https://www.deepseek.com/favicon.ico",
                    iconPath = null,
                    sortOrder = 2,
                    createdAt = now
                ),
                AIService(
                    id = ID_GROK,
                    name = "Grok",
                    url = "https://grok.com",
                    faviconUrl = "https://grok.com/favicon.ico",
                    iconPath = null,
                    sortOrder = 3,
                    createdAt = now
                ),
                AIService(
                    id = ID_GEMINI,
                    name = "Gemini",
                    url = "https://gemini.google.com",
                    faviconUrl = "https://www.gstatic.com/lamda/images/favicon_v1_150160d13f3925574452.png",
                    iconPath = null,
                    sortOrder = 4,
                    createdAt = now
                ),
                AIService(
                    id = ID_PERPLEXITY,
                    name = "Perplexity",
                    url = "https://www.perplexity.ai",
                    faviconUrl = "https://www.perplexity.ai/favicon.ico",
                    iconPath = null,
                    sortOrder = 5,
                    createdAt = now
                )
            )
        }

        fun create(context: Context): DefaultServicesRepository {
            val database = AppDatabase.getDatabase(context)
            val storage = EncryptedStorage.getInstance(context)
            return DefaultServicesRepository(database, storage)
        }
    }

    /**
     * Seeds default AI services and their initial default container sessions
     * if the database is currently empty.
     *
     * @return Number of services seeded (0 if already populated).
     */
    suspend fun seedIfEmpty(): Int {
        val currentCount = serviceDao.countServices()
        if (currentCount > 0) {
            Log.d(TAG, "Database already has $currentCount services, skipping seed.")
            return 0
        }

        Log.i(TAG, "Seeding database with 6 default AI services and initial container sessions...")

        val defaultServices = getDefaultServices()
        val servicesToInsert = mutableListOf<AIService>()
        val sessionsToInsert = mutableListOf<Session>()

        for (service in defaultServices) {
            val defaultSessionId = UUID.randomUUID().toString()
            val initialSession = Session(
                id = defaultSessionId,
                serviceId = service.id,
                accountName = "Personal",
                isDefault = true,
                lastAccessed = System.currentTimeMillis()
            )
            sessionsToInsert.add(initialSession)
            val isDefaultWidgetService = service.id == ID_CHATGPT
            servicesToInsert.add(service.copy(widgetSessionId = if (isDefaultWidgetService) defaultSessionId else null))
        }

        // Insert services first, then sessions (foreign key constraint)
        serviceDao.insertServices(servicesToInsert)
        sessionDao.insertSessions(sessionsToInsert)

        // Set default active service and widget target in preferences
        encryptedStorage?.let { storage ->
            if (storage.getActiveServiceId() == null) {
                storage.setActiveServiceId(ID_CHATGPT)
                val chatgptSession = sessionsToInsert.find { it.serviceId == ID_CHATGPT }
                storage.setActiveSessionId(chatgptSession?.id)
                storage.setWidgetTarget(ID_CHATGPT, chatgptSession?.id)
            }
        }

        Log.i(TAG, "Database successfully seeded with ${servicesToInsert.size} services and ${sessionsToInsert.size} isolated sessions.")
        return servicesToInsert.size
    }
}
