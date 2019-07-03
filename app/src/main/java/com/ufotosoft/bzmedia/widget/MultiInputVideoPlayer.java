package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.AttributeSet;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.glutils.BZOpenGlUtils;
import com.ufotosoft.bzmedia.glutils.BaseProgram;
import com.ufotosoft.bzmedia.utils.BZLogUtil;
import com.ufotosoft.bzmedia.utils.BZMultilInputVideoPathUtils;

import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * Created by zhandalin on 2018-04-08 16:23.
 * 说明:多个输入源的视频播放器
 */
public class MultiInputVideoPlayer extends BZBaseGLSurfaceView {

    private long frameStartTimeMs;
    private AudioTrack audioTrack;
    protected boolean isRelease;
    private static final String TAG = "bz_MultiPlayer";
    private int videoRotate = 0;
    private long videoDuration = 0;
    protected long nativeHandle = 0;
    private OnCompletionListener onCompletionListener = null;
    private OnProgressChangedListener onProgressChangedListener = null;
    private BaseProgram baseProgram;
    private float volume = 1;
    private int displayRotate = 0;
    private int srcVideoWidth = 0;
    private int srcVideoHeight = 0;
    private float lastSeekProgress = 0;
    private boolean isPause = true;
    private volatile boolean userSoftDecode = true;

    public MultiInputVideoPlayer(Context context) {
        this(context, null);
    }

