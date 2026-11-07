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

    // Resolve the destination directory when it is first accessed.
    // Lazy: filesDir touches the disk, and the constructor runs on the main thread (onCreate).
    // First access happens in listFiles(), on a binder thread.
    private val dir by lazy {
        File(appContext.filesDir, VIDEOS_DIR)
    }

    // Copy missing sample videos only once per VideoFolder instance.
    // lazy is synchronized by default, so concurrent
    // binder calls wait for the copy to finish instead of copying twice.
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

        // Ensure the destination directory exists.
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

            // Avoid overwriting files that are already present.
            if (target.exists()) continue

            // Copy to a temporary file before creating the final file.
            val tmp = File(dir, name + TMP_SUFFIX)

            try {
                // Open the asset and copy its contents into the temporary file.
                // use {} automatically closes both streams, even if an exception occurs.
                // input: InputStream used to read bytes from the source asset.
                // output: OutputStream used to write bytes to the destination temporary file.
                assets.open("$ASSET_DIR/$name").use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Rename the temporary file after the copy completes successfully.
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