#include "metadata_extractor.h"

#include <android/log.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaFormat.h>

#include <cstdint>
#include <memory>
#include <string>

namespace videometa {
namespace {

constexpr const char* kTag = "metadata_native_extractor";

// AMediaExtractor and AMediaFormat are NDK objects: they must be freed with their own
// *_delete() functions, not with C++ delete. These deleters let std::unique_ptr do that
// automatically on every return path. (unique_ptr never calls a deleter with nullptr.)
struct ExtractorDeleter {
    void operator()(AMediaExtractor* p) const {
        AMediaExtractor_delete(p);
    }
};
struct FormatDeleter {
    void operator()(AMediaFormat* p) const {
        AMediaFormat_delete(p);
    }
};

using ExtractorPtr = std::unique_ptr<AMediaExtractor, ExtractorDeleter>;
using FormatPtr = std::unique_ptr<AMediaFormat, FormatDeleter>;

}  // namespace

// See metadata_extractor.h for the contract.
MetadataResult extractMetadata(int fd, int64_t length) {
    // Header defaults: status = kUnreadable, duration -1, size 0.
    // So every early "return result;" below means "unreadable".
    MetadataResult result{};

    ExtractorPtr extractor(AMediaExtractor_new());
    if (!extractor) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "Failed to create AMediaExtractor");
        return result;
    }

    // Attach the file. The extractor dup()s fd and parses the container headers.
    // We only borrow fd: Kotlin owns it and closes it after this function returns.
    const media_status_t sourceStatus = AMediaExtractor_setDataSourceFd(extractor.get(), fd, 0, length);
    if (sourceStatus != AMEDIA_OK) {
        // Expected for non-media files, so WARN rather than ERROR.
        __android_log_print(ANDROID_LOG_WARN, kTag, "setDataSourceFd failed: %d",
                            static_cast<int>(sourceStatus));
        return result;
    }

    // Tracks may be video, audio, subtitles or other streams.
    const size_t trackCount = AMediaExtractor_getTrackCount(extractor.get());
    if (trackCount == 0) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "Media file contains no tracks");
        return result;
    }

    bool foundVideo = false;
    for (size_t i = 0; i < trackCount; ++i) {
        // A new format object for track i, freed at the end of each iteration.
        FormatPtr format(AMediaExtractor_getTrackFormat(extractor.get(), i));
        if (!format) {
            __android_log_print(ANDROID_LOG_WARN, kTag, "Failed to get format for track %zu", i);
            continue;
        }

        // e.g. "video/avc" (H.264), "video/hevc" (H.265), "audio/mp4a-latm" (AAC).
        const char* rawMime = nullptr;
        if (!AMediaFormat_getString(format.get(), AMEDIAFORMAT_KEY_MIME, &rawMime) || rawMime == nullptr) {
            __android_log_print(ANDROID_LOG_WARN, kTag, "Missing MIME type for track %zu", i);
            continue;
        }
        // rawMime points inside `format`; copy it before `format` is freed.
        const std::string mime(rawMime);

        // Use the first video track only: a file can contain several,
        // and the UI shows one row per file.
        if (!foundVideo && mime.starts_with("video/")) {
            foundVideo = true;
            result.videoMime = mime;

            // Each getter returns false if the key is missing; then the "unknown" default stays.
            int32_t width = 0;
            int32_t height = 0;
            int64_t durationUs = 0;

            if (AMediaFormat_getInt32(format.get(), AMEDIAFORMAT_KEY_WIDTH, &width)) {
                result.width = width;
            } else {
                __android_log_print(ANDROID_LOG_WARN, kTag, "Width unavailable for track %zu", i);
            }

            if (AMediaFormat_getInt32(format.get(), AMEDIAFORMAT_KEY_HEIGHT, &height)) {
                result.height = height;
            } else {
                __android_log_print(ANDROID_LOG_WARN, kTag, "Height unavailable for track %zu", i);
            }

            if (AMediaFormat_getInt64(format.get(), AMEDIAFORMAT_KEY_DURATION, &durationUs)) {
                result.durationUs = durationUs;  // microseconds
            } else {
                __android_log_print(ANDROID_LOG_WARN, kTag, "Duration unavailable for track %zu", i);
            }
        } else if (mime.starts_with("audio/")) {
            // Any audio track is enough; we only report yes/no.
            result.hasAudio = true;
        }
        // Other track types (subtitles, metadata) are ignored.
    }

    // Parsed fine: OK with a video track, otherwise "No video track" (e.g. audio_only.m4a).
    result.status = foundVideo ? kOk : kNoVideoTrack;
    return result;
}

}  // namespace videometa
