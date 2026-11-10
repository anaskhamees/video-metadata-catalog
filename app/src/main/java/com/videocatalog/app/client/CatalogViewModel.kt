package com.videocatalog.app.client

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.videocatalog.app.IVideoCatalog
import com.videocatalog.app.service.CatalogService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the connection to [CatalogService], which runs in the separate ":metadata" process,
 * and exposes what the screen should show as one [StateFlow].
 *
 * Why a ViewModel: it survives configuration changes (rotation, day/night switch), so the
 * app neither rebinds nor reloads. It binds with the application context, so no Activity
 * can leak through the connection.
 */
class CatalogViewModel(app: Application) : AndroidViewModel(app) {

    //  only this class can change the state; the UI gets a read-only view.
    private val _state = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    // Proxy to the remote service. Read and written only on the main thread
    // (ServiceConnection callbacks and refresh()), so it needs no synchronization.
    private var catalog: IVideoCatalog? = null

    // The current load request, so that a new refresh() can cancel an older one.
    private var loadJob: Job? = null

    // True after bindService() until unbindService(). Not "connected" (that is catalog != null):
    // the docs require unbindService() even when bindService() returned false.
    private var needsUnbind = false

    // Service deaths since the last successful load. If a file makes the native code crash
    // every time, auto-reloading would crash the service forever; after MAX_AUTO_RELOADS
    // we stop and let the user retry with Refresh.
    private var deathsSinceLoad = 0

    // True when the user tapped Refresh while disconnected: load on the next connect
    // even if the auto-reload limit was reached.
    private var retryRequested = false

    // Explicit intent: names our own service class. The UI never calls its code directly.
    private val serviceIntent = Intent(app, CatalogService::class.java)

    // The system calls these callbacks on the main thread.
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // Wrap the raw IBinder in the AIDL proxy generated from IVideoCatalog.aidl.
            val proxy = IVideoCatalog.Stub.asInterface(service)
            catalog = proxy
            if (deathsSinceLoad < MAX_AUTO_RELOADS || retryRequested) {
                retryRequested = false
                load(proxy)
            } else {
                _state.value = CatalogUiState.Error(KEEPS_CRASHING)
            }
        }

        // The service process died (crash or killed). The binding stays active: the system
        // restarts the service and calls onServiceConnected() again, but after repeated
        // crashes it waits longer each time (up to 30 minutes), so Refresh rebinds instead.
        override fun onServiceDisconnected(name: ComponentName?) {
            catalog = null
            deathsSinceLoad++
            _state.value = CatalogUiState.Error(
                if (deathsSinceLoad < MAX_AUTO_RELOADS) "Service stopped, reconnecting…" else KEEPS_CRASHING
            )
        }

        // Unlike onServiceDisconnected(), Android will never reconnect this binding
        // (e.g. after the service's app was updated). Release it and create a new one.
        override fun onBindingDied(name: ComponentName?) {
            catalog = null
            unbind()
            bind()
        }

        // The service returned null from onBind(), so this binding can never be used.
        override fun onNullBinding(name: ComponentName?) {
            _state.value = CatalogUiState.Error("Service unavailable")
        }
    }

    init {
        bind()
    }

    /**
     * Refresh button. Connected: reload the list. Disconnected: Android may not restart the
     * service for a long time after repeated crashes, so drop the binding and bind again,
     * which starts the service right away.
     */
    fun refresh() {
        val service = catalog
        if (service != null) {
            load(service)
            return
        }
        loadJob?.cancel()
        retryRequested = true
        _state.value = CatalogUiState.Loading
        unbind()
        bind()
    }

    /** Asks the service for the current list. A newer call replaces an older one. */
    private fun load(service: IVideoCatalog) {
        loadJob?.cancel()                 // a stale reply must never overwrite a newer one
        _state.value = CatalogUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                // Blocking Binder call: run it on an IO thread, never on the main thread (ANR).
                val videos = withContext(Dispatchers.IO) { service.listVideos() }
                deathsSinceLoad = 0
                _state.value = CatalogUiState.Content(videos)
            } catch (e: DeadObjectException) {
                // The service died mid-call. onServiceDisconnected() reports it; don't overwrite
                // its message, whichever of the two runs first.
            } catch (e: RemoteException) {
                // CancellationException is deliberately NOT caught: it is how coroutines stop, and
                // catching it would let a cancelled request overwrite a newer one.
                _state.value = CatalogUiState.Error("Service unavailable: ${e.javaClass.simpleName}")
            }
        }
    }

    private fun bind() {
        val isBound = getApplication<Application>().bindService(
            serviceIntent, connection, Context.BIND_AUTO_CREATE
        )
        needsUnbind = true
        if (!isBound) _state.value = CatalogUiState.Error("Cannot bind to the catalog service")

    }
    private fun unbind() {
        if (needsUnbind) {
            getApplication<Application>().unbindService(connection)
            needsUnbind = false
        }
    }
    // Release the binding when the ViewModel is destroyed. unbind() is a no-op if already unbound.
    override fun onCleared() {
        super.onCleared()
        unbind()
    }

    private companion object {
        const val MAX_AUTO_RELOADS = 3
        const val KEEPS_CRASHING = "The service keeps crashing. Tap Refresh to retry."
    }
}
