package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import com.ufotosoft.bzmedia.bean.BZColor;
import com.ufotosoft.bzmedia.bean.ViewPort;
import com.ufotosoft.bzmedia.glutils.BZOpenGlUtils;
import com.ufotosoft.bzmedia.glutils.BaseProgram;
import com.ufotosoft.bzmedia.glutils.FrameBufferUtil;
import com.ufotosoft.bzmedia.utils.BZBitmapUtil;
import com.ufotosoft.bzmedia.utils.BZLogUtil;
import com.ufotosoft.bzmedia.utils.BZScreenUtils;

import java.util.Locale;

import static android.opengl.GLES20.glDeleteTextures;

/**
 * Created by zhandalin on 2019-03-06 11:44.
 * 说明:
 */
public class BZGLImageView extends GLTextureView {
    private static final String TAG = "bz_BZGLImageView";
    private BZColor clearBackground = new BZColor(1, 1, 1, 1);
    private int viewWidth;
    private int viewHeight;
    private boolean mFitFullView = false;
    private boolean mIsUsingMask = false;
    protected float mMaskAspectRatio = 1.0f;
    private OnViewPortChangeListener onViewPortChangeListener;
    private ViewPort mDrawViewPort = new ViewPort();
    private Bitmap bitmap;
    private int textureId;
    private BaseProgram baseProgram = null;
    private BaseProgram baseProgramTarget = null;
    private boolean surfaceOnCreated = false;
    private OnDrawFrameListener onDrawFrameListener;
    private FrameBufferUtil frameBufferUtil;


    public BZGLImageView(Context context) {
        this(context, null);
    }

    public BZGLImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

//    public BZGLImageView(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        setRenderer(this);
//        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//    }

    public boolean ismFitFullView() {
        return mFitFullView;
    }

    public void setmFitFullView(boolean mFitFullView) {
        this.mFitFullView = mFitFullView;
    }

    public boolean ismIsUsingMask() {
        return mIsUsingMask;
    }

    public void setmIsUsingMask(boolean mIsUsingMask) {
        this.mIsUsingMask = mIsUsingMask;
    }

    public float getmMaskAspectRatio() {
        return mMaskAspectRatio;
    }

    public void setmMaskAspectRatio(float mMaskAspectRatio) {
        this.mMaskAspectRatio = mMaskAspectRatio;
    }

    public void setImageBitmap(final Bitmap bitmap) {
        this.bitmap = bitmap;
        if (null == bitmap) return;
        calcViewport();
        if (!surfaceOnCreated) {
            return;
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                textureId = BZOpenGlUtils.loadTexture(bitmap);
                requestRender();
            }
        });
    }

    public void setImagePath(String path) {
        Bitmap fitBitmap = BZBitmapUtil.getFitBitmap(getContext(), path, (int) (BZScreenUtils.getScreenHeight(getContext()) * 1.2));
        setImageBitmap(fitBitmap);
    }

