package com.ufotosoft.bzmedia.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.ufotosoft.bzmedia.glutils.BZOpenGlUtils;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by zhandalin on 2017-10-12 14:53.
 * 说明:提供资源解析服务,包括本地文件,资产目录文件
 */
public class BZResourceParserUtil {

    private static final String TAG = "bz_ResourceParserUtil";
    private static Context context;

    public static void init(Context context) {
        BZResourceParserUtil.context = context;
    }

    /**
     * call from jni
     */
    public static synchronized String getFinalIamgePath(String fileName, int rotate, int flipHorizontal, int flipVertical) {
        try {
            InputStream inputStream = null;
            if (fileName.startsWith("/")) {
                inputStream = new FileInputStream(fileName);
            } else {
                inputStream = context.getAssets().open(fileName);
            }
            fileName = context.getFilesDir().getAbsolutePath() + "/temp_" + System.currentTimeMillis() + ".png";
            BZFileUtils.fileCopy(inputStream, fileName);

//            Bitmap bitmap;
//            if (fileName.startsWith("/")) {
//                bitmap = BitmapFactory.decodeStream(new FileInputStream(fileName), null, null);
//            } else {
//                bitmap = BitmapFactory.decodeStream(context.getAssets().open(fileName), null, null);
//            }
//            Bitmap result = progressBitmap(bitmap, rotate, flipHorizontal, flipVertical);
//
//            fileName = context.getFilesDir().getAbsolutePath() + "/temp_" + System.currentTimeMillis() + ".png";
//            result.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(fileName));
//            bitmap.recycle();
//            result.recycle();
        } catch (Exception e) {
            BZLogUtil.e(TAG, e);
        }
        return fileName;
    }

    /**
     * @param width  纹理宽
     * @param height 纹理高
     * @return 返回一个中间是透明的圆形纹理ID, 必须在GL线程调用
     */
    public static synchronized int getCircleTexture(int width, int height) {
        if (width <= 0 || height <= 0) {
            width = 720;
            height = 720;
        }
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawCircle(width / 2, height / 2, Math.min(width / 2, height / 2), paint);

//        InputStream inputStream = null;
//        try {
//            inputStream = context.getAssets().open("oval_camera.png");
//            Bitmap bitmapFinal = BitmapFactory.decodeStream(inputStream, null, null);
//            int minSize = Math.min(width, height);
//            Rect rectSrc = new Rect(0, 0, bitmapFinal.getWidth(), bitmapFinal.getHeight());
//            int startX = (width - minSize) / 2;
//            int startY = (height - minSize) / 2;
//            Rect rectDis = new Rect(startX, startY, startX + minSize, startY + minSize);
//            canvas.drawRect(rectDis, paint);
//
//            canvas.drawBitmap(bitmapFinal, rectSrc, rectDis, new Paint());
//            bitmapFinal.recycle();
//        } catch (Exception e) {
//            BZLogUtil.e(TAG, e);
//        } finally {
//            if (null != inputStream) {
//                try {
//                    inputStream.close();
//                } catch (Exception e) {
//                    BZLogUtil.e(TAG, e);
//                }
//            }
//        }
        int texture = BZOpenGlUtils.loadTexture(bitmap);
        bitmap.recycle();
        return texture;
    }

    /**
     * @param width  纹理宽
     * @param height 纹理高
     * @return 返回一个中间是透明的菱形纹理ID, 必须在GL线程调用
     */
    public static synchronized int getRhombusTexture(int width, int height) {
        //最终图
        Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalBitmap);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        int size = Math.min(width, height);
        float finalSize = (float) (size * 1.0f / Math.sqrt(2));
        RectF rect = new RectF((width - finalSize) / 2, (height - finalSize) / 2, width - (width - finalSize) / 2, height - (height - finalSize) / 2);
        canvas.rotate(45, width / 2, height / 2);
        canvas.drawRect(rect, paint);

        int texture = BZOpenGlUtils.loadTexture(finalBitmap);
        finalBitmap.recycle();
        return texture;
    }


    private static Bitmap progressBitmap(Bitmap bitmap, int rotate, int flipHorizontal, int flipVertical) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        if (flipHorizontal > 0)
            matrix.postScale(-1, 1);   //镜像水平翻转
        if (flipVertical > 0)
            matrix.postScale(1, -1);   //镜像垂直翻转

        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}
