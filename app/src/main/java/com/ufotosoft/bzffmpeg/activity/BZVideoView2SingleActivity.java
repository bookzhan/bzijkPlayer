package com.ufotosoft.bzffmpeg.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.luoye.bzijkplayer.R;
import com.ufotosoft.bzmedia.utils.BZLogUtil;
import com.ufotosoft.bzmedia.widget.BZVideoView2;

public class BZVideoView2SingleActivity extends AppCompatActivity {
    private String path = "/sdcard/bzmedia/temp_134.mp4";
//    private String path = "/storage/emulated/0/DCIM/InstaStory/story_1561959902458.mp4";
    private BZVideoView2 bz_video_view2;
    private float scale = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bzvideo_view2_single);
        bz_video_view2 = findViewById(R.id.bz_video_view2);
        bz_video_view2.setAutoStartPlay(false);
//        bz_video_view2.setPrepareSyn(true);
//        bz_video_view2.setNeedFadeShow(false);
//        bz_video_view2.setClearBackground(new BZColor(1.0f, 1.0f, 0, 1));
//        bz_video_view2.setDataSource(BZAssetsFileManager.getFinalPath(this,"sexy.mp4"));

        String videoPath = getIntent().getStringExtra("path");
        if (null != videoPath) {
            path = videoPath;
        }
        bz_video_view2.pause();
        bz_video_view2.setDataSource(path);

        bz_video_view2.setOnStartRenderListener(new BZVideoView2.OnStartRenderListener() {
            @Override
            public void onStartRender() {
                BZLogUtil.d("bz_onStartRender", "onStartRender");
            }
        });
        bz_video_view2.setOnDrawFrameListener(new BZVideoView2.OnDrawFrameListener() {
            @Override
            public int onDrawFrame(int textureId, int textureWidth, int textureHeight) {
                return textureId;
            }
        });
    }

    @Override
    protected void onPause() {
        bz_video_view2.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        bz_video_view2.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bz_video_view2.onPause();
        bz_video_view2.release();
    }

    public void move(View view) {
        scale += 0.5;
        bz_video_view2.setY(-400);
        bz_video_view2.setScaleX(scale);
        bz_video_view2.setScaleY(scale);
    }

    public void startPlay(View view) {
        bz_video_view2.start();
    }
}
