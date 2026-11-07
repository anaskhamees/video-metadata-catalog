package com.videocatalog.app.service

import android.app.Service
import android.content.Intent
import  android.os.IBinder

// this is bound-only service
class CatalogService: Service() {

    private lateinit var binder: CatalogBinder

    override fun onCreate() {
        super.onCreate()
        binder= CatalogBinder(VideoFolder(applicationContext))
    }
    //called once, the system caches this IBinder and gives it to every client
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

}