package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Surface;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.bean.BZColor;
import com.ufotosoft.bzmedia.bean.ViewPort;
import com.ufotosoft.bzmedia.glutils.BaseProgram;
import com.ufotosoft.bzmedia.glutils.ExternalTextureProgram;
import com.ufotosoft.bzmedia.glutils.FrameBufferUtil;
import com.ufotosoft.bzmedia.utils.BZDeviceUtils;
import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * Created by zhandalin on 2019-02-16 13:34.
 * 说明:内部封装了MediaPlayer, 视频帧是自己画的,可以当做普通View来处理
 */

public class BZVideoView2 extends GLTextureView implements SurfaceTexture.OnFrameAvailableListener, GLTextureView.Renderer {
    private static final String TAG = "bz_BZVideoView2";
    private static final int MSG_WHAT_GET_PLAY_POSITION = 1;
    private long nativeHandle = 0;

    protected boolean mIsUsingMask = false;

    protected boolean mFitFullView = false;

    protected float mMaskAspectRatio = 1.0f;

    private IjkMediaPlayer mMediaPlayer = new IjkMediaPlayer();
    private String videoPath;
    private int videoDuration, count;
    private int videoWidth, videoHeight;
    private int videoRotate = 0;
    private boolean useSoftDecode = false;
    private int requestRenderCount = 0;
    private boolean prepareSyn = false;