    public MultiInputVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        userSoftDecode = !BZOpenGlUtils.detectOpenGLES30();
    }

    @Override
    protected void init() {
        super.init();
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        if (null != onDrawFrameListener) {
            onDrawFrameListener.onDrawFrame(0);
        } else {
            long[] drawFrameInfo = onDrawFrame(nativeHandle);
            if (null == baseProgram) {
                baseProgram = new BaseProgram(false);
            }
            if (null != drawFrameInfo && drawFrameInfo.length > 1 && null != mDrawViewPort) {
                glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
                glViewport(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);
                baseProgram.draw((int) drawFrameInfo[1]);
            }
        }
        limitFrameRate(33);
    }

    /**
     * call from jni
     */
    protected void onPCMDataAvailable(byte[] pcmData, int length) {
//        BZLogUtil.d(TAG, "onPCMDataAvailable length=" + length);
        synchronized (this) {
            if (null == audioTrack && !isRelease) {
                try {
                    int audioBufSize = AudioTrack.getMinBufferSize(44100,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);

                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            audioBufSize,
                            AudioTrack.MODE_STREAM);
                    audioTrack.setStereoVolume(volume, volume);
                    audioTrack.play();
                } catch (Exception e) {
                    BZLogUtil.e(TAG, e);
                }
            }
            if (null != audioTrack) {
                audioTrack.write(pcmData, 0, length);
            }
        }
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

    public void releaseResource() {
        isRelease = true;
        if (nativeHandle != 0) {
            release(nativeHandle);
            nativeHandle = 0;
        }
        stopAudioTrack();
    }

    public void startSeek() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                videoPlayerStartSeek(nativeHandle);
            }
        });
    }

    public void stopSeek() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                videoPlayerStopSeek(nativeHandle);
                seek(nativeHandle, lastSeekProgress, true);
                if (isPause()) {
                    requestRender();
                }
            }
        });
    }

    /**
     * call from jni
     */
    protected void onVideoInfoAvailable(int videoWidth, int videoHeight, int videoRotate, long videoDuration) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoRotate = videoRotate;
        this.videoDuration = videoDuration;
        if (videoRotate == 90 || videoRotate == 270) {
            this.videoWidth = videoHeight;
            this.videoHeight = videoWidth;
        }

        srcVideoWidth = this.videoWidth;
        srcVideoHeight = this.videoHeight;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                calcViewport();
            }
        });
        BZLogUtil.d(TAG, "onVideoInfoAvailable videoWidth=" + videoWidth + "--videoHeight=" + videoHeight + "--videoRotate=" + videoRotate);
    }


    private void stopAudioTrack() {
        synchronized (this) {
            if (null != audioTrack) {
                try {
                    audioTrack.flush();
                    audioTrack.pause();
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Throwable e) {
                    BZLogUtil.e(TAG, e);
                }
            }
            audioTrack = null;
        }
    }

    @Override
    public void onPause() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != baseProgram) {
                    baseProgram.release();
                    baseProgram = null;
                }
                videoPlayerOnPause(nativeHandle);
            }
        });
        super.onPause();
    }

    public void start() {
        if (getRenderMode() != RENDERMODE_CONTINUOUSLY) {
            setRenderMode(RENDERMODE_CONTINUOUSLY);
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                isPause = false;
                videoPlayerPause(nativeHandle, false);
            }
        });
    }

    public void pause() {
        if (getRenderMode() != RENDERMODE_WHEN_DIRTY) {
            setRenderMode(RENDERMODE_WHEN_DIRTY);
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                isPause = true;
                videoPlayerPause(nativeHandle, true);
            }
        });
    }

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    public void setOnProgressChangedListener(OnProgressChangedListener onProgressChangedListener) {
        this.onProgressChangedListener = onProgressChangedListener;
    }

    /**
     * call from jni
     */
    protected void onVideoPlayCompletion(int isPlayCompletion) {
        if (null != onCompletionListener)
            onCompletionListener.onCompletion(isPlayCompletion > 0);
    }

    /**
     * call from jni
     */
    protected void onProgressChanged(float progress) {
        if (null != onProgressChangedListener)
            onProgressChangedListener.onProgressChanged(progress);
    }

    public void setPlayLoop(boolean playLoop) {
        if (nativeHandle == 0) return;
        setPlayLoop(nativeHandle, playLoop);
    }

    public long videoPlayerGetCurrentAudioPts() {
        return videoPlayerGetCurrentAudioPts(nativeHandle);
    }

    private native void setPlayLoop(long nativeHandle, boolean playLoop);

    private native long videoPlayerGetCurrentAudioPts(long nativeHandle);

    private native int videoPlayerPause(long nativeHandle, boolean isPause);

    public native int videoPlayerOnPause(long nativeHandle);

    public void setUserSoftDecode(boolean userSoftDecode) {
        this.userSoftDecode = userSoftDecode;
    }

    public long setDataSources(String[] videoPaths, BZMedia.MultiInputVideoLayoutType type) {
        isRelease = false;
        videoPaths = BZMultilInputVideoPathUtils.checkVideoPath(videoPaths, type);
        if (nativeHandle != 0) {
            release(nativeHandle);
            nativeHandle = 0;
        }
        nativeHandle = setDataSources(videoPaths, type.ordinal(), userSoftDecode);
        return nativeHandle;
    }

    public void seek(final float progress) {
        if (nativeHandle == 0) return;
        lastSeekProgress = progress;
        pause();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (nativeHandle == 0) return;
                seek(nativeHandle, progress, false);
                if (isPause()) {
                    requestRender();
                }
            }
        });
    }

    public boolean isPause() {
        return isPause;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        if (null != audioTrack) {
            try {
                audioTrack.setStereoVolume(volume, volume);
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
        }
    }

    public void setDisplayRotate(final int displayRotate) {
        this.displayRotate = displayRotate;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != baseProgram)
                    baseProgram.setRotation(displayRotate);
                if (displayRotate == 90 || displayRotate == 270) {
                    videoWidth = srcVideoHeight;
                    videoHeight = srcVideoWidth;
                } else {
                    videoWidth = srcVideoWidth;
                    videoHeight = srcVideoHeight;
                }
                calcViewport();
            }
        });
    }

    public long[] drawVideoFrame() {
        return onDrawFrame(nativeHandle);
    }

    @Override
    public int getVideoWidth() {
        return srcVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return srcVideoHeight;
    }

    private native void videoPlayerStartSeek(long nativeHandle);

    private native void videoPlayerStopSeek(long nativeHandle);

    private native long setDataSources(String[] videoPaths, int type, boolean userSoftDecode);

    private native long[] onDrawFrame(long nativeHandle);

    private native int release(long nativeHandle);

    private native int seek(long nativeHandle, float progress, boolean forceRefresh);


    public interface OnCompletionListener {
        void onCompletion(boolean isCompletion);
    }

    public interface OnProgressChangedListener {
        void onProgressChanged(float progress);
    }
}
