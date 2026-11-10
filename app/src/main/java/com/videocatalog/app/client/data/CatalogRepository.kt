package com.videocatalog.app.client.data

import com.videocatalog.app.VideoMetadata

/**
 * REPOSITORY PATTERN: the single entry point the UI layer uses to get catalog data.
 *
 * The ViewModel only knows this interface. It does not know that the data comes from a
 * bound Service, through Binder and AIDL, in another process. That detail lives in
 * [ServiceCatalogRepository], and could be replaced (cache, database, fake for tests)
 * without changing a single line of the ViewModel.
 *
 * DEPENDENCY INVERSION PRINCIPLE: the high-level class (ViewModel) depends on an
 * abstraction (this interface), not on the low-level class (ServiceCatalogRepository).
 *
 * Threading rule: call every member on the main thread.
 */
interface CatalogRepository {

    /**
     * True while a live connection to the catalog exists.
     * An interface can declare a property: each implementation decides how to provide it
     * (a stored field, or a custom getter that computes it).
     */
    val isConnected: Boolean

    /**
     * Starts connecting. Call once.
     * Connection events are delivered to [listener] on the main thread.
     */
    fun connect(listener: Listener)

    /** Drops the current connection and starts a new one, reusing the same listener. */
    fun reconnect()

    /** Releases the connection. No more events are delivered after this. */
    fun disconnect()

    /**
     * Returns the current list of videos.
     *
     * `suspend` = this function can pause without blocking the thread. The caller (a coroutine)
     * waits for the result while the main thread stays free to draw the UI.
     * "Main-safe": the implementation moves the blocking work to a background thread itself,
     * so callers can call it from the main thread.
     *
     * @throws ServiceDiedException the service died during the call; [Listener.onDisconnected] reports it.
     * @throws CatalogUnavailableException any other failure.
     */
    suspend fun listVideos(): List<VideoMetadata>

    /**
     * OBSERVER PATTERN: the repository (subject) notifies the ViewModel (observer)
     * when the connection changes.
     *
     * Why a listener and not a StateFlow? These are EVENTS, not STATE. A StateFlow keeps only
     * the latest value, so "disconnected" followed quickly by "connected" could reach the
     * ViewModel as just "connected", and a crash would never be counted. A listener delivers
     * every event, in order.
     *
     * A nested interface: its full name is CatalogRepository.Listener.
     */
    interface Listener {
        /**
         * Called when the connection to the remote service is successfully established.
         * Android has provided a valid IBinder, which allows the client to communicate
         * with the service (for example, call listVideos()).
         * Also called again each time Android reconnects after the service process died.
         */
        fun onConnected()
        /**
         * Called when an already-established service connection is unexpectedly lost.
         * Example:
         * The service process crashes or is killed by the system.
         * The existing Binder connection is no longer usable.
         * The client should consider the service disconnected.
         * The binding stays active: Android restarts the service and onConnected() follows.
         */
        fun onDisconnected()
        /**
         * Called when Android cannot establish the binding connection.
         * Example:
         * bindService() returns false because the system could not create the binding.
         * No successful connection was established.
         */
        fun onBindFailed()
        /**
         * Called when the service explicitly returns a null binding from onBind().
         * The service exists and Android was able to communicate with it,
         * but the service returned null instead of a usable IBinder.
         * Therefore, the client cannot communicate with the service.
         */
        fun onNullBinding()
    }
}

/**
 * The service process died during a call.
 *
 * The repository translates Android's Binder exceptions (DeadObjectException, RemoteException)
 * into these exceptions of our own. The ViewModel then needs no android.* imports, which is
 * what lets it run in plain JVM unit tests.
 */
class ServiceDiedException : Exception()

/**
 * A call failed for a reason other than the service dying.
 * `val reason` in the constructor = a read-only property, created and assigned in one line.
 */
class CatalogUnavailableException(val reason: String) : Exception(reason)
