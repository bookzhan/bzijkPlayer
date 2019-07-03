package com.ufotosoft.bzmedia.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.bean.ParticleEditInfo;
import com.ufotosoft.bzmedia.bean.VideoEditItem;
import com.ufotosoft.bzmedia.recorder.ParticleEditManager;
import com.ufotosoft.bzmedia.utils.BZBitmapUtil;
import com.ufotosoft.bzmedia.utils.BZDensityUtil;
import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhandalin on 2017-12-12 11:33.
 * 说明:展示视频 的SeekBar
 */

public class VideoSeekBar extends View {
    private static final String TAG = "bz_VideoSeekBar";
    protected boolean isReleased = false;
    protected int backgroundImageNum = 20;

    protected int bitmapRoundSize;
    protected int backgroundImagePadding = 2;
    protected int particleImagePadding = 6;
    private Rect bitmapDstRect = new Rect();
    private Rect bitmapSrcRect = new Rect();
    private RectF shadowRect = new RectF();

    protected List<Bitmap> bitmapList = new ArrayList<>();
    private Paint bitmapPaint = new Paint();
    private Paint shadowPaint = new Paint();
    private Paint barPaint = new Paint();
    private float barPosition = 0;
    private float barStrokeWidth = 4;//dp

    protected int bitmapRoundMinSize = 8;//px
    protected int bitmapRoundMaxSize = 20;//px

    private float lastProgress;

    //1dp
    private float dp_1 = 1;

    private long moveCount = 0;
    private OnSeekBarChangeListener onSeekBarChangeListener;
    private boolean needShadow;
    private ValueAnimator valueAnimator;

    private Bitmap finalBgBitmap = null;
    private String videoPath;
    private String lastVideoPath;
    protected int viewWidth, viewHeight;
    private ParticleEditManager particleEditManager;

    public VideoSeekBar(Context context) {
        this(context, null);
    }

    public VideoSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dp_1 = BZDensityUtil.dip2px(getContext(), dp_1);
        backgroundImagePadding = (int) (dp_1 * backgroundImagePadding);
        particleImagePadding = (int) (dp_1 * particleImagePadding);
        barStrokeWidth = dp_1 * barStrokeWidth;

