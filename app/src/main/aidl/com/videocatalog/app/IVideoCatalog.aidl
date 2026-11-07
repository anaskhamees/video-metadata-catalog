// IVideoCatalog.aidl
package com.videocatalog.app;

import com.videocatalog.app.VideoMetadata;

//Implemented by the service in the metadata process.
interface IVideoCatalog {
    /**
     * Blocking: call it off the main thread.
     * Returns one entry per regular file in the service-owned folder, sorted by name.
     * A bad file becomes an entry with an error status; it never fails the whole call.
     */
    List<VideoMetadata> listVideos();
}