package com.ufotosoft.bzmedia.utils;

import android.content.Context;

import java.io.File;

/**
 * Created by zhandalin on 2018-12-28 16:32.
 * 说明:缓存资产目录下的文件
 */
public class BZAssetsFileManager {
    private static final String TAG = "bz_AssetsFileManager";

    public static String getFinalPath(Context context, String path) {
        if (null == context || BZStringUtils.isEmpty(path)) {
            return path;
        }
        if (path.startsWith("/")) {
            return path;
        }
        //处理资产目录下的文件
        try {
            String fileDirPath = context.getFilesDir().getAbsolutePath();
            String fileName = BZMD5Util.md5(path);
            String substring = path.substring(path.lastIndexOf("."), path.length());

            String tempPath = fileDirPath + "/" + fileName + substring;
            if (new File(tempPath).exists()) {
                return tempPath;
            }
            BZFileUtils.fileCopy(context.getAssets().open(path), tempPath);
            return tempPath;
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        return path;
    }
}
