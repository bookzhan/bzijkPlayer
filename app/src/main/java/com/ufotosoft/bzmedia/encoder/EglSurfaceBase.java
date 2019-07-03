/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ufotosoft.bzmedia.encoder;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Common base class for EGL surfaces.
 * <p>
 * There can be multiple surfaces associated with a single context.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class EglSurfaceBase {
    protected static final String TAG = GlUtil.TAG;

    // EglCore object we're associated with.  It may be associated with multiple surfaces.
    protected EglCore mEglCore;

    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    protected int mWidth = -1;
    protected int mHeight = -1;

    protected EglSurfaceBase(EglCore eglCore) {
        mEglCore = eglCore;
    }

    /**
     * Creates a window surface.
     * <p>
     *
     * @param surface May be a Surface or SurfaceTexture.
     */
    public void createWindowSurface(Object surface) {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            BZLogUtil.e(TAG, "surface already created");
            return;
        }
        mEGLSurface = mEglCore.createWindowSurface(surface);

        // Don't cache width/height here, because the size of the underlying surface can change
        // out from under us (see e.g. HardwareScalerActivity).
        //mWidth = mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
        //mHeight = mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
    }

    /**
     * Creates an off-screen surface.
     */
    public void createOffscreenSurface(int width, int height) {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            BZLogUtil.e(TAG, "surface already created");
            return;
        }
        mEGLSurface = mEglCore.createOffscreenSurface(width, height);
        mWidth = width;
        mHeight = height;
    }

    /**
     * Returns the surface's width, in pixels.
     * <p>
     * If this is called on a window surface, and the underlying surface is in the process
     * of changing size, we may not see the new size right away (e.g. in the "surfaceChanged"
     * callback).  The size should match after the next buffer swap.
     */
    public int getWidth() {
        if (mWidth < 0) {
            return mEglCore.querySurface(mEGLSurface, EGL14.EGL_WIDTH);
        } else {
            return mWidth;
        }
    }

    /**
     * Returns the surface's height, in pixels.
     */
    public int getHeight() {
        if (mHeight < 0) {
            return mEglCore.querySurface(mEGLSurface, EGL14.EGL_HEIGHT);
        } else {
            return mHeight;
        }
    }

    /**
     * Release the EGL surface.
     */
    public void releaseEglSurface() {
        mEglCore.releaseSurface(mEGLSurface);
        mEGLSurface = EGL14.EGL_NO_SURFACE;
        mWidth = mHeight = -1;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        mEglCore.makeCurrent(mEGLSurface);
    }

    /**
     * Makes our EGL context and surface current for drawing, using the supplied surface
     * for reading.
     */
    public void makeCurrentReadFrom(EglSurfaceBase readSurface) {
        mEglCore.makeCurrent(mEGLSurface, readSurface.mEGLSurface);
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    public boolean swapBuffers() {
        boolean result = mEglCore.swapBuffers(mEGLSurface);
        if (!result) {
            BZLogUtil.d(TAG, "WARNING: swapBuffers() failed");
        }
        return result;
    }

    /**
     * Sends the presentation time stamp to EGL.
     *
     * @param nsecs Timestamp, in nanoseconds.
     */
    public void setPresentationTime(long nsecs) {
        mEglCore.setPresentationTime(mEGLSurface, nsecs);
    }

    /**
     * Saves the EGL surface to a file.
     * <p>
     * Expects that this object's EGL surface is current.
     */
    public void saveFrame(File file) {
        if (!mEglCore.isCurrent(mEGLSurface)) {
            BZLogUtil.e(TAG, "Expected EGL context/surface is not current");
            return;
        }

        // glReadPixels fills in a "direct" ByteBuffer with what is essentially big-endian RGBA
        // data (i.e. a byte of red, followed by a byte of green...).  While the Bitmap
        // constructor that takes an int[] wants little-endian ARGB (blue/red swapped), the
        // Bitmap "copy pixels" method wants the same format GL provides.
        //
        // Ideally we'd have some way to re-use the ByteBuffer, especially if we're calling
        // here often.
        //
        // Making this even more interesting is the upside-down nature of GL, which means
        // our output will look upside down relative to what appears on screen if the
        // typical GL conventions are used.

        try {
            String filename = file.toString();

            int width = getWidth();
            int height = getHeight();
            ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            GLES20.glReadPixels(0, 0, width, height,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
            GlUtil.checkGlError("glReadPixels");
            buf.rewind();

            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(filename));
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buf);
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bmp.recycle();
            } finally {
                if (bos != null) bos.close();
            }
            BZLogUtil.d(TAG, "Saved " + width + "x" + height + " frame as '" + filename + "'");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
