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
 *
 * Words used below:
 *   JNIEnv*   A table of JNI functions (FindClass, NewObject, ...). One per thread:
 *             never store it and reuse it on another thread.
 *   jobject   A handle to a Kotlin/Java object in the ART heap. C++ can't read it
 *             directly; it only passes it to JNIEnv functions. jclass and jstring
 *             are special kinds of jobject.
 *   jint, jlong, jboolean
 *             Plain values (int32_t, int64_t, uint8_t), copied, not handles.
 *   Local reference
 *             The default kind of jobject handle. Valid only until the current
 *             native call returns.
 *   Global reference
 *             A handle that stays valid across calls, made with NewGlobalRef().
 */

#include <jni.h>

#include <iterator>  // std::size

#include "metadata_extractor.h"

/**
 * Anonymous namespace: everything in it is private to this file. Only JNI_OnLoad
 * (at the bottom) is visible to Android. CMake also builds with -fvisibility=hidden.
 */
namespace {

/**
 * Names that must match the Kotlin code exactly.
 *
 * JNI writes class names with '/' instead of '.' ("JVM internal name").
 * If the Kotlin package or class is renamed, update these strings.
 */
constexpr const char* kExtractorClass = "com/videocatalog/app/service/NativeExtractor";
constexpr const char* kInfoClass = "com/videocatalog/app/service/NativeVideoInfo";

/**
 * The NativeVideoInfo constructor, written as a JNI type signature:
 *
 *   Kotlin:  NativeVideoInfo(status: Int, durationUs: Long, width: Int, height: Int,
 *                            videoMime: String?, hasAudio: Boolean)
 *   JNI:     ( I  J  I  I  Ljava/lang/String;  Z ) V
 *
 *   I = Int    J = Long    Z = Boolean    Lpackage/Class; = an object type
 *   Inside ( ) are the parameters, in order, with NO separators.
 *   After ( ) is the return type; V (void) for every constructor.
 *
 * If the Kotlin constructor's parameters or their order change, this must change too.
 * A mismatch is caught at load time: GetMethodID() in JNI_OnLoad fails.
 */
constexpr const char* kInfoCtorSig = "(IJIILjava/lang/String;Z)V";

/**
 * Cached in JNI_OnLoad, then only read.
 *
 * Looking up a class and a constructor is slow, so it is done once at load time
 * instead of on every call.
 *
 * Thread safety: JNI_OnLoad writes these once, before any Kotlin code can call
 * nativeExtract(). After that they are only read, so binder threads can share them.
 */

/**
 * GLOBAL reference to the NativeVideoInfo class. A local reference from FindClass()
 * would become invalid when JNI_OnLoad returns. Never deleted: Android never unloads
 * an app's native library, so this lives as long as the process (intended).
 */
jclass gInfoClass = nullptr;

/**
 * ID of the NativeVideoInfo constructor. A jmethodID is a plain ID, not a reference:
 * it never needs DeleteLocalRef/DeleteGlobalRef, and stays valid while the class is loaded.
 */
jmethodID gInfoCtor = nullptr;

/**
 * The native implementation of Kotlin's NativeExtractor.extractNativeMetadata().
 * Called on a binder thread of the :metadata process, through CatalogBinder.
 *
 * Parameters JNI always passes first:
 *   env     the JNI function table for THIS thread
 *   thiz    the Kotlin object the method was called on. NativeExtractor is a Kotlin
 *           `object`, so this is its singleton instance. Unused, like an unused `this`.
 * Then the Kotlin parameters:
 *   fd      the open file descriptor (Kotlin Int → jint). Borrowed: Kotlin closes it.
 *   length  the file size in bytes (Kotlin Long → jlong).
 *
 * Returns a new NativeVideoInfo, or nullptr with a pending Java exception
 * (Kotlin then sees the exception, not the null).
 */
jobject nativeExtract(JNIEnv* env, jobject /* thiz */, jint fd, jlong length) {
    /*
     * 1. Run the pure C++ extractor. A C++ exception must NEVER cross into the JVM
     *    (that is undefined behaviour, usually a crash). extractMetadata() only
     *    throws if memory runs out (std::string allocation), so on any exception we
     *    fall back to the default result, which means "unreadable".
     */
    videometa::MetadataResult result;
    try {
        result = videometa::extractMetadata(fd, length);
    } catch (...) {
        result = videometa::MetadataResult{};
    }

    /*
     * 2. Convert the C++ std::string MIME type into a Kotlin String.
     *    Empty MIME (no video track) → Kotlin null, which fits `videoMime: String?`.
     */
    jstring mime = nullptr;
    if (!result.videoMime.empty()) {
        // NewStringUTF copies the characters into a new Java String and returns a
        // LOCAL reference. MIME types are plain ASCII, which NewStringUTF accepts.
        mime = env->NewStringUTF(result.videoMime.c_str());
        if (mime == nullptr) {
            // Out of memory: an OutOfMemoryError is already pending in the JVM.
            // Return right away; Kotlin receives that exception.
            return nullptr;
        }
    }

    /*
     * 3. Create the Kotlin object by calling its constructor.
     *    The values after gInfoCtor must follow kInfoCtorSig exactly, in order:
     *    I status, J durationUs, I width, I height, String videoMime, Z hasAudio.
     *    NewObject cannot check types, so a wrong type or order here is NOT a
     *    compile error, just wrong values or a crash. Each value is cast explicitly.
     */
    jobject info = env->NewObject(gInfoClass, gInfoCtor,
                                  static_cast<jint>(result.status),
                                  static_cast<jlong>(result.durationUs),
                                  static_cast<jint>(result.width),
                                  static_cast<jint>(result.height),
                                  mime,
                                  result.hasAudio ? JNI_TRUE : JNI_FALSE);

    /*
     * 4. Release our local reference to the String. This doesn't destroy the String:
     *    the new NativeVideoInfo holds its own reference. Local references are freed
     *    anyway when we return, but releasing early is good JNI practice.
     *    (If NewObject failed, info is nullptr with a pending exception, and
     *    returning it passes that exception to Kotlin.)
     */
    if (mime != nullptr) {
        env->DeleteLocalRef(mime);
    }
    return info;
}

/**
 * Registration table: Kotlin method → C++ function.
 *
 * Each entry is { Kotlin method name, JNI signature, C++ function address }.
 * The signature "(IJ)L...NativeVideoInfo;" means: takes (Int, Long), returns
 * NativeVideoInfo. Again, no separators between parameter types.
 */
const JNINativeMethod kMethods[] = {
    {"extractNativeMetadata",
     "(IJ)Lcom/videocatalog/app/service/NativeVideoInfo;",
     reinterpret_cast<void*>(nativeExtract)},  // JNI stores every function as void*
};

}  // namespace

