package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zhandalin on 2017-11-14 11:58.
 * 说明:把video 作为GIF 显示的View
 */

public class BZVideo4GifView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private boolean hasStopParseGif = false;
    private volatile long nativeHandle = 0;
    private int width = 0;
    private int height = 0;

    public BZVideo4GifView(Context context) {
        this(context, null);
    }

    public BZVideo4GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        if (nativeHandle != 0)
            setGifRenderViewPort(nativeHandle, 0, 0, width, height);
    }

    public void startParseVideo4Gif(final String videoPath, final float speed, final int fps) {
        post(new Runnable() {
            @Override
            public void run() {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        hasStopParseGif = false;
                        if (nativeHandle != 0) {
                            setStopFlag(nativeHandle);
                            releaseGifParser(nativeHandle);
                            nativeHandle = 0;
                        }
                        nativeHandle = initGifParser(videoPath, speed, fps);
                        setGifRenderViewPort(nativeHandle, 0, 0, width, height);
                    }
                });
                if (getRenderMode() != GLSurfaceView.RENDERMODE_CONTINUOUSLY)
                    setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            }
        });
    }

    public void stopParseGif() {
        setStopFlag(nativeHandle);
        queueEvent(new Runnable() {
            @Override
            public void run() {
                hasStopParseGif = true;
                releaseGifParser(nativeHandle);
                nativeHandle = 0;
            }
        });
        if (getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY)
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void adjustGifParseSpeed(final float speed) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                adjustGifParseSpeed(nativeHandle, speed);
            }
        });
    }

    @Override
    public void onPause() {
        stopParseGif();
        super.onPause();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!hasStopParseGif)
            drawGifFrame(nativeHandle);
    }

    private static native long initGifParser(String videoPath, float speed, int fps);

    private static native void setGifRenderViewPort(long nativeHandle, int x, int y, int width, int height);

    private static native void adjustGifParseSpeed(long nativeHandle, float speed);

    private static native void drawGifFrame(long nativeHandle);

    private static native void setStopFlag(long nativeHandle);

    private static native void releaseGifParser(long nativeHandle);

}
