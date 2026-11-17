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
 * [CatalogRepository] backed by [CatalogService], which runs in the ":metadata" process.
 * Hides binding, the AIDL proxy, Binder exceptions and rebinding behind a small API.
 *
 * Threading: all fields are touched only on the main thread (ServiceConnection callbacks
 * and the public methods), so no synchronization is needed.
 */
class ServiceCatalogRepository(context: Context) : CatalogRepository {

    // Application context: holding an Activity context here would leak the Activity.
    private val appContext = context.applicationContext

    private val serviceIntent = Intent(appContext, CatalogService::class.java)

    // AIDL proxy; null while disconnected.
    private var catalog: IVideoCatalog? = null

    // True after bindService() until unbindService(). Not "connected" (that is catalog != null):
    // the docs require unbindService() even when bindService() returned false.
    private var needsUnbind = false

    private var listener: CatalogRepository.Listener? = null

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            catalog = IVideoCatalog.Stub.asInterface(service)
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

        override fun onNullBinding(name: ComponentName?) {
            listener?.onNullBinding()
        }
    }

    override val isConnected: Boolean
        get() = catalog != null

    override fun connect(listener: CatalogRepository.Listener) {
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
        listener = null
    }

    override suspend fun listVideos(): List<VideoMetadata> {
        // Read on the main thread before switching dispatchers: the field is main-thread only.
        val service = catalog ?: throw CatalogUnavailableException("not connected")

        return try {
            // Blocking Binder call: keep it off the main thread.
            withContext(Dispatchers.IO) { service.listVideos() }
        } catch (e: DeadObjectException) {
            // Must precede RemoteException, its superclass.
            throw ServiceDiedException()
        } catch (e: RemoteException) {
            throw CatalogUnavailableException(e.javaClass.simpleName)
        }
        // CancellationException is deliberately not caught, so a cancelled request
        // can never overwrite the result of a newer one.
    }

    private fun bind() {
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
