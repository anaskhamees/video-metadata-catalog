/**
 * jni_bridge.cpp
 * This file is implemented with AI assistance
 *
 * The JNI layer: the only file that knows about both Kotlin and C++.
 *
 *   Kotlin  NativeExtractor.extractNativeMetadata(fd, length)
 *     │  (JNI call, bound by RegisterNatives below)
 *     ▼
 *   C++     nativeExtract()  ──calls──▶  videometa::extractMetadata()  (metadata_extractor.cpp)
 *     │  converts the C++ MetadataResult into a Kotlin object
 *     ▼
 *   Kotlin  NativeVideoInfo
 */

#include <jni.h>

#include <iterator>  // std::size

#include "metadata_extractor.h"

namespace {

// Must match the Kotlin package and class names exactly.
constexpr const char* kExtractorClass = "com/videocatalog/app/service/NativeExtractor";
constexpr const char* kInfoClass = "com/videocatalog/app/service/NativeVideoInfo";

// NativeVideoInfo(status: Int, durationUs: Long, width: Int, height: Int,
//                 videoMime: String?, hasAudio: Boolean)
// Must be kept in sync with the Kotlin constructor; a mismatch fails JNI_OnLoad.
constexpr const char* kInfoCtorSig = "(IJIILjava/lang/String;Z)V";

// Written once in JNI_OnLoad before any call can happen, then read-only, so binder
// threads can share them. The global ref is never deleted: the library is never unloaded.
jclass gInfoClass = nullptr;
jmethodID gInfoCtor = nullptr;

/**
 * NativeExtractor.extractNativeMetadata(). Called on a binder thread of ":metadata".
 * [fd] is borrowed; Kotlin closes it. Returns a NativeVideoInfo, or nullptr with a
 * pending Java exception.
 */
jobject nativeExtract(JNIEnv* env, jobject /* thiz */, jint fd, jlong length) {
    // A C++ exception must never cross into the JVM. extractMetadata() only throws on
    // allocation failure; fall back to the default ("unreadable") result.
    videometa::MetadataResult result;
    try {
        result = videometa::extractMetadata(fd, length);
    } catch (...) {
        result = videometa::MetadataResult{};
    }

    // Empty MIME (no video track) maps to Kotlin null.
    jstring mime = nullptr;
    if (!result.videoMime.empty()) {
        mime = env->NewStringUTF(result.videoMime.c_str());
        if (mime == nullptr) {
            // OutOfMemoryError is already pending.
            return nullptr;
        }
    }

    // NewObject is varargs and unchecked: arguments must match kInfoCtorSig exactly.
    jobject info = env->NewObject(gInfoClass, gInfoCtor,
                                  static_cast<jint>(result.status),
                                  static_cast<jlong>(result.durationUs),
                                  static_cast<jint>(result.width),
                                  static_cast<jint>(result.height),
                                  mime,
                                  result.hasAudio ? JNI_TRUE : JNI_FALSE);

    if (mime != nullptr) {
        env->DeleteLocalRef(mime);
    }
    return info;
}

const JNINativeMethod kMethods[] = {
    {"extractNativeMetadata",
     "(IJ)Lcom/videocatalog/app/service/NativeVideoInfo;",
     reinterpret_cast<void*>(nativeExtract)},
};

}  // namespace

/**
 * Caches the class/constructor and registers the native method. Returning JNI_ERR makes
 * System.loadLibrary() throw, so name or signature mismatches fail fast at load time.
 */
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // FindClass must run here: on a binder thread it would use the system class loader
    // and fail to find app classes.
    jclass infoClass = env->FindClass(kInfoClass);
    if (infoClass == nullptr) {
        return JNI_ERR;  // class not found: wrong name, or removed by R8 (missing @Keep)
    }
    gInfoClass = static_cast<jclass>(env->NewGlobalRef(infoClass));
    env->DeleteLocalRef(infoClass);
    if (gInfoClass == nullptr) {
        return JNI_ERR;
    }

    gInfoCtor = env->GetMethodID(gInfoClass, "<init>", kInfoCtorSig);
    if (gInfoCtor == nullptr) {
        return JNI_ERR;  // the Kotlin constructor doesn't match kInfoCtorSig
    }

    jclass extractorClass = env->FindClass(kExtractorClass);
    if (extractorClass == nullptr) {
        return JNI_ERR;
    }
    const jint rc = env->RegisterNatives(extractorClass, kMethods,
                                         static_cast<jint>(std::size(kMethods)));
    env->DeleteLocalRef(extractorClass);
    if (rc != JNI_OK) {
        return JNI_ERR;  // method name or signature doesn't match the Kotlin declaration
    }

    return JNI_VERSION_1_6;
}
