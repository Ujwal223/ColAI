package com.ujwal.colai.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ujwal.colai.core.data.DefaultServicesRepository
import com.ujwal.colai.core.data.LogoCacheManager
import com.ujwal.colai.core.database.AppDatabase
import com.ujwal.colai.core.database.ServiceDao
import com.ujwal.colai.core.database.SessionDao
import com.ujwal.colai.core.model.AIService
import com.ujwal.colai.core.model.Session
import com.ujwal.colai.core.security.EncryptedStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI state for the Bento Grid Home Dashboard.
 */
data class HomeUiState(
    val services: List<AIService> = emptyList(),
    val filteredServices: List<AIService> = emptyList(),
    val sessionsMap: Map<String, List<Session>> = emptyMap(),
    val activeServiceId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isFirstLaunch: Boolean = false
)

/**
 * ViewModel managing the Bento Grid AI Service dashboard, search filtering,
 * and service launching.
 */
class HomeViewModel(
    application: Application,
    private val serviceDao: ServiceDao,
    private val sessionDao: SessionDao,
    private val encryptedStorage: EncryptedStorage,
    private val defaultServicesRepository: DefaultServicesRepository,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activeServiceId = MutableStateFlow(encryptedStorage.getActiveServiceId())
    val activeServiceId: StateFlow<String?> = _activeServiceId.asStateFlow()

    // Reactive streams from Room Database
    private val allServicesFlow = serviceDao.getAllServices()
    private val allSessionsFlow = sessionDao.getAllSessions()

    val uiState: StateFlow<HomeUiState> = combine(
        allServicesFlow,
        allSessionsFlow,
        _searchQuery,
        _isLoading,
        _activeServiceId
    ) { services, sessions, query, loading, activeId ->
        val sessionsGrouped = sessions.groupBy { it.serviceId }
        val filtered = if (query.isBlank()) {
            services
        } else {
            services.filter { service ->
                service.name.contains(query, ignoreCase = true) ||
                    service.url.contains(query, ignoreCase = true)
            }
        }
        val isFirstLaunch = !encryptedStorage.isOnboardingCompleted() && services.isEmpty()

        HomeUiState(
            services = services,
            filteredServices = filtered,
            sessionsMap = sessionsGrouped,
            activeServiceId = activeId,
            searchQuery = query,
            isLoading = loading,
            isFirstLaunch = isFirstLaunch
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(ioDispatcher) {
                // Seed default services if empty
                if (encryptedStorage.isOnboardingCompleted() || serviceDao.countServices() == 0) {
                    defaultServicesRepository.seedIfEmpty()
                }
                _activeServiceId.value = encryptedStorage.getActiveServiceId()
            }
            _isLoading.value = false
            preloadTopServices()
        }
    }

    private fun preloadTopServices() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val services = serviceDao.getAllServicesList()
                if (services.isEmpty()) return@launch

                val activeId = encryptedStorage.getActiveServiceId() ?: services.firstOrNull()?.id
                val topServices = services.sortedByDescending { it.id == activeId }.take(2)
                val pool = com.ujwal.colai.core.engine.GeckoSessionPool.getInstance()

                for (service in topServices) {
                    val defaultSession = sessionDao.getDefaultSessionForService(service.id)
                        ?: sessionDao.getSessionsForServiceList(service.id).firstOrNull()
                    if (defaultSession != null) {
                        val effectiveContextId = if (defaultSession.isDefault && encryptedStorage.isSharedPersonalCookiesEnabled()) {
                            "shared_personal_container"
                        } else {
                            defaultSession.id
                        }
                        if (!pool.hasSession(defaultSession.id)) {
                            withContext(Dispatchers.Main) {
                                pool.preloadSession(
                                    sessionId = defaultSession.id,
                                    contextId = effectiveContextId,
                                    targetUrl = service.url
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeViewModel", "Background preloading error", e)
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun selectService(service: AIService, session: Session? = null) {
        viewModelScope.launch(ioDispatcher) {
            encryptedStorage.setActiveServiceId(service.id)
            _activeServiceId.value = service.id

            val targetSession = session ?: sessionDao.getDefaultSessionForService(service.id)
                ?: sessionDao.getSessionsForServiceList(service.id).firstOrNull()

            targetSession?.let { s ->
                encryptedStorage.setActiveSessionId(s.id)
                sessionDao.updateLastAccessed(s.id, System.currentTimeMillis())
            }
        }
    }

    fun deleteService(service: AIService) {
        viewModelScope.launch(ioDispatcher) {
            serviceDao.deleteService(service)
            if (_activeServiceId.value == service.id) {
                val remaining = serviceDao.getAllServicesList()
                val next = remaining.firstOrNull()
                encryptedStorage.setActiveServiceId(next?.id)
                _activeServiceId.value = next?.id
            }
        }
    }

    fun refreshFavicon(service: AIService) {
        viewModelScope.launch(ioDispatcher) {
            val manager = LogoCacheManager(getApplication<Application>(), serviceDao)
            manager.forceRefreshLogo(service)
        }
    }

    fun refresh() {
        initializeData()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val storage = EncryptedStorage.getInstance(application)
            val repo = DefaultServicesRepository(db, storage)
            return HomeViewModel(
                application = application,
                serviceDao = db.serviceDao(),
                sessionDao = db.sessionDao(),
                encryptedStorage = storage,
                defaultServicesRepository = repo
            ) as T
        }
    }
}
