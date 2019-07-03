package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Surface;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.glutils.BZOpenGlUtils;
import com.ufotosoft.bzmedia.glutils.ExternalTextureProgram;
import com.ufotosoft.bzmedia.utils.BZDeviceUtils;
import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * Created by zhandalin on 2017-09-29 09:47.
 * 说明:
 */
public class BZVideoView extends BZBaseGLSurfaceView {
    private static final String TAG = "bz_BZVideoView";
    private int videoRotate;
    private String videoPath;
    private OnCompletionListener onCompletionListener;
    private long videoDuration;
    private OnProgressChangedListener onProgressChangedListener;
    private long frameStartTimeMs;
    private float volume = 1;

    private boolean playLoop = true;
    private volatile boolean userSoftDecode;
    private long externalTextureProgramHandle = 0;
    private SurfaceTexture surfaceTexture = null;
    private Surface mSurface = null;
    private int onFrameAvailableCount = 0;
    private OnPreparedListener onPreparedListener = null;
    private IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
    private boolean onFrameAvailable = false;
    private boolean isFirstRender = true;
    private boolean autoStartPlay = true;
    private boolean videoRenderingStart = false;
    private boolean isRelease = false;
    private OnStartRenderListener onStartRenderListener = null;
    private boolean prepareSyn = false;
    private boolean needFadeShow = true;


    public BZVideoView(Context context) {
        this(context, null);
    }

