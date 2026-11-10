package com.videocatalog.app.client.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import com.videocatalog.app.IVideoCatalog
import com.videocatalog.app.VideoMetadata
import com.videocatalog.app.service.CatalogService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The real [CatalogRepository], backed by [CatalogService], which runs in the ":metadata" process.
 *
 * FACADE PATTERN: hides a complicated subsystem (bindService, ServiceConnection callbacks,
 * AIDL proxy, Binder exceptions, rebinding) behind four simple methods.
 *
 * Threading: all fields are read and written on the main thread only (ServiceConnection
 * callbacks and the public methods), so they need no synchronization (no locks, no @Volatile).
 *
 * `context: Context` has no val/var, so it is only a constructor parameter, not a stored
 * property. We keep only the application context below.
 */
class ServiceCatalogRepository(context: Context) : CatalogRepository {

    // The application context lives as long as the app. Holding an Activity context here
    // would leak the Activity (keep it in memory after it is destroyed).
    private val appContext = context.applicationContext

    // Explicit intent: names our own service class, so no other app can answer it.
    private val serviceIntent = Intent(appContext, CatalogService::class.java)

    // PROXY PATTERN: IVideoCatalog is a proxy object generated from IVideoCatalog.aidl.
    // Calling catalog.listVideos() looks like a local call, but the proxy packs the request
    // into a Parcel and sends it through Binder to the service process.
    // `?` = nullable: null while disconnected.
    private var catalog: IVideoCatalog? = null

    // True after bindService() until unbindService(). Not "connected" (that is catalog != null):
    // the docs require unbindService() even when bindService() returned false.
    private var needsUnbind = false

    // Who to notify. Set by connect(), cleared by disconnect().
    private var listener: CatalogRepository.Listener? = null

    // `object : ServiceConnection { ... }` is an OBJECT EXPRESSION: it creates one instance of an
    // anonymous class that implements ServiceConnection (like an anonymous class in Java).
    // The system calls these callbacks on the main thread.
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // Wrap the raw IBinder in the AIDL proxy generated from IVideoCatalog.aidl.
            catalog = IVideoCatalog.Stub.asInterface(service)
            // `?.` = safe call: does nothing if listener is null (already disconnected).
            listener?.onConnected()
        }

        // The service process died (crash or killed). The binding stays active and the system
        // restarts the service, then calls onServiceConnected() again.
        override fun onServiceDisconnected(name: ComponentName?) {
            catalog = null
            listener?.onDisconnected()
        }

        // Android will never reconnect this binding (e.g. after an app update).
        // Release it and create a new one; onServiceConnected() reports the result.
        // The ViewModel is not told: from its point of view nothing changed yet.
        override fun onBindingDied(name: ComponentName?) {
            catalog = null
            unbind()
            bind()
        }

        // The service returned null from onBind(), so this binding can never be used.
        override fun onNullBinding(name: ComponentName?) {
            listener?.onNullBinding()
        }
    }

    // A property with a CUSTOM GETTER: no stored field; the value is computed on every read.
    override val isConnected: Boolean
        get() = catalog != null

    override fun connect(listener: CatalogRepository.Listener) {
        // check(...) throws IllegalStateException if the condition is false.
        // Fails fast on a programming mistake instead of silently replacing the listener.
        check(this.listener == null) { "connect() must be called only once" }
        this.listener = listener
        bind()
    }

    override fun reconnect() {
        catalog = null
        unbind()
        bind()
    }

    override fun disconnect() {
        unbind()
        catalog = null
        listener = null   // after unbindService() no more callbacks come; this is a second guard
    }

    override suspend fun listVideos(): List<VideoMetadata> {
        // Read `catalog` here, on the caller's (main) thread, BEFORE switching threads:
        // the field is only safe to touch on the main thread.
        // `?:` = ELVIS OPERATOR: "if the left side is null, do the right side".
        val service = catalog ?: throw CatalogUnavailableException("not connected")

        // `try` is an expression in Kotlin: its value is returned.
        return try {
            // Blocking Binder call: run it on an IO thread, never on the main thread (ANR).
            // withContext switches threads, waits for the block, and comes back to the caller's
            // thread with the result.
            withContext(Dispatchers.IO) { service.listVideos() }
        } catch (e: DeadObjectException) {
            // EXCEPTION TRANSLATION (an ADAPTER between Binder errors and our own errors).
            // Must come BEFORE RemoteException: DeadObjectException is its child class, and the
            // first matching catch wins.
            throw ServiceDiedException()
        } catch (e: RemoteException) {
            throw CatalogUnavailableException(e.javaClass.simpleName)
        }
        // CancellationException is deliberately NOT caught: it is how coroutines stop. Catching
        // it would let a cancelled (old) request overwrite the result of a newer one.
    }

    private fun bind() {
        // BIND_AUTO_CREATE: start the service if it is not running yet.
        val isBound = appContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        needsUnbind = true
        if (!isBound) listener?.onBindFailed()
    }

    private fun unbind() {
        if (needsUnbind) {
            appContext.unbindService(connection)
            needsUnbind = false
        }
    }
}
