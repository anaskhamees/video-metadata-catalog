
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum required CMake version.
cmake_minimum_required(VERSION 3.22.1)

# Defines the project name and enables the C++ compiler.
project(video_metadata LANGUAGES CXX)

# Builds a shared library (libvideo_metadata.so).
# video_metadata: Target/library name.
# SHARED: Creates a dynamically linked library (.so).
add_library(video_metadata SHARED
        jni_bridge.cpp
        metadata_extractor.cpp)

# Requires the C++20 standard for this target.
# PRIVATE: Applies only to this library; not propagated to dependent targets.
# cxx_std_20: C++20 standard requirement.
target_compile_features(video_metadata PRIVATE cxx_std_20)

# Adds compiler options for this target.
# PRIVATE: Applies only to video_metadata.
# -Wall: Enables common compiler warnings.
# -Wextra: Enables additional compiler warnings.
# -fvisibility=hidden: Hides symbols by default unless explicitly exported.
# only JNI_OnLoad is exported, via JNIEXPORT (natives are bound with RegisterNatives).
target_compile_options(video_metadata PRIVATE -Wall -Wextra -fvisibility=hidden)

target_link_libraries(video_metadata
        mediandk    # AMediaExtractor, AMediaFormat
        log)        # __android_log_print