    public BZVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        userSoftDecode = !BZDeviceUtils.hardDecoderEnable();
    }

    protected void init() {
        if (BZOpenGlUtils.detectOpenGLES30()) {
            setEGLContextClientVersion(3);
        } else {
            setEGLContextClientVersion(2);
            BZLogUtil.d(TAG, "OpenGL ES 3.0 not supported on device");
        }
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        externalTextureProgramHandle = ExternalTextureProgram.initNative(true, needFadeShow);
        int textureID = ExternalTextureProgram.initGlResource(externalTextureProgramHandle);
        ExternalTextureProgram.setVideoRotation(externalTextureProgramHandle, videoRotate);

        surfaceTexture = new SurfaceTexture(textureID);
        mSurface = new Surface(surfaceTexture);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                onFrameAvailableCount++;
                onFrameAvailable = true;
                requestRender();
            }
        });
        ijkMediaPlayer.setSurface(mSurface);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        super.onSurfaceChanged(gl, width, height);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        super.onDrawFrame(gl);
        while (prepareSyn && !videoRenderingStart && !isRelease) {
            try {
                Thread.sleep(30);
            } catch (Exception e) {
                e.printStackTrace();
            }
            BZLogUtil.v(TAG, "!videoRenderingStart sleep");
        }
        glClearColor(bzClearColor.r, bzClearColor.g, bzClearColor.b, bzClearColor.a);
        glClear(GL_COLOR_BUFFER_BIT);
        if (null == surfaceTexture || null == mSurface || !onFrameAvailable) {
            return;
        }
        updateTexImage();
        if (null != onDrawFrameListener) {
            onDrawFrameListener.onDrawFrame(0);
        } else {
            glClearColor(bzClearColor.r, bzClearColor.g, bzClearColor.b, bzClearColor.a);
            glClear(GL_COLOR_BUFFER_BIT);
            if (null != mDrawViewPort)
                glViewport(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);
            ExternalTextureProgram.onDrawFrame(externalTextureProgramHandle);
        }
        if (null != onProgressChangedListener) {
            float progress = ijkMediaPlayer.getCurrentPosition() * 1.0f / videoDuration;
            if (progress < 0) progress = 0;
            if (progress > 1) progress = 1;
            onProgressChangedListener.onProgressChanged(progress);
        }
        limitFrameRate(33);
        if (isFirstRender) {
            isFirstRender = false;
            if (null != onStartRenderListener) {
                onStartRenderListener.onStartRender();
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

    private void updateTexImage() {
        if (onFrameAvailableCount <= 0) {
            return;
        }
        try {
            if (null != surfaceTexture)
                surfaceTexture.updateTexImage();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        onFrameAvailableCount--;
    }

    @Override
    public void onPause() {
        isRelease = true;
        if (null != mSurface) {
            mSurface.release();
            mSurface = null;
        }
        if (null != surfaceTexture) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                ExternalTextureProgram.releaseGlResource(externalTextureProgramHandle);
                externalTextureProgramHandle = 0;
            }
        });
        super.onPause();
    }

    public void setUserSoftDecode(boolean userSoftDecode) {
        this.userSoftDecode = userSoftDecode;
    }

    public void setDataSource(final String videoPath) {
        this.videoPath = videoPath;
        BZLogUtil.d(TAG, "setDataSource videoPath=" + videoPath);
        if (Looper.getMainLooper() == Looper.myLooper()) {
            BZLogUtil.d(TAG, "is mainLooper start");
            setDataSourceImp(videoPath);
        } else {
            post(new Runnable() {
                @Override
                public void run() {
                    setDataSourceImp(videoPath);
                }
            });
        }
    }

    private synchronized void setDataSourceImp(final String videoPath) {
        if (ijkMediaPlayer.isPlaying()) {
            ijkMediaPlayer.pause();
            ijkMediaPlayer.stop();
        }
        ijkMediaPlayer.reset();
        onFrameAvailable = false;
        isFirstRender = true;
        videoRenderingStart = false;
        isRelease = false;

        BZMedia.getVideoInfo(videoPath, new BZMedia.OnSendMediaInfoListener() {
            @Override
            public void sendMediaInfo(int what, int extra) {
                switch (what) {
                    case BZMedia.MEDIA_INFO_WHAT_VIDEO_DURATION:
                        BZLogUtil.d(TAG, "MEDIA_INFO_WHAT_VIDEO_DURATION=" + extra);
                        videoDuration = extra;
                        break;
                    case BZMedia.MEDIA_INFO_WHAT_VIDEO_ROTATE:
                        BZLogUtil.d(TAG, "MEDIA_INFO_WHAT_VIDEO_ROTATE=" + extra);
                        videoRotate = extra;
                        ExternalTextureProgram.setVideoRotation(externalTextureProgramHandle, videoRotate);
                        break;
                    case BZMedia.MEDIA_INFO_WHAT_VIDEO_WIDTH:
                        BZLogUtil.d(TAG, "MEDIA_INFO_WHAT_VIDEO_WIDTH=" + extra);
                        videoWidth = extra;
                        break;
                    case BZMedia.MEDIA_INFO_WHAT_VIDEO_HEIGHT:
                        BZLogUtil.d(TAG, "MEDIA_INFO_WHAT_VIDEO_HEIGHT=" + extra);
                        videoHeight = extra;
                        break;
                }
            }
        });
        if (videoRotate == 90 || videoRotate == 270) {
            int temp = videoWidth;
            videoWidth = videoHeight;
            videoHeight = temp;
        }

        calcViewport();
        try {
            ijkMediaPlayer.setDataSource(videoPath);
            ijkMediaPlayer.setLooping(playLoop);
            if (!userSoftDecode) {
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                BZLogUtil.d(TAG, "setOption mediacodec 1");
            }
            ijkMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(IMediaPlayer iMediaPlayer) {
                    if (autoStartPlay) {
                        iMediaPlayer.start();
                    } else {
                        iMediaPlayer.pause();
                    }
                    if (null != onPreparedListener) {
                        onPreparedListener.onPrepared();
                    }
                }
            });
            ijkMediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IMediaPlayer iMediaPlayer) {
                    if (null != onCompletionListener) {
                        onCompletionListener.onCompletion(true);
                    }
                }
            });
            ijkMediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                    if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        BZLogUtil.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                        videoRenderingStart = true;
                    }
                    return false;
                }
            });
            ijkMediaPlayer.setVolume(volume, volume);
            ijkMediaPlayer.prepareAsync();
        } catch (IOException e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public long videoPlayerOnDrawFrame(int textureId) {
        ExternalTextureProgram.onDrawFrame(externalTextureProgramHandle);
        return ijkMediaPlayer.getCurrentPosition();
    }

    public long videoPlayerGetCurrentAudioPts() {
        return ijkMediaPlayer.getCurrentPosition();
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setPlayLoop(final boolean playLoop) {
        this.playLoop = playLoop;
    }

    public void start() {
        if (getRenderMode() != RENDERMODE_CONTINUOUSLY)
            setRenderMode(RENDERMODE_CONTINUOUSLY);
        if (!ijkMediaPlayer.isPlaying())
            ijkMediaPlayer.start();
    }

    public boolean isPlaying() {
        return ijkMediaPlayer.isPlaying();
    }

    public int getVideoRotate() {
        return videoRotate;
    }

    public void pause() {
        if (getRenderMode() != RENDERMODE_WHEN_DIRTY)
            setRenderMode(RENDERMODE_WHEN_DIRTY);
        if (ijkMediaPlayer.isPlaying())
            ijkMediaPlayer.pause();
    }

    public boolean isPause() {
        return !ijkMediaPlayer.isPlaying();
    }

    public void release() {
        isRelease = true;
        if (ijkMediaPlayer.isPlaying()) {
            ijkMediaPlayer.pause();
            ijkMediaPlayer.stop();
        }
        ijkMediaPlayer.reset();
        ijkMediaPlayer.release();
        onPause();
    }

    public long getVideoDuration() {
        return videoDuration;
    }

    public void seek(final float progress) {
        ijkMediaPlayer.seekTo((long) (videoDuration * progress));
        requestRender();
    }

    public void setNeedFadeShow(boolean needFadeShow) {
        this.needFadeShow = needFadeShow;
    }

    /**
     * 用GLSurfaceView 有的手机会有问题,只能异步
     */
    public void setPrepareSyn(boolean prepareSyn) {
//        this.prepareSyn = prepareSyn;
    }

    public void setAutoStartPlay(boolean autoStartPlay) {
        this.autoStartPlay = autoStartPlay;
    }

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    public void setOnProgressChangedListener(OnProgressChangedListener onProgressChangedListener) {
        this.onProgressChangedListener = onProgressChangedListener;
    }

    public void setOnStartRenderListener(OnStartRenderListener onStartRenderListener) {
        this.onStartRenderListener = onStartRenderListener;
    }

    public void startSeek() {

    }

    public void stopSeek() {

    }


    public void setVolume(float volume) {
        this.volume = volume;
        ijkMediaPlayer.setVolume(volume, volume);
    }

    public float getVideoFps() {
        return 30;
    }


    public interface OnCompletionListener {
        void onCompletion(boolean isCompletion);
    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnStartRenderListener {
        void onStartRender();
    }

    public interface OnProgressChangedListener {
        void onProgressChanged(float progress);
    }


}
