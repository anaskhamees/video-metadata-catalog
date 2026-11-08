package com.videocatalog.app.service

import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import com.videocatalog.app.ExtractStatus
import com.videocatalog.app.VideoMetadata
import com.videocatalog.app.IVideoCatalog
import java.io.File
import java.io.IOException

// Implements the AIDL Binder interface.
// Runs in the metadata service process on Binder threads.
class CatalogBinder(private val folder: VideoFolder) : IVideoCatalog.Stub() {

    // Called by the UI process through the Binder interface.
    override fun listVideos(): List<VideoMetadata?> {

        Log.d(TAG, "listVideos() pid=${Process.myPid()} thread=${Thread.currentThread().name}")

        // Get all video files and convert each file into VideoMetadata.
        // map() calls readVideoMetadata() for every file.
        return folder.listFiles().map(::readVideoMetadata)
    }

    // Reads one video file and extracts its metadata.
    private fun readVideoMetadata(file: File): VideoMetadata = try {

        // Open the video file in read-only mode.
        // use{} automatically closes the file descriptor when finished.
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { parcelFileDescriptor ->

            // Call the native C++ extractor through JNI.
            // Pass the file descriptor and file size.
            // Convert the returned native information into VideoMetadata.
            NativeExtractor.extractNativeMetadata(
                parcelFileDescriptor.fd,
                parcelFileDescriptor.statSize
            ).toVideoMetadata(file.name)

        }

    } catch (e: IOException) {

        Log.w(TAG, "Cannot open ${file.name}", e)

        // Create metadata for this file and mark it as unreadable.
        VideoMetadata().apply {
            fileName = file.name
            status = ExtractStatus.UNREADABLE
        }
    }

    // Extension function: converts NativeVideoInfo into VideoMetadata.
    private fun NativeVideoInfo.toVideoMetadata(name: String) = VideoMetadata().also {

        // Copy the extracted information into the AIDL VideoMetadata object.
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