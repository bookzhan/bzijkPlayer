package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.ufotosoft.bzmedia.utils.BZDensityUtil;
import com.ufotosoft.bzmedia.utils.BZLogUtil;


public class BZGifView extends ImageView {
    private static final String TAG = "bz_BZGifView";

    private boolean isAutoPlay = true;
    private Paint roundRectPaint = new Paint();
    private Paint selectedRectPaint = new Paint();
    private Rect bitmapSrcRect = new Rect();
    private RectF roundRect = new RectF();
    private float roundSize = 4;
    private float selectedWidth = 3;
    private boolean enableRound = true;
    private AnimationDrawable gifAnimationDrawable;
    private Bitmap iconBitmap;

    /**
     * PowerImageView构造函数。
     *
     * @param context
     */
    public BZGifView(Context context) {
        this(context, null);
    }

    /**
     * PowerImageView构造函数。
     *
     * @param context
     */
    public BZGifView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * PowerImageView构造函数，在这里完成所有必要的初始化操作。
     *
     * @param context
     */
    public BZGifView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        float dp_1 = BZDensityUtil.dip2px(getContext(), 1);
        roundSize *= dp_1;
        selectedWidth *= dp_1;

        roundRectPaint.setAntiAlias(true);
        roundRectPaint.setStyle(Paint.Style.STROKE);
        roundRectPaint.setStrokeWidth(roundSize);
        roundRectPaint.setColor(Color.WHITE);


        selectedRectPaint.setAntiAlias(true);
        selectedRectPaint.setStyle(Paint.Style.STROKE);
        selectedRectPaint.setStrokeWidth(selectedWidth);
        selectedRectPaint.setColor(Color.parseColor("#FFFF3A6F"));
    }

    public void setGifPath(String path) {
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        try {
            boolean isRecycled = animationDrawableIsRecycled(gifAnimationDrawable);
            if (isRecycled) {
                setImageDrawable(null);
            }
            if (null != iconBitmap && !iconBitmap.isRecycled() && null == gifAnimationDrawable) {
                bitmapSrcRect.set(0, 0, iconBitmap.getWidth(), iconBitmap.getHeight());
                roundRect.set(0, 0, getWidth(), getHeight());
                canvas.drawBitmap(iconBitmap, bitmapSrcRect, roundRect, selectedRectPaint);
                roundRectPaint.setStrokeWidth(roundSize);
                canvas.drawRect(roundRect, roundRectPaint);
                canvas.drawRoundRect(roundRect, roundSize, roundSize, roundRectPaint);
            }
            // mMovie等于null，说明是张普通的图片，则直接调用父类的onDraw()方法
            super.onDraw(canvas);

            if (isRecycled) {
                return;
            }

            if (isAutoPlay) {
                if (!gifAnimationDrawable.isRunning()) {
                    gifAnimationDrawable.start();
                }
            } else {
                if (gifAnimationDrawable.isRunning()) {
                    gifAnimationDrawable.stop();
                }
            }

            if (enableRound && isSelected()) {
                roundRect.set(0, 0, getWidth(), getHeight());
                roundRectPaint.setStrokeWidth(roundSize + selectedWidth);
                canvas.drawRect(roundRect, roundRectPaint);

                roundRect.set(selectedWidth, selectedWidth, getWidth() - selectedWidth, getHeight() - selectedWidth);
                canvas.drawRoundRect(roundRect, roundSize, roundSize, selectedRectPaint);
            } else if (enableRound) {
                roundRect.set(0, 0, getWidth(), getHeight());
                roundRectPaint.setStrokeWidth(roundSize);
                canvas.drawRect(roundRect, roundRectPaint);
                canvas.drawRoundRect(roundRect, roundSize, roundSize, roundRectPaint);
            }
        } catch (Exception e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public void setRoundSize(float roundSize) {
        this.roundSize = roundSize;
    }

    public void enableRound(boolean enableRound) {
        this.enableRound = enableRound;
    }


    public void setAutoPlay(boolean autoPlay) {
        isAutoPlay = autoPlay;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.gifAnimationDrawable != null) {
            this.gifAnimationDrawable.stop();
        }
    }

    public void setIcon(Bitmap iconBitmap) {
        this.iconBitmap = iconBitmap;
        invalidate();
    }

    public void setGifAnimationDrawable(AnimationDrawable gifAnimationDrawable) {
        if (this.gifAnimationDrawable != null) {
            tryRecycleAnimationDrawable(this.gifAnimationDrawable);
            setImageDrawable(null);
            this.gifAnimationDrawable = null;
        }
        this.gifAnimationDrawable = gifAnimationDrawable;
        if (gifAnimationDrawable != null) {
            gifAnimationDrawable.setOneShot(false);
            setImageDrawable(gifAnimationDrawable);
            invalidate();
        }
    }

    private boolean animationDrawableIsRecycled(AnimationDrawable animationDrawable) {
        if (null == animationDrawable || animationDrawable.getNumberOfFrames() <= 0) {
            return true;
        }
        boolean isRecycled = false;
        for (int i = 0; i < animationDrawable.getNumberOfFrames(); i++) {
            Drawable frame = animationDrawable.getFrame(i);
            if (frame instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) frame).getBitmap();
                if (bitmap.isRecycled()) {
                    isRecycled = true;
                    break;
                }
            }
        }
        return isRecycled;
    }

    private static void tryRecycleAnimationDrawable(AnimationDrawable animationDrawable) {
        if (animationDrawable != null) {
            animationDrawable.stop();
            for (int i = 0; i < animationDrawable.getNumberOfFrames(); i++) {
                Drawable frame = animationDrawable.getFrame(i);
                if (frame instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) frame).getBitmap();
                    if (!bitmap.isRecycled())
                        bitmap.recycle();
                }
                frame.setCallback(null);

            }
            animationDrawable.setCallback(null);
        }
    }
}
