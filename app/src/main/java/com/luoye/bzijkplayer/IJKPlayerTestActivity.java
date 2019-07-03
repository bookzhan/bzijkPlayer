package com.luoye.bzijkplayer;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;


import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IJKPlayerTestActivity extends AppCompatActivity {

    private static final String TAG = "bz_IJKPlayerTest";
    private TextureView texture_view;
    private Surface surface;
    private IjkMediaPlayer ijkMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ijkplayer_test);
        texture_view = findViewById(R.id.texture_view);
        texture_view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                IJKPlayerTestActivity.this.surface = new Surface(surfaceTexture);
                startPlay(null);
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

            }
        });
    }

    public void startPlay(View view) {
        if (null == surface) {
            Log.d(TAG, "null == surface");
            return;
        }
        if (null == ijkMediaPlayer) {
            ijkMediaPlayer = new IjkMediaPlayer();
        } else {
            ijkMediaPlayer.reset();
        }
        try {
            ijkMediaPlayer.pause();
            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "render-wait-start", 1);
//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-sync", 1);

//            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

            ijkMediaPlayer.setSurface(surface);
//            ijkMediaPlayer.setDataSource("/sdcard/bzmedia/av_test_1080x1920_16_r0.mp4");
            ijkMediaPlayer.setDataSource("/sdcard/bzmedia/temp_134.mp4");
            ijkMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(IMediaPlayer iMediaPlayer) {
                    ijkMediaPlayer.pause();
                }
            });
            ijkMediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                    return false;
                }
            });
            ijkMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != ijkMediaPlayer) {
            ijkMediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != ijkMediaPlayer) {
            ijkMediaPlayer.stop();
            ijkMediaPlayer.release();
        }
    }

    public void start(View view) {
        ijkMediaPlayer.start();
    }
}
