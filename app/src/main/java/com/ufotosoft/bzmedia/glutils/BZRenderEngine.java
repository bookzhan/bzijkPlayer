package com.ufotosoft.bzmedia.glutils;

import com.ufotosoft.bzmedia.bean.TextureHandleInfo;

/**
 * Created by zhandalin on 2019-02-20 09:58.
 * 说明:
 */
public class BZRenderEngine {
    public static native long init();

    public static native void setFlip(long nativeHandel, boolean flipHorizontal, boolean flipVertical);

    public static native void setFinalSize(long nativeHandel, int finalWidth, int finalHeight);

    public static native void setTextureHandleInfo(long nativeHandel, TextureHandleInfo textureHandleInfo);

    public static native void setRotation(long nativeHandel, int rotation);

    public static native int draw(long nativeHandel, int textureId, int textureWidth, int textureHeight);

    public static native int release(long nativeHandel);
}
