package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.utils.BZBitmapUtil;
import com.ufotosoft.bzmedia.utils.BZDensityUtil;
import com.ufotosoft.bzmedia.utils.BZLogUtil;
import com.ufotosoft.bzmedia.utils.BZMultilInputVideoPathUtils;

/**
 * Created by zhandalin on 2018-07-12 10:53.
 * 说明:多路视频SeekBar
 */
public class MultiVideoSeekBar extends VideoSeekBar {
    private static final String TAG = "bz_MultiVideoSeekBar";
    private String[] videoPaths = null;
    private boolean isIniting = false;
    private BZMedia.MultiInputVideoLayoutType layoutType = BZMedia.MultiInputVideoLayoutType.INPUTS_1_NORMAL;

    public MultiVideoSeekBar(Context context) {
        this(context, null);
    }

    public MultiVideoSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiVideoSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && null != videoPaths) {
            initVideoThumbnail(videoPaths);
        }
    }

    public void init(String videoPaths[], BZMedia.MultiInputVideoLayoutType layoutType) {
        this.layoutType = layoutType;
        this.videoPaths = BZMultilInputVideoPathUtils.checkVideoPath(videoPaths, layoutType);
    }

    protected void initVideoThumbnail(final String[] videoPaths) {
        if (isIniting) {
            BZLogUtil.d(TAG, "initVideoThumbnail isIniting");
            return;
        }
        isIniting = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int width = BZDensityUtil.getScreenWidth(getContext()) / backgroundImageNum * 4;
                if (layoutType == BZMedia.MultiInputVideoLayoutType.INPUTS_2_V || layoutType == BZMedia.MultiInputVideoLayoutType.INPUTS_3_V) {
                    width *= 4;
                }
                clipVideoFrameForMultilInput(videoPaths, layoutType.ordinal(), backgroundImageNum, width);
                isIniting = false;
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * call from jni
     */
    public void onGetBitmapFromVideo(int index, Bitmap bitmapSrc) {
        final int bitmapHeight = viewHeight - particleImagePadding;
        final int bitmapWidth = viewWidth / backgroundImageNum;
        bitmapRoundSize = bitmapSrc.getWidth() / 10;
        if (bitmapRoundSize < bitmapRoundMinSize)
            bitmapRoundSize = bitmapRoundMinSize;
        if (bitmapRoundSize > bitmapRoundMaxSize)
            bitmapRoundSize = bitmapRoundMaxSize;

        Bitmap bitmap = BZBitmapUtil.centerCutBitmap(bitmapSrc, bitmapSrc.getWidth(), bitmapSrc.getWidth() * bitmapHeight / bitmapWidth);
        if (null == bitmap) return;

        //处理圆角
        if (index == 0) {
            Bitmap roundImage = createRoundImage(bitmap, bitmapRoundSize, true);
            synchronized (this) {
                bitmapList.add(roundImage);
            }
            postInvalidate();
        } else if (index == backgroundImageNum - 1) {
            Bitmap roundImage = createRoundImage(bitmap, bitmapRoundSize, false);
            synchronized (this) {
                bitmapList.add(roundImage);
            }
            postInvalidate();
        } else {
            synchronized (this) {
                bitmapList.add(bitmap);
            }
            postInvalidate();
        }
        if (isReleased) {
            BZLogUtil.d(TAG, "Thread end isReleased so releaseBitmaps");
            releaseBitmaps();
        }
    }

    private native int clipVideoFrameForMultilInput(String[] inputs,
                                                    int layoutType,
                                                    int imageCount, int scale2Width);
}
