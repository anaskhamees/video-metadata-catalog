// ExtractStatus.aidl
package com.videocatalog.app;

/**
 * Result of reading one file's metadata. Sent to the UI inside VideoMetadata.
 *
 * @Backing(type="int") sets the storage type of the enum: each value is a 32-bit int.
 * The generated Java uses int constants and writes them with Parcel.writeInt(), so it works
 * on any Android version. It also matches the C++ side (enum Status : int32_t) and
 * NativeVideoInfo.status (Int). Values must stay in sync with metadata_extractor.h.
 */
@Backing(type="int")
enum ExtractStatus {
    OK = 0,              // metadata read from the first video track
    NO_VIDEO_TRACK = 1,  // valid media file, but it has no video track (audio-only .m4a)
    UNREADABLE = 2,      // not media, unsupported, or could not be opened.
}
