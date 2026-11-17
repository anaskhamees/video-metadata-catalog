package com.videocatalog.app.client.data

import com.videocatalog.app.VideoMetadata

/**
 * Source of catalog data for the UI layer.
 *
 * The ViewModel depends only on this interface, so it does not know the data comes from a
 * bound service in another process. Tests substitute a fake implementation.
 *
 * All members must be called on the main thread.
 */
interface CatalogRepository {

    /** True while a live connection to the catalog exists. */
    val isConnected: Boolean

    /** Starts connecting. Call once. Events are delivered to [listener] on the main thread. */
    fun connect(listener: Listener)

    /** Drops the current connection and starts a new one, reusing the same listener. */
    fun reconnect()

    /** Releases the connection. No more events are delivered after this. */
    fun disconnect()

    /**
     * Returns the current list of videos. Main-safe: blocking work runs on a background thread.
     *
     * @throws ServiceDiedException the service died during the call; [Listener.onDisconnected] reports it.
     * @throws CatalogUnavailableException any other failure.
     */
    suspend fun listVideos(): List<VideoMetadata>

    /**
     * Connection events. A listener is used instead of a StateFlow because these are events,
     * not state: a StateFlow could conflate "disconnected" + "connected" into just "connected",
     * and the crash would never be counted.
     */
    interface Listener {
        /** Connected, initially or again after the service process was restarted. */
        fun onConnected()

        /** The service process died. The binding stays active; [onConnected] follows on restart. */
        fun onDisconnected()

        /** bindService() failed; no connection was established. */
        fun onBindFailed()

        /** The service returned null from onBind(); this binding can never be used. */
        fun onNullBinding()
    }
}

/**
 * The service process died during a call. Binder exceptions are translated into these
 * types so the ViewModel has no android.* dependencies and runs in plain JVM unit tests.
 */
class ServiceDiedException : Exception()

/** A call failed for a reason other than the service dying. */
class CatalogUnavailableException(val reason: String) : Exception(reason)
