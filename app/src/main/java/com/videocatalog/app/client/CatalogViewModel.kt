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
 * MVVM: the ViewModel turns data and events into screen state. It never touches Views,
 * Compose, Context, or Binder.
 *
 * Its two jobs:
 *  1. Expose what the screen shows as one [StateFlow] (single source of truth).
 *  2. Decide when to reload after the service crashes (the retry policy).
 *
 * DEPENDENCY INJECTION (constructor injection): the repository is passed in from outside
 * instead of being created here. The app passes ServiceCatalogRepository; tests pass a fake.
 * `private val` in the constructor = the parameter is also stored as a private property.
 *
 * It extends ViewModel (not AndroidViewModel) because it no longer needs an Application.
 */
class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {

    // BACKING PROPERTY pattern: the mutable version is private (underscore by convention),
    // the public one is read-only. Only this class can change the state; the UI can only read it.
    private val _state = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    // The current load request, so that a new load() can cancel an older one.
    private var loadJob: Job? = null

    // Service deaths since the last successful load. If a file makes the native code crash
    // every time, auto-reloading would crash the service forever; after MAX_AUTO_RELOADS
    // we stop and let the user retry with Refresh.
    private var deathsSinceLoad = 0

    // True when the user tapped Refresh while disconnected: load on the next connect
    // even if the auto-reload limit was reached.
    private var retryRequested = false

    // OBSERVER: this object receives the repository's connection events.
    // `object : CatalogRepository.Listener { ... }` = an anonymous object implementing the interface.
    // It is private, so no other class can call these methods on the ViewModel.
    //
    // ORDER MATTERS: it must be declared BEFORE `init`. Kotlin runs property initializers and
    // init blocks from top to bottom, and connect() may call onBindFailed() immediately; if this
    // property were declared after init, it would still be null at that moment.
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
            // `if` is an expression in Kotlin: it returns a value (no ternary operator needed).
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

    // Runs once, when the ViewModel is created (NOT again after rotation: the same instance is reused).
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
        loadJob?.cancel()                 // a stale reply must never overwrite a newer one
        _state.value = CatalogUiState.Loading

        // viewModelScope: a coroutine scope tied to this ViewModel. It is cancelled automatically
        // when the ViewModel is cleared, so no request outlives the screen.
        // launch { } starts a coroutine and returns its Job (a handle we can cancel).
        loadJob = viewModelScope.launch {
            try {
                // Suspends here (does not block the main thread) until the repository answers.
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

    // Called once, when the screen is gone for good (not on rotation). Release the connection.
    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    // COMPANION OBJECT: members shared by all instances (like `static` in Java).
    private companion object {
        const val MAX_AUTO_RELOADS = 3
        const val RECONNECTING = "Service stopped, reconnecting…"
        const val KEEPS_CRASHING = "The service keeps crashing. Tap Refresh to retry."
    }
}
