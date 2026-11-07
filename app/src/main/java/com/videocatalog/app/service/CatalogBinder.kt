package com.videocatalog.app.service
import android.os.Process
import android.util.Log
import com.videocatalog.app.ExtractStatus
import com.videocatalog.app.VideoMetadata
import com.videocatalog.app.IVideoCatalog

// This is IVideoCatalog (Binder interface) implementation, it is run on binder threads of metadata process
class CatalogBinder(private val folder: VideoFolder) : IVideoCatalog.Stub(){
    override fun listVideos(): List<VideoMetadata?> {
        Log.d(TAG, "listVideos() pid=${Process.myPid()} thread=${Thread.currentThread().name}")
        return folder.listFiles().map { file ->
            VideoMetadata().apply {
                fileName=file.name
                status= ExtractStatus.OK
            }
        }
    }

    private  companion object{
        const val TAG="CatalogBinder"
    }

}