package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.bean.ViewPort;
import com.ufotosoft.bzmedia.glutils.ExternalTextureProgram;
import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * Created by zhandalin on 2017-12-05 17:21.
 * 说明:内部封装了MediaPlayer, 视频帧是自己画的
 */

public class BZMPVideoView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "bz_BZVideoView4MediaPlayer";
    private static final int MSG_WHAT_GET_PLAY_POSITION = 1;
    private long nativeHandle = 0;

    protected boolean mIsUsingMask = false;

    protected boolean mFitFullView = false;

    protected float mMaskAspectRatio = 1.0f;

    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private String videoPath;
    private int videoDuration, count;
    private int videoWidth, videoHeight;
    private int videoRotate = 0;
    private long requestRenderCount = 0;

    protected ViewPort mDrawViewPort = new ViewPort();

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_GET_PLAY_POSITION:
                    handler.removeMessages(MSG_WHAT_GET_PLAY_POSITION);
                    if (null != onPlayProgressListener && mMediaPlayer.isPlaying() && mMediaPlayer.getDuration() > 0) {
                        onPlayProgressListener.onPlayProgress(1.0f * mMediaPlayer.getCurrentPosition() / mMediaPlayer.getDuration());
                    }
                    if (count % 20 == 0) {
                        BZLogUtil.v(TAG, "msg_what_get_play_position runing");
                    }
                    count++;
                    handler.sendEmptyMessageDelayed(MSG_WHAT_GET_PLAY_POSITION, 50);
                    break;
            }
            return true;
        }
    });
    private OnPlayProgressListener onPlayProgressListener;
    private Surface surface;
    private MediaPlayer.OnPreparedListener onPreparedListener;
    private SurfaceTexture surfaceTexture;
    private int viewWidth, viewHeight;
    private boolean prepareFinished = false;

    public BZMPVideoView(Context context) {
        this(context, null);
    }

    public BZMPVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    protected void init() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (nativeHandle != 0) {
            ExternalTextureProgram.releaseGlResource(nativeHandle);
            nativeHandle = 0;
        }
        nativeHandle = ExternalTextureProgram.initNative(false);
        int textureId = ExternalTextureProgram.initGlResource(nativeHandle);
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);
        surface = new Surface(surfaceTexture);
        if (null != videoPath) {
            startPlay(videoPath, surface);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWidth = width;
        viewHeight = height;
        calcViewport();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);
        //防止出现黑屏
        if (requestRenderCount <= 2) {
            try {
                if (null != surfaceTexture)
                    surfaceTexture.updateTexImage();
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
            return;
        }
        glViewport(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);
        ExternalTextureProgram.onDrawFrame(nativeHandle);
        try {
            if (null != surfaceTexture)
                surfaceTexture.updateTexImage();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    @Override
    public void onPause() {
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
            }
        });
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    protected boolean calcViewport() {
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

//        if (null != onViewportCalcCompleteListener)
//            onViewportCalcCompleteListener.onViewportCalcCompleteListener(mDrawViewPort);
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
        //表示gl环境已经好了
        if (null != surface) {
            startPlay(videoPath, surface);
        } else {
            BZLogUtil.d(TAG, "null==surfaceTexture");
        }
    }

    private void startPlay(final String videoPath, final Surface surface) {
        if (null == videoPath || null == surface) {
            BZLogUtil.e(TAG, "null == videoPath || null == surface");
            return;
        }
        requestRenderCount = 0;
        prepareFinished = false;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                ExternalTextureProgram.setVideoRotation(nativeHandle, videoRotate);
                try {
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(videoPath);
                    mMediaPlayer.setSurface(surface);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                    if (null != onPreparedListener)
                        onPreparedListener.onPrepared(mMediaPlayer);
                    prepareFinished = true;
                } catch (Throwable e) {
                    BZLogUtil.e("startPlay fail videoPath=" + videoPath);
                    BZLogUtil.e(TAG, e);
                }
            }
        });
        handler.sendEmptyMessage(MSG_WHAT_GET_PLAY_POSITION);
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

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }


    public void start() {
        try {
            if (!mMediaPlayer.isPlaying())
                mMediaPlayer.start();
            handler.sendEmptyMessage(MSG_WHAT_GET_PLAY_POSITION);
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void pause() {
        try {
            handler.removeMessages(MSG_WHAT_GET_PLAY_POSITION);
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.pause();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void release() {
        try {
            handler.removeMessages(MSG_WHAT_GET_PLAY_POSITION);
            mMediaPlayer.release();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void seek(float position) {
        try {
            mMediaPlayer.seekTo((int) (position * videoDuration + 0.5));
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void setOnPlayProgressListener(OnPlayProgressListener onPlayProgressListener) {
        this.onPlayProgressListener = onPlayProgressListener;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (prepareFinished) {
            requestRender();
        }
        requestRenderCount++;
    }

    public interface OnPlayProgressListener {
        void onPlayProgress(float progress);
    }

}
