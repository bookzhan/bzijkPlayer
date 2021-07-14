# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_definitions(-DNDEBUG)

set(common_sources
        src/main/cpp/ijkmedia/ijkj4a/j4a/j4a_allclasses.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/j4a_base.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/android/media/AudioTrack.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/android/media/MediaCodec.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/android/media/MediaFormat.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/android/media/PlaybackParams.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/android/os/Build.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/android/os/Bundle.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/java/nio/Buffer.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/java/nio/ByteBuffer.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/java/util/ArrayList.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/tv/danmaku/ijk/media/player/misc/IMediaDataSource.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/tv/danmaku/ijk/media/player/misc/IAndroidIO.c
        src/main/cpp/ijkmedia/ijkj4a/j4a/class/tv/danmaku/ijk/media/player/IjkMediaPlayer.c
        src/main/cpp/ijkmedia/ijkj4a/j4au/class/android/media/AudioTrack.util.c
        src/main/cpp/ijkmedia/ijkj4a/j4au/class/java/nio/ByteBuffer.util.c

        #ijkmedia/ijksdl/
        src/main/cpp/ijkmedia/ijksdl/ijksdl_aout.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_audio.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_egl.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_error.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_mutex.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_stdinc.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_thread.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_timer.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_vout.c
        src/main/cpp/ijkmedia/ijksdl/ijksdl_extra_log.c
        src/main/cpp/ijkmedia/ijksdl/gles2/color.c
        src/main/cpp/ijkmedia/ijksdl/gles2/common.c
        src/main/cpp/ijkmedia/ijksdl/gles2/renderer.c
        src/main/cpp/ijkmedia/ijksdl/gles2/renderer_rgb.c
        src/main/cpp/ijkmedia/ijksdl/gles2/renderer_yuv420p.c
        src/main/cpp/ijkmedia/ijksdl/gles2/renderer_yuv444p10le.c
        src/main/cpp/ijkmedia/ijksdl/gles2/shader.c
        src/main/cpp/ijkmedia/ijksdl/gles2/fsh/rgb.fsh.c
        src/main/cpp/ijkmedia/ijksdl/gles2/fsh/yuv420p.fsh.c
        src/main/cpp/ijkmedia/ijksdl/gles2/fsh/yuv444p10le.fsh.c
        src/main/cpp/ijkmedia/ijksdl/gles2/vsh/mvp.vsh.c
        src/main/cpp/ijkmedia/ijksdl/dummy/ijksdl_vout_dummy.c
        src/main/cpp/ijkmedia/ijksdl/ffmpeg/ijksdl_vout_overlay_ffmpeg.c
        src/main/cpp/ijkmedia/ijksdl/ffmpeg/abi_all/image_convert.c
        src/main/cpp/ijkmedia/ijksdl/android/android_audiotrack.c
        src/main/cpp/ijkmedia/ijksdl/android/android_nativewindow.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_android_jni.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_aout_android_audiotrack.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_aout_android_opensles.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_codec_android_mediacodec_dummy.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_codec_android_mediacodec_internal.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_codec_android_mediacodec_java.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_codec_android_mediacodec.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_codec_android_mediadef.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_codec_android_mediaformat_java.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_codec_android_mediaformat.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_vout_android_nativewindow.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_vout_android_surface.c
        src/main/cpp/ijkmedia/ijksdl/android/ijksdl_vout_overlay_android_mediacodec.c

        #ijkmedia/ijksoundtouch/
        src/main/cpp/ijkmedia/ijksoundtouch/soundtouch-jni.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/AAFilter.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/FIFOSampleBuffer.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/FIRFilter.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/cpu_detect_x86.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/sse_optimized.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundStretch/WavFile.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/RateTransposer.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/SoundTouch.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/InterpolateCubic.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/InterpolateLinear.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/InterpolateShannon.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/TDStretch.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/BPMDetect.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/PeakFinder.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/SoundTouch.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/source/SoundTouch/mmx_optimized.cpp
        src/main/cpp/ijkmedia/ijksoundtouch/ijksoundtouch_wrap.cpp

        #ijkmedia/ijkyuv
        src/main/cpp/ijkmedia/ijkyuv/source/compare.cc
        src/main/cpp/ijkmedia/ijkyuv/source/compare_common.cc
        src/main/cpp/ijkmedia/ijkyuv/source/compare_posix.cc
        src/main/cpp/ijkmedia/ijkyuv/source/convert.cc
        src/main/cpp/ijkmedia/ijkyuv/source/convert_argb.cc
        src/main/cpp/ijkmedia/ijkyuv/source/convert_from.cc
        src/main/cpp/ijkmedia/ijkyuv/source/convert_from_argb.cc
        src/main/cpp/ijkmedia/ijkyuv/source/convert_to_argb.cc
        src/main/cpp/ijkmedia/ijkyuv/source/convert_to_i420.cc
        src/main/cpp/ijkmedia/ijkyuv/source/cpu_id.cc
        src/main/cpp/ijkmedia/ijkyuv/source/format_conversion.cc
        src/main/cpp/ijkmedia/ijkyuv/source/planar_functions.cc
        src/main/cpp/ijkmedia/ijkyuv/source/rotate.cc
        src/main/cpp/ijkmedia/ijkyuv/source/rotate_argb.cc
        src/main/cpp/ijkmedia/ijkyuv/source/rotate_mips.cc
        src/main/cpp/ijkmedia/ijkyuv/source/row_any.cc
        src/main/cpp/ijkmedia/ijkyuv/source/row_common.cc
        src/main/cpp/ijkmedia/ijkyuv/source/row_mips.cc
        src/main/cpp/ijkmedia/ijkyuv/source/row_posix.cc
        src/main/cpp/ijkmedia/ijkyuv/source/scale.cc
        src/main/cpp/ijkmedia/ijkyuv/source/scale_argb.cc
        src/main/cpp/ijkmedia/ijkyuv/source/scale_common.cc
        src/main/cpp/ijkmedia/ijkyuv/source/scale_mips.cc
        src/main/cpp/ijkmedia/ijkyuv/source/scale_posix.cc
        src/main/cpp/ijkmedia/ijkyuv/source/video_common.cc

        #ijkplayer
        src/main/cpp/ijkmedia/ijkplayer/ff_cmdutils.c
        src/main/cpp/ijkmedia/ijkplayer/ff_ffplay.c
        src/main/cpp/ijkmedia/ijkplayer/ff_ffpipeline.c
        src/main/cpp/ijkmedia/ijkplayer/ff_ffpipenode.c
        src/main/cpp/ijkmedia/ijkplayer/ijkmeta.c
        src/main/cpp/ijkmedia/ijkplayer/ijkplayer.c
        src/main/cpp/ijkmedia/ijkplayer/pipeline/ffpipeline_ffplay.c
        src/main/cpp/ijkmedia/ijkplayer/pipeline/ffpipenode_ffplay_vdec.c
        src/main/cpp/ijkmedia/ijkplayer/android/ffmpeg_api_jni.c
        src/main/cpp/ijkmedia/ijkplayer/android/ijkplayer_android.c
        src/main/cpp/ijkmedia/ijkplayer/android/ijkplayer_jni.c
        src/main/cpp/ijkmedia/ijkplayer/android/pipeline/ffpipeline_android.c
        src/main/cpp/ijkmedia/ijkplayer/android/pipeline/ffpipenode_android_mediacodec_vdec.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/allformats.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijklivehook.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkmediadatasource.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkio.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkiomanager.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkiocache.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkioffio.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkioandroidio.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkioprotocol.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkioapplication.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkiourlhook.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkasync.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijkurlhook.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijklongurl.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavformat/ijksegment.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavutil/ijkdict.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavutil/ijkutils.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavutil/ijkthreadpool.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavutil/ijktree.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavutil/ijkfifo.c
        src/main/cpp/ijkmedia/ijkplayer/ijkavutil/ijkstl.cpp

        src/main/cpp/prof.c
        src/main/cpp/native-lib.cpp
        )

