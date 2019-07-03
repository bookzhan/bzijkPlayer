package com.ufotosoft.bzmedia.utils;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 *
 */
public class BZScreenUtils {


    public static int getStatusBarHeightUseConstant(Context context) {
        return (int) Math.ceil(25 * context.getResources().getDisplayMetrics().density);
    }

    /**
     * @return 返回的只是可以展示类容的高度
     */
    public static Point getScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        Point size = new Point();
        size.x = dm.widthPixels;
        size.y = dm.heightPixels - getStatusBarHeight(context);
        return size;
    }

    /**
     * @return 屏幕的宽
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * @return 屏幕的高 返回的只是可以展示类容的高度
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels - getStatusBarHeight(context);
    }


    /**
     * @return 计算系统状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


}
