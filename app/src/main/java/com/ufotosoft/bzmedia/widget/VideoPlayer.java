package com.ufotosoft.bzmedia.widget;

/**
 * Created by zhandalin on 2019-03-18 17:29.
 * 说明:
 */
public class VideoPlayer {
    /**
     * 底层初始化,做了打开视频,音频,并打开对应的解码器
     */
    public static native long videoPlayerInit(String videoPath, Object attachObj, boolean userSoftDecode);

    public static native int videoPlayerRelease(long nativeHandle);

    public static native void videoPlayerStartSeek(long nativeHandle);

    public static native void videoPlayerStopSeek(long nativeHandle);

    public static native void videoPlayerSeek(long nativeHandle, long videoTime, boolean forceRefresh);

    public static native long videoPlayerOnDrawFrame(long nativeHandle, int textureId);

    public static native void videoPlayerPause(long nativeHandle, boolean isPause);

    public static native void videoPlayerOnPause(long nativeHandle);

    public static native void videoPlayerSetVolume(long nativeHandle, float volume);

    public static native long videoPlayerGetCurrentAudioPts(long nativeHandle);

    public static native void videoPlayerSetPlayLoop(long nativeHandle, boolean isLoop);
}
