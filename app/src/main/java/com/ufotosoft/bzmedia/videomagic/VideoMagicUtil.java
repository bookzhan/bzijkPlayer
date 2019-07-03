package com.ufotosoft.bzmedia.videomagic;

/**
 * Created by zhandalin on 2018-04-04 16:36.
 * 说明:
 */
public class VideoMagicUtil {

    public static long initEffect(VideoMagicType videoMagicType, int width, int height) {
        return initEffect(videoMagicType.ordinal(), width, height, 30);
    }

    public static long initEffect(VideoMagicType videoMagicType, int width, int height, float fps) {
        return initEffect(videoMagicType.ordinal(), width, height, fps);
    }

    private native static long initEffect(int videoMagicType, int width, int height, float fps);

    public native static void drawTexture(long handler, int srcTexture, int disTexture);

    public native static void release(long handler);

    public enum VideoMagicType {
        GHOST, DITHER, ILLUSION, BLACK, SEVENTY, XSIG
    }
}
