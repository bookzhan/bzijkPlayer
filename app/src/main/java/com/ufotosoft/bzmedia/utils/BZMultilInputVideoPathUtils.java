package com.ufotosoft.bzmedia.utils;

import com.ufotosoft.bzmedia.BZMedia;

/**
 * Created by zhandalin on 2018-07-12 11:04.
 * 说明:
 */
public class BZMultilInputVideoPathUtils {

    /**
     * 要保证传入的视频数量与样式对应
     */
    public static String[] checkVideoPath(String[] videoPath, BZMedia.MultiInputVideoLayoutType layoutType) {
        if (null == videoPath || videoPath.length <= 0) {
            return null;
        }
        boolean handle = false;
        int count = 0;
        //判断视频数量是否足够
        switch (layoutType) {
            case INPUTS_1_NORMAL:
            case INPUTS_1_CIRCLE:
            case INPUTS_1_RHOMBUS:
                handle = true;
                count = 1;
                break;
            case INPUTS_2_H:
            case INPUTS_2_V:
                if (videoPath.length > 1) {
                    handle = true;
                }
                count = 2;
                break;
            case INPUTS_3_H:
            case INPUTS_3_V:
                if (videoPath.length > 2) {
                    handle = true;
                }
                count = 3;
                break;
            case INPUTS_4:
                if (videoPath.length > 3) {
                    handle = true;
                }
                count = 4;
                break;
            case INPUTS_9:
                if (videoPath.length > 8) {
                    handle = true;
                }
                count = 9;
                break;
        }
        if (handle) {
            String[] finalVideoPath = new String[count];
            for (int i = 0; i < count; i++) {
                finalVideoPath[i] = videoPath[i];
            }
            return finalVideoPath;
        }
        return null;
    }
}