/**
 * JNI_OnLoad: runs once, when Kotlin calls System.loadLibrary("video_metadata").
 * It caches what nativeExtract() needs and registers the native method.
 *
 * Returning JNI_ERR makes System.loadLibrary() throw, so any name or signature
 * mistake shows up immediately when the service first uses NativeExtractor,
 * not later in the middle of a call.
 *
 *   extern "C"  keep the plain C name "JNI_OnLoad" so Android can find it
 *               (C++ would otherwise decorate the name).
 *   JNIEXPORT   make this one function visible despite -fvisibility=hidden.
 *   JNICALL     the calling convention JNI expects (empty on Android, kept for portability).
 */
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    /*
     * 1. Get the JNIEnv for the current thread. JavaVM is process-wide; JNIEnv is
     *    per-thread. The thread running loadLibrary() is already attached to the VM.
     */
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    /*
     * 2. Find NativeVideoInfo and keep a GLOBAL reference to it.
     *    FindClass works here because JNI_OnLoad runs with the app's class loader.
     *    (Called later on a binder thread, it could fail to find app classes;
     *    caching it now avoids that.)
     */
    jclass infoClass = env->FindClass(kInfoClass);
    if (infoClass == nullptr) {
        return JNI_ERR;  // class not found: wrong name, or removed by R8 (missing @Keep)
    }
    gInfoClass = static_cast<jclass>(env->NewGlobalRef(infoClass));
    env->DeleteLocalRef(infoClass);  // the local one is no longer needed
    if (gInfoClass == nullptr) {
        return JNI_ERR;
    }

    /* 3. Find the constructor. "<init>" is the JVM's internal name for constructors. */
    gInfoCtor = env->GetMethodID(gInfoClass, "<init>", kInfoCtorSig);
    if (gInfoCtor == nullptr) {
        return JNI_ERR;  // the Kotlin constructor doesn't match kInfoCtorSig
    }

    /* 4. Bind Kotlin's external fun to nativeExtract(). */
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

    /* 5. Success: tell Android which JNI version this library needs. */
    return JNI_VERSION_1_6;
}
