package com.videocatalog.app.service

import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import com.videocatalog.app.ExtractStatus
import com.videocatalog.app.VideoMetadata
import com.videocatalog.app.IVideoCatalog
import java.io.File
import java.io.IOException

/** AIDL implementation. Runs in the ":metadata" process on binder threads. */
class CatalogBinder(private val folder: VideoFolder) : IVideoCatalog.Stub() {

    override fun listVideos(): List<VideoMetadata?> {

        Log.d(TAG, "listVideos() pid=${Process.myPid()} thread=${Thread.currentThread().name}")

        return folder.listFiles().map(::readVideoMetadata)
    }

    private fun readVideoMetadata(file: File): VideoMetadata = try {

        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { parcelFileDescriptor ->

            NativeExtractor.extractNativeMetadata(
                parcelFileDescriptor.fd,
                parcelFileDescriptor.statSize
            ).toVideoMetadata(file.name)

        }

    } catch (e: IOException) {

        Log.w(TAG, "Cannot open ${file.name}", e)

        VideoMetadata().apply {
            fileName = file.name
            status = ExtractStatus.UNREADABLE
        }
    }

    private fun NativeVideoInfo.toVideoMetadata(name: String) = VideoMetadata().also {

        it.fileName = name
        it.status = status
        it.durationUs = durationUs
        it.width = width
        it.height = height
        it.videoMime = videoMime
        it.hasAudio = hasAudio
    }

    private companion object {
        const val TAG = "CatalogBinder"
    }
}