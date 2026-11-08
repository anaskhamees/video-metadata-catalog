#ifndef VIDEO_CATALOG_METADATA_EXTRACTOR_H
#define VIDEO_CATALOG_METADATA_EXTRACTOR_H

#include <cstdint>
#include <string>

namespace videometa {

// Must match the values in ExtractStatus.aidl.
enum Status : int32_t {
    kOk = 0,
    kNoVideoTrack = 1,
    kUnreadable = 2,
};

struct MetadataResult {
    Status status = kUnreadable;
    int64_t durationUs = -1;   // -1 = unknown
    int32_t width = 0;         // 0 = unknown
    int32_t height = 0;
    std::string videoMime;     // empty when there is no video track
    bool hasAudio = false;
};

// Reads the first video track's metadata from an open file descriptor.
// Borrows fd: never closes it (the caller owns it).
// Keeps no shared state (no globals or statics), so it is safe to call from several threads at once.
MetadataResult extractMetadata(int fd, int64_t length);

}  // namespace videometa

#endif  // VIDEO_CATALOG_METADATA_EXTRACTOR_H
