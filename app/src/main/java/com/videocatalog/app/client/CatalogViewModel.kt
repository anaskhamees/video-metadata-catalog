package com.videocatalog.app.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videocatalog.app.client.data.CatalogRepository
import com.videocatalog.app.client.data.CatalogUnavailableException
import com.videocatalog.app.client.data.ServiceDiedException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Exposes the catalog screen state and owns the reload policy after service crashes.
 * Has no Android framework dependencies beyond ViewModel, so it is unit-tested on the JVM.
 */
class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val _state = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    // Service deaths since the last successful load. If a file makes the native code crash
    // every time, auto-reloading would crash the service forever; after MAX_AUTO_RELOADS
    // we stop and let the user retry with Refresh.
    private var deathsSinceLoad = 0

    // True when the user tapped Refresh while disconnected: load on the next connect
    // even if the auto-reload limit was reached.
    private var retryRequested = false

    // Must be declared before `init`: connect() may call onBindFailed() synchronously,
    // and a property declared after init would still be null at that point.
    private val connectionListener = object : CatalogRepository.Listener {

        override fun onConnected() {
            if (deathsSinceLoad < MAX_AUTO_RELOADS || retryRequested) {
                retryRequested = false
                load()
            } else {
                _state.value = CatalogUiState.Error(KEEPS_CRASHING)
            }
        }

        override fun onDisconnected() {
            deathsSinceLoad++
            _state.value = CatalogUiState.Error(
                if (deathsSinceLoad < MAX_AUTO_RELOADS) RECONNECTING else KEEPS_CRASHING
            )
        }

        override fun onBindFailed() {
            _state.value = CatalogUiState.Error("Cannot bind to the catalog service")
        }

        override fun onNullBinding() {
            _state.value = CatalogUiState.Error("Service unavailable")
        }
    }

    init {
        repository.connect(connectionListener)
    }

    /**
     * Refresh button. Connected: reload the list. Disconnected: Android may not restart the
     * service for a long time after repeated crashes, so reconnect, which starts it right away.
     */
    fun refresh() {
        if (repository.isConnected) {
            load()
            return
        }
        loadJob?.cancel()
        retryRequested = true
        _state.value = CatalogUiState.Loading
        repository.reconnect()
    }

    /** Asks for the current list. A newer call replaces an older one. */
    private fun load() {
        loadJob?.cancel() // a stale reply must never overwrite a newer one
        _state.value = CatalogUiState.Loading

        loadJob = viewModelScope.launch {
            try {
                val videos = repository.listVideos()
                deathsSinceLoad = 0
                _state.value = CatalogUiState.Content(videos)
            } catch (e: ServiceDiedException) {
                // onDisconnected() reports it; don't overwrite its message.
            } catch (e: CatalogUnavailableException) {
                _state.value = CatalogUiState.Error("Service unavailable: ${e.reason}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    private companion object {
        const val MAX_AUTO_RELOADS = 3
        const val RECONNECTING = "Service stopped, reconnecting…"
        const val KEEPS_CRASHING = "The service keeps crashing. Tap Refresh to retry."
    }
}
