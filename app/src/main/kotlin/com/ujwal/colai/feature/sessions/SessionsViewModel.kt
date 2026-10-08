package com.ujwal.colai.feature.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.database.SessionDao
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel managing multi-account container sessions for a specific AI service.
 * Supports creating, renaming, setting default, and deleting isolated container profiles.
 */
class SessionsViewModel(
    application: Application,
    private val sessionDao: SessionDao,
    private val encryptedStorage: EncryptedStorage,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _serviceId = MutableStateFlow<String?>(null)
    val serviceId: StateFlow<String?> = _serviceId.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(encryptedStorage.getActiveSessionId())
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun loadSessionsForService(targetServiceId: String) {
        _serviceId.value = targetServiceId
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val list = sessionDao.getSessionsForServiceList(targetServiceId)
                _sessions.value = list

                val currentActive = encryptedStorage.getActiveSessionId()
                if (currentActive == null || list.none { it.id == currentActive }) {
                    val defaultSession = list.find { it.isDefault } ?: list.firstOrNull()
                    defaultSession?.let { s ->
                        _activeSessionId.value = s.id
                        encryptedStorage.setActiveSessionId(s.id)
                    }
                } else {
                    _activeSessionId.value = currentActive
                }
            }
        }
    }

    fun createNewSession(
        serviceId: String,
        name: String,
        onCreated: ((Session) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmedName = name.trim().ifBlank { "Account ${_sessions.value.size + 1}" }
        val isDuplicate = _sessions.value.any { it.accountName.equals(trimmedName, ignoreCase = true) }
        if (isDuplicate) {
            onError?.invoke("A session named '$trimmedName' already exists for this service.")
            return
        }

        val newSessionId = UUID.randomUUID().toString()
        val newSession = Session(
            id = newSessionId,
            serviceId = serviceId,
            accountName = trimmedName,
            isDefault = _sessions.value.isEmpty(),
            lastAccessed = System.currentTimeMillis()
        )

        viewModelScope.launch(ioDispatcher) {
            sessionDao.insertSession(newSession)
            val updated = sessionDao.getSessionsForServiceList(serviceId)
            _sessions.value = updated
            _activeSessionId.value = newSessionId
            encryptedStorage.setActiveSessionId(newSessionId)
            onCreated?.invoke(newSession)
        }
    }

    fun renameSession(
        session: Session,
        newName: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == session.accountName) return

        val isDuplicate = _sessions.value.any {
            it.id != session.id && it.accountName.equals(trimmed, ignoreCase = true)
        }
        if (isDuplicate) {
            onError?.invoke("A session named '$trimmed' already exists for this service.")
            return
        }

        viewModelScope.launch(ioDispatcher) {
            val updated = session.copy(accountName = trimmed)
            sessionDao.updateSession(updated)
            session.serviceId.let { sid ->
                _sessions.value = sessionDao.getSessionsForServiceList(sid)
            }
            onSuccess?.invoke()
        }
    }

    fun setDefaultSession(serviceId: String, sessionId: String) {
        viewModelScope.launch(ioDispatcher) {
            sessionDao.setDefaultSession(serviceId, sessionId)
            _sessions.value = sessionDao.getSessionsForServiceList(serviceId)
        }
    }

    fun switchActiveSession(serviceId: String, sessionId: String) {
        viewModelScope.launch(ioDispatcher) {
            encryptedStorage.setActiveSessionId(sessionId)
            encryptedStorage.setActiveServiceId(serviceId)
            sessionDao.updateLastAccessed(sessionId, System.currentTimeMillis())
            _activeSessionId.value = sessionId
        }
    }

    fun deleteSession(serviceId: String, sessionId: String) {
        viewModelScope.launch(ioDispatcher) {
            sessionDao.deleteSessionById(sessionId)
            val remaining = sessionDao.getSessionsForServiceList(serviceId)

            if (remaining.isEmpty()) {
                // Keep at least one default container active
                val fallbackSession = Session(
                    id = UUID.randomUUID().toString(),
                    serviceId = serviceId,
                    accountName = "Personal",
                    isDefault = true,
                    lastAccessed = System.currentTimeMillis()
                )
                sessionDao.insertSession(fallbackSession)
                _sessions.value = listOf(fallbackSession)
                _activeSessionId.value = fallbackSession.id
                encryptedStorage.setActiveSessionId(fallbackSession.id)
            } else {
                _sessions.value = remaining
                if (_activeSessionId.value == sessionId) {
                    val nextDefault = remaining.find { it.isDefault } ?: remaining.first()
                    _activeSessionId.value = nextDefault.id
                    encryptedStorage.setActiveSessionId(nextDefault.id)
                }
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val storage = EncryptedStorage.getInstance(application)
            return SessionsViewModel(
                application = application,
                sessionDao = db.sessionDao(),
                encryptedStorage = storage
            ) as T
        }
    }
}
