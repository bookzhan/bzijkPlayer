package com.ufotosoft.bzmedia.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

import com.ufotosoft.bzmedia.glutils.ExternalTextureProgram;
import com.ufotosoft.bzmedia.glutils.FrameBufferUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * Created by zhandalin on 2017-05-09 17:16.
 * 说明:
 */
public class BZNativeSurfaceView extends BZBaseGLSurfaceView {
    private final static String TAG = "bz_NativeSurfaceView";
    protected SurfaceTexture surfaceTexture;
    private int rotation = 0;
    private long nativeHandle = 0;
    private FrameBufferUtil frameBufferUtil = null;

    public BZNativeSurfaceView(Context context) {
        super(context);
    }

    public BZNativeSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(bzClearColor.r, bzClearColor.g, bzClearColor.b, bzClearColor.a);
        if (nativeHandle != 0) {
            ExternalTextureProgram.releaseGlResource(nativeHandle);
            nativeHandle = 0;
        }
        nativeHandle = ExternalTextureProgram.initNative(false);
        int mTextureID = ExternalTextureProgram.initGlResource(nativeHandle);
        //保证角度能回显回来
        if (rotation != 0) {
            setSrcRotation(rotation);
        }
        surfaceTexture = new SurfaceTexture(mTextureID);
        surfaceTexture.setOnFrameAvailableListener(BZNativeSurfaceView.this);
        if (null != surfaceCallback)
            surfaceCallback.surfaceCreated(surfaceTexture);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClearColor(bzClearColor.r, bzClearColor.g, bzClearColor.b, bzClearColor.a);
        glClear(GL_COLOR_BUFFER_BIT);

        if (surfaceTexture.getTimestamp() <= 0) {
            try {
                if (null != surfaceTexture)
                    surfaceTexture.updateTexImage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        glViewport(mDrawViewPort.x, mDrawViewPort.y, mDrawViewPort.width, mDrawViewPort.height);

        //这种画到buffer上
        if (null != onDrawFrameListener) {
            if (null == frameBufferUtil)
                frameBufferUtil = new FrameBufferUtil(videoWidth, videoHeight);
            //转换成普通纹理
            frameBufferUtil.bindFrameBuffer();
            glClearColor(bzClearColor.r, bzClearColor.g, bzClearColor.b, bzClearColor.a);
            glClear(GL_COLOR_BUFFER_BIT);
            glViewport(0, 0, videoWidth, videoHeight);
            ExternalTextureProgram.onDrawFrame(nativeHandle);
            frameBufferUtil.unbindFrameBuffer();
            onDrawFrameListener.onDrawFrame(frameBufferUtil.getFrameBufferTextureID());
        } else {
            ExternalTextureProgram.onDrawFrame(nativeHandle);
        }
        try {
            if (null != surfaceTexture)
                surfaceTexture.updateTexImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onPause() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != surfaceTexture) {
                    surfaceTexture.release();
                    surfaceTexture = null;
                }
                if (null != frameBufferUtil) {
                    frameBufferUtil.release();
                    frameBufferUtil = null;
                }
                ExternalTextureProgram.releaseGlResource(nativeHandle);
                nativeHandle = 0;
            }
        });
        super.onPause();
    }

    public void setVideoSize(final int videoWidth, final int videoHeight) {
        if (rotation == 90 || rotation == 270) {
            this.videoWidth = videoHeight;
            this.videoHeight = videoWidth;
        } else {
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (null != frameBufferUtil) {
                    frameBufferUtil.release();
                    frameBufferUtil = null;
                }
                calcViewport();
            }
        });
    }

    public void setImageSize(int videoWidth, int videoHeight) {
        setVideoSize(videoWidth, videoHeight);
    }

    /**
     * @param rotation 弧度值
     */
    public void setSrcRotation(final int rotation) {
        this.rotation = rotation;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                ExternalTextureProgram.setVideoRotation(nativeHandle, rotation);
                calcViewport();
            }
        });
    }

    @Override
    public void setFlip(final boolean needFlipHorizontal, final boolean needFlipVertical) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                ExternalTextureProgram.setFlip(nativeHandle, needFlipHorizontal, needFlipVertical);
            }
        });
    }

}