    protected ViewPort mDrawViewPort = new ViewPort();
    private long lastPosition = 0;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_GET_PLAY_POSITION:
                    handler.removeMessages(MSG_WHAT_GET_PLAY_POSITION);
                    if (null != onPlayProgressListener && mMediaPlayer.isPlaying() && mMediaPlayer.getDuration() > 0) {
                        long currentPosition = mMediaPlayer.getCurrentPosition();
                        if (currentPosition > lastPosition) {
                            onPlayProgressListener.onPlayProgress(1.0f * currentPosition / mMediaPlayer.getDuration());
                            lastPosition = currentPosition;
                        }
                    }
                    if (count % 100 == 0) {
                        BZLogUtil.v(TAG, "msg_what_get_play_position runing videoPath=" + videoPath);
                    }
                    count++;
                    handler.sendEmptyMessageDelayed(MSG_WHAT_GET_PLAY_POSITION, 15);
                    break;
            }
            return true;
        }
    });
    private OnPlayProgressListener onPlayProgressListener;
    private Surface surface;
    private OnPreparedListener onPreparedListener;
    private SurfaceTexture surfaceTexture;
    private int viewWidth, viewHeight;
    private boolean prepareFinished = false;
    private float mVolume = 1;
    private OnDrawFrameListener onDrawFrameListener;
    private OnViewPortChangeListener onViewPortChangeListener;
    private OnStartRenderListener onStartRenderListener;
    private FrameBufferUtil frameBufferUtil;
    private BaseProgram baseProgram;
    private boolean mPlayLoop = false;
    private boolean startRenderListenerHasCallBack = false;
    private BZColor clearBackground = new BZColor(1, 1, 1, 1);
    private OnPlayCompleteListener onPlayCompleteListener = null;
    private boolean onFrameAvailable = false;
    private long frameStartTimeMs = 0;
    private boolean autoStartPlay = true;
    private boolean videoRenderingStart = false;
    private boolean isRelease = false;
    private boolean needFadeShow = true;
    private volatile boolean isOnDrawFraming = false;
    private boolean isPause = true;

    public BZVideoView2(Context context) {
        this(context, null);
    }

    public BZVideoView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSelf();
    }

    protected void initSelf() {
        useSoftDecode = !BZDeviceUtils.hardDecoderEnable();
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setRenderer(this);
        setRenderMode(GLTextureView.RENDERMODE_WHEN_DIRTY);
    }

    public void setUseSoftDecode(boolean useSoftDecode) {
        this.useSoftDecode = useSoftDecode;
    }

    @Override
    public void onPause() {
        BZLogUtil.d(TAG, "onPause videoPath=" + videoPath);
        isRelease = true;
        while (isOnDrawFraming) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BZLogUtil.d(TAG, "onPause isOnDrawFraming waiting");
        }
        pause();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != surfaceTexture) {
                    surfaceTexture.release();
                    surfaceTexture = null;
                }
                if (null != surface) {
                    surface.release();
                    surface = null;
                }
                ExternalTextureProgram.releaseGlResource(nativeHandle);
                nativeHandle = 0;
                if (null != frameBufferUtil) {
                    frameBufferUtil.release();
                    frameBufferUtil = null;
                }
                if (null != baseProgram) {
                    baseProgram.release();
                    baseProgram = null;
                }
            }
        });
        super.onPause();
        BZLogUtil.d(TAG, "onPause finish videoPath=" + videoPath);
    }

    protected synchronized boolean calcViewport() {
        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return false;

        float scaling;
        if (mIsUsingMask) {
            scaling = mMaskAspectRatio;
        } else {
            scaling = videoWidth / (float) videoHeight;
        }

        float viewRatio = viewWidth / (float) viewHeight;
        float s = scaling / viewRatio;

        int w, h;

        if (mFitFullView) {
            //撑满全部view(内容大于view)
            if (s > 1.0) {
                w = (int) Math.ceil(viewHeight * scaling);
                h = viewHeight;
            } else {
                w = viewWidth;
                h = (int) Math.ceil(viewWidth / scaling);
            }
        } else {
            //显示全部内容(内容小于view)
            if (s > 1.0) {
                w = viewWidth;
                h = (int) Math.ceil(viewWidth / scaling);
            } else {
                h = viewHeight;
                w = (int) Math.ceil(viewHeight * scaling);
            }
        }
        //需要转换成2的整数倍
        mDrawViewPort.width = w;
        mDrawViewPort.height = h;
        mDrawViewPort.x = (int) Math.ceil((viewWidth - mDrawViewPort.width) / 2);
        mDrawViewPort.y = (int) Math.ceil((viewHeight - mDrawViewPort.height) / 2);

        if (null != onViewPortChangeListener)
            onViewPortChangeListener.onViewPortChange(mDrawViewPort);
        queueEvent(new Runnable() {
            @Override
            public void run() {
                ExternalTextureProgram.setVideoRotation(nativeHandle, videoRotate);
            }
        });
        BZLogUtil.d(TAG, String.format(Locale.CHINESE, "View port: x=%d, y=%d, width=%d, height=%d", mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height));
        return true;
    }

    public void setDataSource(String videoPath) {
        this.videoPath = videoPath;
        BZMedia.getVideoInfo(videoPath, new BZMedia.OnSendMediaInfoListener() {
            @Override
            public void sendMediaInfo(int what, int extra) {
                switch (what) {
                    case BZMedia.MEDIA_INFO_WHAT_VIDEO_DURATION:
                        videoDuration = extra;
                        BZLogUtil.d(TAG, "MEDIA_INFO_WHAT_VIDEO_DURATION=" + extra);
                        break;
                    case BZMedia.MEDIA_INFO_WHAT_VIDEO_ROTATE:
                        BZLogUtil.d(TAG, "MEDIA_INFO_WHAT_VIDEO_ROTATE=" + extra);
                        videoRotate = extra;
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
        startPlay(videoPath);
    }

    private void startPlay(final String videoPath) {
        if (null == videoPath) {
            BZLogUtil.e(TAG, "null == videoPath || null == surface");
            return;
        }
        BZLogUtil.d(TAG, "startPlay videoPath=" + videoPath);
        lastPosition = 0;
        prepareFinished = false;
        onFrameAvailable = false;
        isRelease = false;
        try {
            mMediaPlayer.reset();
//            mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5);
            if (!useSoftDecode)
                mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);

            if (isPause) {
                mMediaPlayer.pause();
            }
            mMediaPlayer.setLooping(mPlayLoop);
            mMediaPlayer.setDataSource(videoPath);
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(IMediaPlayer iMediaPlayer) {
                    if (autoStartPlay) {
                        iMediaPlayer.start();
                    } else {
                        iMediaPlayer.pause();
                    }
                    mMediaPlayer.setVolume(mVolume, mVolume);
                    if (null != onPreparedListener)
                        onPreparedListener.onPrepared();
                    prepareFinished = true;
                    startRenderListenerHasCallBack = false;
                    handler.sendEmptyMessage(MSG_WHAT_GET_PLAY_POSITION);
                    BZLogUtil.d(TAG, "prepareFinished videoPath=" + videoPath);
                }
            });
            mMediaPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IMediaPlayer iMediaPlayer, int framework_err, int impl_err) {
                    BZLogUtil.d(TAG, "IMediaPlayer onError framework_err=" + framework_err + " impl_err=" + impl_err);
                    useSoftDecode = true;
                    return false;
                }
            });
            mMediaPlayer.setOnSeekCompleteListener(new IMediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(IMediaPlayer iMediaPlayer) {
                    lastPosition = 0;
                    BZLogUtil.d(TAG, "onSeekComplete");
                }
            });
            mMediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IMediaPlayer iMediaPlayer) {
                    lastPosition = 0;
                    if (null != onPlayCompleteListener)
                        onPlayCompleteListener.onPlayComplete();
                }
            });
            videoRenderingStart = false;
            mMediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                    if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        BZLogUtil.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                        videoRenderingStart = true;
                    }
                    return false;
                }
            });

            mMediaPlayer.prepareAsync();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, "startPlay fail videoPath=" + videoPath);
            BZLogUtil.e(TAG, e);
        }
    }

    public int getVideoDuration() {
        return videoDuration;
    }

    public int getVideoRotate() {
        return videoRotate;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void start() {
        isPause = false;
        BZLogUtil.d(TAG, "start videoPath=" + videoPath);
        try {
            if (!mMediaPlayer.isPlaying())
                mMediaPlayer.start();
            handler.sendEmptyMessage(MSG_WHAT_GET_PLAY_POSITION);
            lastPosition = 0;
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void pause() {
        isPause = true;
        BZLogUtil.d(TAG, "pause videoPath=" + videoPath);
        try {
            handler.removeMessages(MSG_WHAT_GET_PLAY_POSITION);
            mMediaPlayer.pause();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void release() {
        isRelease = true;
        try {
            onPause();
            lastPosition = 0;
            handler.removeMessages(MSG_WHAT_GET_PLAY_POSITION);
            mMediaPlayer.release();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        BZLogUtil.d(TAG, "release finish");
    }

    public void seek(float position) {
        try {
            lastPosition = 0;
            mMediaPlayer.seekTo((long) (position * videoDuration));
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void setVolume(float volume) {
        mVolume = volume;
        try {
            mMediaPlayer.setVolume(volume, volume);
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public float getVolume() {
        return mVolume;
    }

    public void setFitFullView(boolean fitFullView) {
        mFitFullView = fitFullView;
        calcViewport();
    }

    public void setOnPlayProgressListener(OnPlayProgressListener onPlayProgressListener) {
        this.onPlayProgressListener = onPlayProgressListener;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (prepareFinished) {
            requestRender();
        }
        onFrameAvailable = true;
        requestRenderCount++;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        BZLogUtil.d(TAG, "onSurfaceCreated videoPath=" + videoPath);
        glClearColor(clearBackground.r, clearBackground.g, clearBackground.b, clearBackground.a);
        if (nativeHandle != 0) {
            ExternalTextureProgram.releaseGlResource(nativeHandle);
            nativeHandle = 0;
        }
        nativeHandle = ExternalTextureProgram.initNative(true, needFadeShow);
        int textureId = ExternalTextureProgram.initGlResource(nativeHandle);
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);
        surface = new Surface(surfaceTexture);
        mMediaPlayer.setSurface(surface);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWidth = width;
        viewHeight = height;
        calcViewport();
        BZLogUtil.d(TAG, "onSurfaceChanged videoPath=" + videoPath);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        isOnDrawFraming = true;
        while (prepareSyn && !videoRenderingStart && !isRelease) {
            try {
                Thread.sleep(30);
            } catch (Exception e) {
                e.printStackTrace();
            }
            BZLogUtil.v(TAG, "prepareSyn && !videoRenderingStart && !isRelease sleep");
        }
        glClearColor(clearBackground.r, clearBackground.g, clearBackground.b, clearBackground.a);
        glClear(GL_COLOR_BUFFER_BIT);
        if (null == surfaceTexture || null == surface || !onFrameAvailable) {
            isOnDrawFraming = false;
            return;
        }
        //防止出现黑屏
        updateTexImage();

        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            BZLogUtil.w(TAG, "videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0");
            isOnDrawFraming = false;
            return;
        }

        //直接画屏幕
        if (null == onDrawFrameListener) {
            glViewport(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);
            ExternalTextureProgram.onDrawFrame(nativeHandle);
        } else {
            //需要对纹理进一步处理
            if (null == frameBufferUtil) {
                frameBufferUtil = new FrameBufferUtil(videoWidth, videoHeight);
            }
            //释放掉重新new
            if (videoWidth != frameBufferUtil.getWidth() || videoHeight != frameBufferUtil.getHeight()) {
                frameBufferUtil.release();
                frameBufferUtil = new FrameBufferUtil(videoWidth, videoHeight);
            }
            //转换成普通纹理
            frameBufferUtil.bindFrameBuffer();
            glClearColor(clearBackground.r, clearBackground.g, clearBackground.b, clearBackground.a);
            glClear(GL_COLOR_BUFFER_BIT);
            glViewport(0, 0, videoWidth, videoHeight);
            ExternalTextureProgram.onDrawFrame(nativeHandle);
            frameBufferUtil.unbindFrameBuffer();

            //画屏
            int retTextureID = onDrawFrameListener.onDrawFrame(frameBufferUtil.getFrameBufferTextureID(), videoWidth, videoHeight);
            if (null == baseProgram)
                baseProgram = new BaseProgram(false);

            glViewport(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);
            baseProgram.draw(retTextureID);
        }
        limitFrameRate(33);

        if (!startRenderListenerHasCallBack && null != onStartRenderListener) {
            onStartRenderListener.onStartRender();
            startRenderListenerHasCallBack = true;
        }
        isOnDrawFraming = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        BZLogUtil.d(TAG, "onDetachedFromWindow");
        isRelease = true;
        while (isOnDrawFraming) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BZLogUtil.d(TAG, "onDetachedFromWindow isOnDrawFraming waiting");
        }
        super.onDetachedFromWindow();
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
        if (requestRenderCount <= 0) {
            return;
        }
        try {
            if (null != surfaceTexture)
                surfaceTexture.updateTexImage();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        requestRenderCount--;
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public void setOnDrawFrameListener(OnDrawFrameListener onDrawFrameListener) {
        this.onDrawFrameListener = onDrawFrameListener;
    }

    public void setOnViewPortChangeListener(OnViewPortChangeListener onViewPortChangeListener) {
        this.onViewPortChangeListener = onViewPortChangeListener;
    }

    public void setOnStartRenderListener(OnStartRenderListener onStartRenderListener) {
        this.onStartRenderListener = onStartRenderListener;
    }

    public void setClearBackground(BZColor bzColor) {
        if (null == bzColor) return;
        clearBackground.r = bzColor.r;
        clearBackground.g = bzColor.g;
        clearBackground.b = bzColor.b;
        clearBackground.a = bzColor.a;
    }

    public void setNeedFadeShow(boolean needFadeShow) {
        this.needFadeShow = needFadeShow;
    }

    public void setAutoStartPlay(boolean autoStartPlay) {
        this.autoStartPlay = autoStartPlay;
    }

    public void setPlayLoop(boolean playLoop) {
        mPlayLoop = playLoop;
        try {
            mMediaPlayer.setLooping(playLoop);
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void setPrepareSyn(boolean prepareSyn) {
        this.prepareSyn = prepareSyn;
    }

    public void setOnPlayCompleteListener(OnPlayCompleteListener onPlayCompleteListener) {
        this.onPlayCompleteListener = onPlayCompleteListener;
    }

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }


    public interface OnPlayProgressListener {
        void onPlayProgress(float progress);
    }

    public interface OnViewPortChangeListener {
        void onViewPortChange(ViewPort viewport);
    }

    public interface OnStartRenderListener {
        void onStartRender();
    }


    public interface OnDrawFrameListener {
        int onDrawFrame(int textureId, int textureWidth, int textureHeight);
    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnPlayCompleteListener {
        void onPlayComplete();
    }
}
