package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by zhandalin on 2019-06-29 13:31.
 * 说明:直接继承自TextureView, 只有视频播放,不做任何处理
 */
public class BZSimpleVideoView extends TextureView {
    private final static String TAG = "bz_BZSimpleVideoView";
    private final Object playLock = new Object();
    private final static int UPDATE_PROGRESS_MSG = 0x23;

    private IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
    private String videoPath = null;
    private Surface surface = null;
    private boolean isStart = true;
    private OnProgressListener onProgressListener = null;
    private long index = 0;
    private long getBitmapIndex = 0;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == UPDATE_PROGRESS_MSG) {
                stopLoopMsg();
                if (null != onProgressListener) {
                    long duration = ijkMediaPlayer.getDuration();
                    if (duration > 0) {
                        onProgressListener.onProgress(ijkMediaPlayer.getCurrentPosition() * 1.0f / duration);
                    }
                }
                handler.sendEmptyMessageDelayed(UPDATE_PROGRESS_MSG, 30);
            }
            if (index % 30 == 0) {
                BZLogUtil.v(TAG, "handler is run");
            }
            if (index > Long.MAX_VALUE - 1) {
                index = 0;
            }
            index++;
            return false;
        }
    });
    private boolean playLoop = false;
    private OnVideoBitmapListener onVideoBitmapListener = null;
    private float mVolume = 1;

    public BZSimpleVideoView(Context context) {
        this(context, null);
    }

    public BZSimpleVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BZSimpleVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                synchronized (playLock) {
                    BZSimpleVideoView.this.surface = new Surface(surfaceTexture);
                    if (null != videoPath) {
                        startPlay();
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                if (null != onVideoBitmapListener) {
                    long startTime = System.currentTimeMillis();
                    Bitmap bitmap = getBitmap();
                    if (getBitmapIndex > Long.MAX_VALUE - 1) {
                        getBitmapIndex = 0;
                    }
                    if (getBitmapIndex % 30 == 0) {
                        BZLogUtil.v(TAG, "getBitmap 耗时=" + (System.currentTimeMillis() - startTime));
                    }
                    onVideoBitmapListener.onVideoBitmap(bitmap);
                }
            }
        });
    }

    public void setPlayLoop(boolean playLoop) {
        this.playLoop = playLoop;
    }

    public void setDataSource(String videoPath) {
        synchronized (playLock) {
            this.videoPath = videoPath;
            if (null != surface) {
                startPlay();
            }
        }
    }

    private void startPlay() {
        if (null == videoPath || null == surface) {
            BZLogUtil.d(TAG, "null == videoPath||null==surface");
            return;
        }
        BZLogUtil.d(TAG, "startPlay");
        try {
            ijkMediaPlayer.pause();
            ijkMediaPlayer.stop();
            ijkMediaPlayer.reset();

            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);

            ijkMediaPlayer.setLooping(playLoop);
            ijkMediaPlayer.setSurface(surface);
            ijkMediaPlayer.setDataSource(videoPath);
            ijkMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(IMediaPlayer iMediaPlayer) {
                    iMediaPlayer.setVolume(mVolume, mVolume);
                    if (isStart) {
                        iMediaPlayer.start();
                    } else {
                        iMediaPlayer.pause();
                    }
                    startLoopMsg();
                }
            });
            ijkMediaPlayer.prepareAsync();
        } catch (Exception e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public IjkMediaPlayer getMediaPlayer() {
        return ijkMediaPlayer;
    }

    public void start() {
        startLoopMsg();
        isStart = true;
        if (!ijkMediaPlayer.isPlaying())
            ijkMediaPlayer.start();

    }

    public void pause() {
        isStart = false;
        if (ijkMediaPlayer.isPlaying())
            ijkMediaPlayer.pause();

        stopLoopMsg();
    }

    private void startLoopMsg() {
        if (null != onProgressListener) {
            handler.sendEmptyMessage(UPDATE_PROGRESS_MSG);
        }
    }

    private void stopLoopMsg() {
        handler.removeMessages(UPDATE_PROGRESS_MSG);
    }

    public boolean isPlaying() {
        return ijkMediaPlayer.isPlaying();
    }

    public void release() {
        stopLoopMsg();
        try {
            ijkMediaPlayer.pause();
            ijkMediaPlayer.stop();
            ijkMediaPlayer.reset();
            ijkMediaPlayer.release();
        } catch (Exception e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void setVolume(float volume) {
        this.mVolume = volume;
        ijkMediaPlayer.setVolume(volume, volume);
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
        startLoopMsg();
    }

    public void setOnVideoBitmapListener(OnVideoBitmapListener onVideoBitmapListener) {
        this.onVideoBitmapListener = onVideoBitmapListener;
    }


    public interface OnProgressListener {
        void onProgress(float progress);
    }

    public interface OnVideoBitmapListener {
        void onVideoBitmap(Bitmap bitmap);
    }
}
