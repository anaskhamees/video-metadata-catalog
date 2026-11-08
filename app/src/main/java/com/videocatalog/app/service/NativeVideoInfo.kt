package com.videocatalog.app.service

import androidx.annotation.Keep

/**
 * Result of one native extraction. Built from C++ in jni_bridge.cpp with the constructor
 * signature (IJIILjava/lang/String;Z)V: keep the name, package and parameter order in sync.
 * @Keep stops R8 from renaming or removing it in a minified build.
 */
@Keep
class NativeVideoInfo(
    val status: Int,
    val durationUs: Long,
    val width: Int,
    val height: Int,
    val videoMime: String?,
    val hasAudio: Boolean,
)