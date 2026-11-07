/**
 * Represents metadata extracted from a video file.
 *
 * This Parcelable data structure is used to transfer video metadata
 * between the Video Catalog service and its clients through Binder IPC.
 */
package com.videocatalog.app;

import com.videocatalog.app.ExtractStatus;

/**
 * Contains the metadata information associated with a video file.
 */
parcelable VideoMetadata {
    String fileName = "";
    ExtractStatus status = ExtractStatus.UNREADABLE;   // until set, so a missed status never reads as OK
    long durationUs = -1;                              // microseconds; -1 = unknown
    int width = 0;                                     // pixels; 0 = unknown
    int height = 0;
    @nullable String videoMime;                        // e.g. "video/avc"; null when there is no video track
    boolean hasAudio = false;
}