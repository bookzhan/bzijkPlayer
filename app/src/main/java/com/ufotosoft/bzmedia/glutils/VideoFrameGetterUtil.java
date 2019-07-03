package com.ufotosoft.bzmedia.glutils;

import android.graphics.Bitmap;

/**
 * Created by zhandalin on 2019-02-21 15:51.
 * 说明:
 */
public class VideoFrameGetterUtil {
    public static native long init(String videoPath, boolean userSoftDecode);

    public static native int getVideoWidth(long nativeHandle);

    public static native int getVideoHeight(long nativeHandle);

    public static native int getVideoFrame(long nativeHandle, long currentTime);

    public static native Bitmap getVideoFrame4Bitmap(long nativeHandle, long currentTime);

    public static native int release(long nativeHandle);

}
