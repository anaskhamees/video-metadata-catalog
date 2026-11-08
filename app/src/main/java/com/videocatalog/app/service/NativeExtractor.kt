package com.videocatalog.app.service

import androidx.annotation.Keep

/**
 * Kotlin side of the JNI bridge. The object is first used on a binder thread of
 * ":metadata", so the library is never loaded in the UI process.
 * JNI_OnLoad finds this class and registers extractNativeMetadata() by name, so @Keep stops R8
 * from renaming or removing them in a minified build.
 */
@Keep
object NativeExtractor {
    init {
        System.loadLibrary("video_metadata")
    }

    /** Reads track metadata from an open file descriptor. Borrows [fd]: never closes it. */
    external fun extractNativeMetadata(fd: Int, length: Long): NativeVideoInfo
}