package com.videocatalog.app.service

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Manages the service's private video directory and initializes it
 * with the sample videos packaged in the application assets.
 */
class VideoFolder(context: Context) {

    private companion object {
        const val TAG = "VideoFolder"
        const val VIDEOS_DIR = "videos"
        const val ASSET_DIR = "samples"
        const val TMP_SUFFIX = ".tmp"
    }

    private val appContext = context.applicationContext

    // Lazy: filesDir touches the disk, and the constructor runs on the main thread.
    // First access happens in listFiles(), on a binder thread.
    private val dir by lazy {
        File(appContext.filesDir, VIDEOS_DIR)
    }

    // Synchronized lazy: concurrent binder calls wait for one copy instead of copying twice.
    private val seeding = lazy {
        copyMissingSamples()
    }

    /**
     * Returns the available video files, excluding temporary files.
     * Sample videos are copied before the directory is listed.
     */
    fun listFiles(): List<File> {
        seeding.value

        return dir.listFiles { file ->
            file.isFile && !file.name.endsWith(TMP_SUFFIX)
        }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    /**
     * Copies sample videos from assets into the private video directory.
     * Existing files are preserved, and incomplete copies are not exposed
     * under their final filenames.
     */
    private fun copyMissingSamples() {

        if (!dir.isDirectory && !dir.mkdirs()) {
            Log.e(TAG, "Cannot create $dir")
            return
        }

        val assets = appContext.assets

        val names = try {
            assets.list(ASSET_DIR).orEmpty()
        } catch (e: IOException) {
            Log.e(TAG, "Cannot list assets/$ASSET_DIR", e)
            return
        }
        for (name in names) {

            val target = File(dir, name)

            if (target.exists()) continue

            // Copy to a temp file and rename, so a partial copy never appears under the real name.
            val tmp = File(dir, name + TMP_SUFFIX)

            try {
                assets.open("$ASSET_DIR/$name").use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!tmp.renameTo(target)) {
                    throw IOException("rename failed for $name")
                }

            } catch (e: IOException) {
                Log.e(TAG, "Could not copy sample $name", e)
                tmp.delete()
            }
        }
    }
}