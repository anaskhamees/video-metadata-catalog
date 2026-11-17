package com.videocatalog.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Bound-only service hosting the catalog in the ":metadata" process. */
class CatalogService : Service() {

    private lateinit var binder: CatalogBinder

    override fun onCreate() {
        super.onCreate()
        binder = CatalogBinder(VideoFolder(applicationContext))
    }

    // Called once; the system caches this IBinder and hands it to every client.
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

}