//    @Override
//    public void onSurfaceCreated() {
//        surfaceOnCreated = true;
//        glClearColor(clearBackground.r, clearBackground.g, clearBackground.b, clearBackground.a);
//        baseProgram = new BaseProgram(true);
//
//        if (null != bitmap) {
//            textureId = BZOpenGlUtils.loadTexture(bitmap);
//            requestRender();
//        }
//    }
//
//    @Override
//    public void onSurfaceChanged(int width, int height) {
//        viewWidth = width;
//        viewHeight = height;
//        calcViewport();
//    }

    @Override
    public void onPause() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != frameBufferUtil) {
                    frameBufferUtil.release();
                    frameBufferUtil = null;
                }
                if (null != baseProgram) {
                    baseProgram.release();
                    baseProgram = null;
                }
                if (null != baseProgramTarget) {
                    baseProgramTarget.release();
                    baseProgramTarget = null;
                }
                if (textureId > 0) {
                    glDeleteTextures(1, new int[textureId], 0);
                    textureId = 0;
                }
            }
        });
        super.onPause();
    }

    private boolean calcViewport() {
        if (null == bitmap) return false;
        int videoWidth = bitmap.getWidth();
        int videoHeight = bitmap.getHeight();

        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return false;
        float scaling;
        if (mIsUsingMask) {
            scaling = mMaskAspectRatio;
        } else {
            scaling = videoWidth / (float) videoHeight;
        }

        float viewRatio = viewWidth / (float) viewHeight;
        float s = scaling / viewRatio;

        int w, h;

        if (mFitFullView) {
            //撑满全部view(内容大于view)
            if (s > 1.0) {
                w = (int) Math.ceil(viewHeight * scaling);
                h = viewHeight;
            } else {
                w = viewWidth;
                h = (int) Math.ceil(viewWidth / scaling);
            }
        } else {
            //显示全部内容(内容小于view)
            if (s > 1.0) {
                w = viewWidth;
                h = (int) Math.ceil(viewWidth / scaling);
            } else {
                h = viewHeight;
                w = (int) Math.ceil(viewHeight * scaling);
            }
        }
        //需要转换成2的整数倍
        mDrawViewPort.width = w;
        mDrawViewPort.height = h;
        mDrawViewPort.x = (int) Math.ceil((viewWidth - mDrawViewPort.width) / 2);
        mDrawViewPort.y = (int) Math.ceil((viewHeight - mDrawViewPort.height) / 2);

        if (null != onViewPortChangeListener)
            onViewPortChangeListener.onViewPortChange(mDrawViewPort);
        BZLogUtil.d(TAG, String.format(Locale.CHINESE, "View port: x=%d, y=%d, width=%d, height=%d", mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height));
        return true;
    }

//
//    @Override
//    public void onDrawFrame() {
//        glClearColor(clearBackground.r, clearBackground.g, clearBackground.b, clearBackground.a);
//        glClear(GL_COLOR_BUFFER_BIT);
//        glViewport(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);
//        if (null == bitmap) {
//            return;
//        }
//        if (null == baseProgram) {
//            baseProgram = new BaseProgram(true);
//        }
//        if (textureId <= 0) {
//            textureId = BZOpenGlUtils.loadTexture(bitmap);
//        }
//        //直接画屏幕
//        if (null == onDrawFrameListener) {
//            baseProgram.draw(textureId);
//        } else {
//            //需要对纹理进一步处理
//            if (null == frameBufferUtil) {
//                frameBufferUtil = new FrameBufferUtil(bitmap.getWidth(), bitmap.getHeight());
//            }
//            //释放掉重新new
//            if (bitmap.getWidth() != frameBufferUtil.getWidth() || bitmap.getHeight() != frameBufferUtil.getHeight()) {
//                frameBufferUtil.release();
//                frameBufferUtil = new FrameBufferUtil(bitmap.getWidth(), bitmap.getHeight());
//            }
//            //转换成普通纹理
//            frameBufferUtil.bindFrameBuffer();
//            glViewport(0, 0, bitmap.getWidth(), bitmap.getHeight());
//            baseProgram.draw(textureId);
//            frameBufferUtil.unbindFrameBuffer();
//
//            //画屏
//            int retTextureID = onDrawFrameListener.onDrawFrame(frameBufferUtil.getFrameBufferTextureID(), bitmap.getWidth(), bitmap.getHeight());
//            if (null == baseProgramTarget)
//                baseProgramTarget = new BaseProgram(false);
//
//            glViewport(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);
//            baseProgramTarget.draw(retTextureID);
//        }
//    }

    public void setClearBackground(BZColor bzColor) {
        if (null == bzColor) return;
        clearBackground.r = bzColor.r;
        clearBackground.g = bzColor.g;
        clearBackground.b = bzColor.b;
        clearBackground.a = bzColor.a;
    }


    public void setOnViewPortChangeListener(OnViewPortChangeListener onViewPortChangeListener) {
        this.onViewPortChangeListener = onViewPortChangeListener;
    }

    public void setOnDrawFrameListener(OnDrawFrameListener onDrawFrameListener) {
        this.onDrawFrameListener = onDrawFrameListener;
    }

    public interface OnViewPortChangeListener {
        void onViewPortChange(ViewPort viewport);
    }

    public interface OnDrawFrameListener {
        int onDrawFrame(int textureId, int textureWidth, int textureHeight);
    }

}
