package com.ufotosoft.bzmedia.glutils;

/**
 * Created by zhandalin on 2018-12-18 10:58.
 * 说明:
 */
public class ExternalTextureProgram {
    public static long initNative(boolean flipVertical) {
        return initNative(flipVertical, false);
    }

    /**
     * @param needFadeShow 是否需要渐显
     */
    public native static long initNative(boolean flipVertical, boolean needFadeShow);

    public native static int initGlResource(long nativeHandle);

    public native static int setVideoRotation(long nativeHandle, int rotation);

    public native static int onDrawFrame(long nativeHandle);

    public native static int setFlip(long nativeHandle, boolean needFlipHorizontal, boolean needFlipVertical);

    public native static int releaseGlResource(long nativeHandle);
}