set(armeabi_v7a_sources
        src/main/cpp/ijkmedia/ijkyuv/source/compare_neon.cc
        src/main/cpp/ijkmedia/ijkyuv/source/rotate_neon.cc
        src/main/cpp/ijkmedia/ijkyuv/source/row_neon.cc
        src/main/cpp/ijkmedia/ijkyuv/source/scale_neon.cc
        )

set(arm64_v8a_sources
        src/main/cpp/ijkmedia/ijkyuv/source/compare_neon64.cc
        src/main/cpp/ijkmedia/ijkyuv/source/rotate_neon64.cc
        src/main/cpp/ijkmedia/ijkyuv/source/row_neon64.cc
        src/main/cpp/ijkmedia/ijkyuv/source/scale_neon64.cc
        )

if (${ANDROID_ABI} STREQUAL armeabi-v7a)
    add_library( # Sets the name of the library.
            ijkplayer
            SHARED
            ${common_sources}
            ${armeabi_v7a_sources}
            )
else ()
    add_library( # Sets the name of the library.
            ijkplayer
            SHARED
            ${common_sources}
            ${arm64_v8a_sources}
            )
endif ()


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
configure_file(${Project_SOURCE_DIR}/src/main/cpp/jniLibs/${ANDROID_ABI}/libijkffmpeg.so ${Project_BINARY_DIR}/libijkffmpeg.so COPYONLY)


#ffmpeg
add_library(ijkffmpeg-lib
        SHARED
        IMPORTED)
set_target_properties(ijkffmpeg-lib
        PROPERTIES IMPORTED_LOCATION
        libijkffmpeg.so)
#sdl
add_library(ijksdl-lib
        SHARED
        IMPORTED)
set_target_properties(ijksdl-lib
        PROPERTIES IMPORTED_LOCATION
        libijksdl.so)

find_library(log-lib log)
find_library(m-lib m)
find_library(z-lib z)
find_library(android-lib android)
find_library(EGL-lib EGL)
find_library(jnigraphics-lib jnigraphics)
find_library(GLES-lib GLESv2)
find_library(OpenSLES-lib OpenSLES)

include_directories(
        src/main/cpp
        src/main/cpp/ffmpeginclude
        src/main/cpp/ffmpeginclude/${ANDROID_ABI}
        src/main/cpp/ijkmedia
        src/main/cpp/ijkmedia/ijkplayer
        src/main/cpp/ijkmedia/ijkj4a
        src/main/cpp/ijkmedia/ijksdl
        src/main/cpp/ijkmedia/ijksoundtouch
        src/main/cpp/ijkmedia/ijksoundtouch/include
        src/main/cpp/ijkmedia/ijkyuv
        src/main/cpp/ijkmedia/ijkyuv/include
)


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        ijkplayer

        # Links the target library to the log library
        # included in the NDK.
        ijkffmpeg-lib ${log-lib} ${m-lib} ${z-lib} ${android-lib} ${EGL-lib} ${GLES-lib} ${jnigraphics-lib} ${OpenSLES-lib})