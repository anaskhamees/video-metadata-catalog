package com.videocatalog.app.service
import android.os.Process
import android.util.Log
import com.videocatalog.app.ExtractStatus
import com.videocatalog.app.VideoMetadata
import com.videocatalog.app.IVideoCatalog

// This is IVideoCatalog (Binder interface) implementation, it is run on binder threads of metadata process
class CatalogBinder : IVideoCatalog.Stub(){
    override fun listVideos(): List<VideoMetadata?> {
        Log.d(TAG, "listVideos() pid=${Process.myPid()} thread=${Thread.currentThread().name}")
        return listOf(
            VideoMetadata().apply {
                fileName="stub.mp4"
                status= ExtractStatus.OK
                durationUs=5_000_000
                width=1280
                height=720
                videoMime="video/avc"
                hasAudio=true
            }
        )
    }

    private  companion object{
        const val TAG="CatalogBinder"
    }

}