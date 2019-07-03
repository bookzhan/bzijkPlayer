package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.AttributeSet;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zhandalin on 2018-05-05 15:42.
 * 说明:
 */
public class BackAndForthVideoPlayer extends BZBaseGLSurfaceView {
    private int averageDuration = 33;
    private long frameStartTimeMs;
    private final static String TAG = "bz_BackAndForthVideoPlayer";

    public BackAndForthVideoPlayer(Context context) {
        this(context, null);
    }

    public BackAndForthVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init() {
        super.init();
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        videoPlayerOnDrawFrame();
        if (averageDuration <= 0)
            averageDuration = 30;
        limitFrameRate(1000 / averageDuration);
    }

    protected void limitFrameRate(int framesPerSecond) {
        try {
            long elapsedFrameTimeMs = SystemClock.elapsedRealtime() - frameStartTimeMs;
            long expectedFrameTimeMs = 1000 / framesPerSecond;
            long timeToSleepMs = expectedFrameTimeMs - elapsedFrameTimeMs;
            if (timeToSleepMs > 0) {
                SystemClock.sleep(timeToSleepMs);
            }
            frameStartTimeMs = SystemClock.elapsedRealtime();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }
    @Override
    protected boolean calcViewport() {
        if (super.calcViewport()) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    videoPlayerViewPort(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);
                }
            });
            requestRender();
        }
        return true;
    }

    /**
     * call from jni
     */
    protected void onVideoInfoAvailable(int videoWidth, int videoHeight, int videoRotate, long videoDuration, float videoFps) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        if (videoRotate == 90 || videoRotate == 270) {
            this.videoWidth = videoHeight;
            this.videoHeight = videoWidth;
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                calcViewport();
            }
        });
    }

    public void setDataSource(String videoPath, int repeatCount, float speed) {
        averageDuration = videoPlayerInit(videoPath, repeatCount, speed);
        BZLogUtil.d(TAG, "setDataSource averageDuration=" + averageDuration);
    }

    public void start() {
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        videoPlayerPause(false);
    }

    public void pause() {
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        videoPlayerPause(true);
    }

    @Override
    public void onPause() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                videoPlayerOnActivityPause();
            }
        });
        super.onPause();
    }

    private native int videoPlayerInit(String videoPath, int repeatCount, float speed);

    public native int videoPlayerOnDrawFrame();

    public native void videoPlayerViewPort(int x, int y, int width, int height);

    public native void videoPlayerSetPlayLoop(boolean isLoop);

    private native void videoPlayerOnActivityPause();

    private native void videoPlayerPause(boolean isPause);

    public native int videoPlayerRelease();

}
