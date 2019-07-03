package com.ufotosoft.bzffmpeg;

import android.app.Application;
import android.os.Environment;

import com.luoye.bzijkplayer.BuildConfig;
import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.utils.BZFileUtils;

import java.io.File;

/**
 * Created by zhandalin on 2017-08-12 10:19.
 * 说明:
 */
public class BZApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/bzmedia");
        if (!file.exists()) {
            file.mkdirs();
        }
        BZMedia.init(getApplicationContext(), BuildConfig.DEBUG);
        BZFileUtils.createNewFile("/sdcard/bzmedia/input.txt");
//        try {
//            BZFileUtils.createNewFile("/sdcard/bzmedia/input.txt");
//            for (int i = 0; i < 9; i++) {
//                String videoPath = "/sdcard/bzmedia/input_" + (i + 1) + ".mp4";
//                BZFileUtils.fileCopy(getAssets().open("video/input_" + (i + 1) + ".mp4"), videoPath);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