        barPaint.setColor(Color.parseColor("#FFFF3A6F"));
        barPaint.setStrokeWidth(barStrokeWidth);
        shadowPaint.setColor(Color.parseColor("#7f191919"));
    }

    public void setParticleEditManager(ParticleEditManager particleEditManager) {
        this.particleEditManager = particleEditManager;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;
        if (viewWidth / backgroundImageNum * backgroundImageNum != viewWidth) {
            post(new Runnable() {
                @Override
                public void run() {
                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    layoutParams.width = viewWidth / backgroundImageNum * backgroundImageNum;
                    setLayoutParams(layoutParams);
                }
            });
        }
        if (w > 0 && h > 0 && null != videoPath) {
            initVideoThumbnail(videoPath);
        }
    }

    public void init(String videoPath) {
        this.videoPath = videoPath;
    }


    protected void initVideoThumbnail(final String videoPath) {
        BZLogUtil.d(TAG, "init videoPath=" + videoPath);
        if (null != lastVideoPath && lastVideoPath.equals(videoPath)) {
            BZLogUtil.w(TAG, "init getBitmapFromVideoThreadIsRuning lastVideoPath==videoPath");
            return;
        }
        lastVideoPath = videoPath;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int bitmapHeight = viewHeight - particleImagePadding;
                final int bitmapWidth = viewWidth / backgroundImageNum;

                long startTime = System.currentTimeMillis();
                //bitmapWidth * 4 防止GL处理后精度丢失太多
                BZMedia.getBitmapFromVideo(videoPath, backgroundImageNum, bitmapWidth * 4, new BZMedia.OnGetBitmapFromVideoListener() {
                    @Override
                    public void onGetBitmapFromVideo(int index, Bitmap bitmapSrc) {
                        //保留宽
//                        Bitmap scaleBitmap = BZBitmapUtil.scaleBitmap(bitmapSrc, 1.0f * bitmapWidth / bitmapSrc.getHeight());
                        bitmapRoundSize = bitmapSrc.getWidth() / 10;
                        if (bitmapRoundSize < bitmapRoundMinSize)
                            bitmapRoundSize = bitmapRoundMinSize;
                        if (bitmapRoundSize > bitmapRoundMaxSize)
                            bitmapRoundSize = bitmapRoundMaxSize;

                        Bitmap bitmap = BZBitmapUtil.centerCutBitmap(bitmapSrc, bitmapWidth, bitmapHeight);
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
                    }
                });
                BZLogUtil.d(TAG, "getBitmapFromVideo 耗时=" + (System.currentTimeMillis() - startTime));
                if (isReleased) {
                    BZLogUtil.d(TAG, "Thread end isReleased so releaseBitmaps");
                    releaseBitmaps();
                }
            }
        }).start();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        //画最终的背景图片
        int imageWidth = getWidth() / backgroundImageNum;
        for (int i = 0; i < bitmapList.size(); i++) {
            Bitmap bitmap = bitmapList.get(i);
            bitmapSrcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            bitmapDstRect.set(imageWidth * i, backgroundImagePadding, (i + 1) * imageWidth, getHeight() - backgroundImagePadding);
            canvas.drawBitmap(bitmap, bitmapSrcRect, bitmapDstRect, bitmapPaint);
        }
        if (null == particleEditManager) {
            return;
        }

        //draw 粒子icon
        List<ParticleEditInfo> itemList = particleEditManager.getParticleEditInfoItemList();
        int particleIconWidth = getHeight() - 2 * particleImagePadding;

        for (int i = 0; i < itemList.size(); i++) {
            VideoEditItem infoItem = itemList.get(i).getVideoEditItem();
            Bitmap finalDrawBitmap;
            if (i == itemList.size() - 1 && needShadow) {
                shadowRect.set(0, backgroundImagePadding, getWidth(), getHeight() - backgroundImagePadding);
                canvas.drawRoundRect(shadowRect, bitmapRoundSize, bitmapRoundSize, shadowPaint);
                finalDrawBitmap = infoItem.getParticleIconSelected();
            } else {
                finalDrawBitmap = infoItem.getParticleIconNormal();
            }
            if (null != finalDrawBitmap && !finalDrawBitmap.isRecycled()) {
                float startPosition = infoItem.getStartPosition();
                bitmapSrcRect.set(0, 0, finalDrawBitmap.getWidth(), finalDrawBitmap.getHeight());
                float particleTotalLen = getWidth() * infoItem.getEndPosition() - getWidth() * startPosition;
                if (particleTotalLen < 0) continue;

                //画开始不足整数个的部分
                int index = (int) (getWidth() * startPosition / particleIconWidth);
                //开始不足的部分最大的长度
                int maxLen = (int) (particleIconWidth * (index + 1) - getWidth() * startPosition);
                //实际需要绘制的长度
                int startDrawLent = (int) particleTotalLen;
                if (startDrawLent > maxLen)
                    startDrawLent = maxLen;

                if (getWidth() * startPosition / particleIconWidth != 0) {
                    int start = particleIconWidth - maxLen;

                    int bitmapLeft = (int) (1.0f * start / particleIconWidth * finalDrawBitmap.getWidth());
                    int bitmapDrawLen = (int) (1.0f * startDrawLent / particleIconWidth * finalDrawBitmap.getWidth());

                    bitmapSrcRect.set(bitmapLeft, 0, bitmapLeft + bitmapDrawLen, finalDrawBitmap.getHeight());
                    int left = (int) (getWidth() * startPosition);

                    bitmapDstRect.set(left, particleImagePadding, left + startDrawLent, getHeight() - particleImagePadding);
                    canvas.drawBitmap(finalDrawBitmap, bitmapSrcRect, bitmapDstRect, bitmapPaint);

                    //减去开始的部分
                    particleTotalLen -= startDrawLent;
                } else {
                    //开启的位置就是整数个图片, 那么置为0
                    startDrawLent = 0;
                }

                //画整数个
                int count = (int) (particleTotalLen / particleIconWidth);
                for (int j = 0; j < count; j++) {
                    bitmapSrcRect.set(0, 0, finalDrawBitmap.getWidth(), finalDrawBitmap.getHeight());
                    int left = (int) (getWidth() * startPosition + startDrawLent + j * particleIconWidth);
                    bitmapDstRect.set(left, particleImagePadding, left + particleIconWidth, getHeight() - particleImagePadding);
                    canvas.drawBitmap(finalDrawBitmap, bitmapSrcRect, bitmapDstRect, bitmapPaint);
                }
                //画余下的
                int lenTemp = (int) (particleTotalLen % particleIconWidth);
                int left = (int) (getWidth() * startPosition + startDrawLent + count * particleIconWidth);

                bitmapSrcRect.set(0, 0, (int) (1.0f * lenTemp / particleIconWidth * finalDrawBitmap.getWidth()), finalDrawBitmap.getHeight());
                bitmapDstRect.set(left, particleImagePadding, left + lenTemp, getHeight() - particleImagePadding);
                canvas.drawBitmap(finalDrawBitmap, bitmapSrcRect, bitmapDstRect, bitmapPaint);
            }

        }

        //draw bar
        canvas.drawLine(barPosition, 0, barPosition, getHeight(), barPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        float eventX = event.getX();
        if (eventX < 0) {
            eventX = 0;
        }
        if (eventX > getWidth()) {
            eventX = getWidth();
        }
        barPosition = eventX;
        if (null != onSeekBarChangeListener)
            onSeekBarChangeListener.onProgressChanged(barPosition / getWidth(), true);

        if (barPosition <= barStrokeWidth / 2) {
            barPosition = barStrokeWidth / 2;
        }
        if (barPosition >= getWidth() - barStrokeWidth / 2) {
            barPosition = getWidth() - barStrokeWidth / 2;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (null != onSeekBarChangeListener)
                    onSeekBarChangeListener.onStartTrackingTouch();
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                //减少重绘次数
                if (moveCount % 2 == 0) {
                    invalidate();
                }
                moveCount++;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (null != onSeekBarChangeListener)
                    onSeekBarChangeListener.onStopTrackingTouch();
                invalidate();
                break;

        }
        super.onTouchEvent(event);
        return true;
    }

    public void setProgress(float progress) {
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;
        if (null != onSeekBarChangeListener)
            onSeekBarChangeListener.onProgressChanged(progress, false);

        barPosition = progress * getWidth();
        if (barPosition <= barStrokeWidth / 2) {
            barPosition = barStrokeWidth / 2;
        }
        if (barPosition >= getWidth() - barStrokeWidth / 2) {
            barPosition = getWidth() - barStrokeWidth / 2;
        }
        invalidate();

//        if (progress > lastProgress) {
//            updateProgress(progress);
//        } else {
//            if (null != valueAnimator)
//                valueAnimator.cancel();
//            barPosition = progress * getWidth();
//            if (barPosition <= barStrokeWidth / 2) {
//                barPosition = barStrokeWidth / 2;
//            }
//            if (barPosition >= getWidth() - barStrokeWidth / 2) {
//                barPosition = getWidth() - barStrokeWidth / 2;
//            }
//            invalidate();
//        }
    }

    public void updateProgress(final float progress) {
        if (null != valueAnimator)
            valueAnimator.cancel();
        valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();
                float mProgress = lastProgress + (progress - lastProgress) * animatedValue;
                barPosition = mProgress * getWidth();
                if (barPosition <= barStrokeWidth / 2) {
                    barPosition = barStrokeWidth / 2;
                }
                if (barPosition >= getWidth() - barStrokeWidth / 2) {
                    barPosition = getWidth() - barStrokeWidth / 2;
                }
                invalidate();
//                MHLogUtil.d(TAG, "onAnimationUpdate---=" + animatedValue + "--mProgress=" + mProgress + "---lastProgress=" + lastProgress);
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
//                MHLogUtil.d(TAG, "onAnimationEnd");
                lastProgress = progress;
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
//                MHLogUtil.d(TAG, "onAnimationCancel");
                lastProgress = progress;
                invalidate();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();
    }

    public void setBackgroundImageNum(int backgroundImageNum) {
        this.backgroundImageNum = backgroundImageNum;
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener onSeekBarChangeListener) {
        this.onSeekBarChangeListener = onSeekBarChangeListener;
    }

    public void release() {
        BZLogUtil.d(TAG, "release");
        releaseBitmaps();
        lastVideoPath = null;
    }

    protected synchronized void releaseBitmaps() {
        BZLogUtil.d(TAG, "releaseBitmaps ");
        for (int i = 0; i < bitmapList.size(); i++) {
            Bitmap bitmap = bitmapList.get(i);
            if (!bitmap.isRecycled())
                bitmap.recycle();
        }
        synchronized (this) {
            bitmapList.clear();
        }
        BZLogUtil.d(TAG, "releaseBitmaps finish");
    }

    public void setNeedShadow(boolean needShadow) {
        this.needShadow = needShadow;
    }

    /**
     * 根据原图添加圆角,只是左右两边的圆角
     */
    protected static Bitmap createRoundImage(Bitmap source, int roundSize, boolean isLeft) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap target = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        RectF rect = new RectF();
        if (isLeft) {
            rect.set(0, 0, source.getWidth() + roundSize * 2, source.getHeight());
        } else {
            rect.set(-roundSize * 2, 0, source.getWidth(), source.getHeight());
        }

        canvas.drawRoundRect(rect, roundSize, roundSize, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);
        return target;
    }

    public interface OnSeekBarChangeListener {

        void onProgressChanged(float progress, boolean fromUser);


        void onStartTrackingTouch();


        void onStopTrackingTouch();
    }
}
