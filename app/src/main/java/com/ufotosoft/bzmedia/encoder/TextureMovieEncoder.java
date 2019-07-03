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

import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;

import com.ufotosoft.bzmedia.bean.RecordDrawInfo;
import com.ufotosoft.bzmedia.glutils.BaseProgram;
import com.ufotosoft.bzmedia.glutils.FrameBufferUtil;
import com.ufotosoft.bzmedia.glutils.RecordInfoQueue;
import com.ufotosoft.bzmedia.utils.BZLogUtil;
import com.ufotosoft.bzmedia.utils.BZSpUtils;

import java.lang.ref.WeakReference;

import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glFinish;
import static android.opengl.GLES20.glFlush;
import static android.opengl.GLES20.glViewport;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object
 * <li>create an EncoderConfig
 * <li>call TextureMovieEncoder#startRecording() with the config
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 * call TextureMovieEncoder#frameAvailable().
 * </ul>
 * <p>
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class TextureMovieEncoder implements Runnable {
    private static final String TAG = "bz_TextureMovieEncoder";
    private static final boolean VERBOSE = false;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private BaseProgram baseProgram;
    private BaseProgram mFullScreen;
    private BaseProgram baseProgramEncode = null;
    private VideoEncoderCore mVideoEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private final Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private OnVideoEncodeListener onVideoEncodeListener;
    private EncoderConfig encoderConfig = null;
    private OnVideoPacketAvailableListener onVideoPacketAvailableListener = null;
    private boolean isPrepareSuccess = false;
    private long mFrameNum = 0;
    private RecordInfoQueue willDrawInfoQueue = new RecordInfoQueue();
    //资源池
    private RecordInfoQueue drawInfoPond = new RecordInfoQueue();
    private int mTextureId = 0;

    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     * with reasonable defaults for those and bit rate.
     */
    public static class EncoderConfig {
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final EGLContext mEglContext;
        final boolean mNeedFlipVertical;

        public EncoderConfig(int width, int height, int bitRate,
                             EGLContext sharedEglContext, boolean needFlipVertical) {
            //对齐处理
            mWidth = width / 16 * 16;
            mHeight = height / 16 * 16;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
            mNeedFlipVertical = needFlipVertical;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    "' ctxt=" + mEglContext;
        }
    }

    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    public void startRecording(EncoderConfig config) {
        BZLogUtil.d(TAG, "Encoder: startRecording()");
        this.encoderConfig = config;
        synchronized (mReadyFence) {
            if (mRunning) {
                BZLogUtil.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureEncoderThread").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording() {
        if (null != mHandler) {
            mHandler.removeMessages(MSG_START_RECORDING);
            mHandler.removeMessages(MSG_STOP_RECORDING);
            mHandler.removeMessages(MSG_FRAME_AVAILABLE);
            mHandler.removeMessages(MSG_SET_TEXTURE_ID);
            mHandler.removeMessages(MSG_UPDATE_SHARED_CONTEXT);
            mHandler.removeMessages(MSG_QUIT);
        }
        //释放外部gl线程的东西
        if (null != baseProgram) {
            baseProgram.release();
            baseProgram = null;
        }
        willDrawInfoQueue.release();
        drawInfoPond.release();

        if (null != mHandler) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
        } else {
            handleStopRecording();
        }
        isPrepareSuccess = false;
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void frameAvailable() {
        if (!isPrepareSuccess) {
            return;
        }
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        long timestamp = System.nanoTime();
        if (timestamp == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            BZLogUtil.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
            return;
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp));
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     * <p>
     * TODO: do something less clumsy
     */
    public void setTextureId(int id) {
        if (!isPrepareSuccess || null == encoderConfig) {
            return;
        }
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        if (null == baseProgram) {
            baseProgram = new BaseProgram(false);
        }
        //5是缓存池大小
        drawInfoPond.release4Remain(5);
        int size = drawInfoPond.size();

        if (mFrameNum % 15 == 0) {
            BZLogUtil.v(TAG, "drawInfoPond size=" + size);
        }
        RecordDrawInfo recordDrawInfo;
        if (size > 0) {
            recordDrawInfo = drawInfoPond.get(0);
            recordDrawInfo.setTimeStamp(System.currentTimeMillis());
            drawInfoPond.remove(0);
//            BZLogUtil.v(TAG, "--重复利用 FrameBufferUtil--");
        } else {
//            BZLogUtil.v(TAG, "--新建 FrameBufferUtil--");
            FrameBufferUtil frameBufferUtil = new FrameBufferUtil(encoderConfig.mWidth, encoderConfig.mHeight);
            recordDrawInfo = new RecordDrawInfo(frameBufferUtil, System.currentTimeMillis());
        }

        FrameBufferUtil frameBufferUtil = recordDrawInfo.getFrameBufferUtil();
        frameBufferUtil.bindFrameBuffer();
        glClearColor(0, 0, 0, 0);
        glViewport(0, 0, encoderConfig.mWidth, encoderConfig.mHeight);
        baseProgram.draw(id);
        //不能放在外面 有的手机会卡死... 可能是由于当前纹理已经绘制到屏幕上了,再调用这两个就会有问题
        glFlush();
        glFinish();
        frameBufferUtil.unbindFrameBuffer();

        willDrawInfoQueue.add(recordDrawInfo);

        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        BZLogUtil.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
        BZLogUtil.d(TAG, "Encoder thread exiting end");
    }


    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                BZLogUtil.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable(timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_QUIT:
                    Looper looper = Looper.myLooper();
                    if (null != looper) {
                        looper.quit();
                    }
                    break;
                default:
                    BZLogUtil.e(TAG, "Unhandled msg what=" + what);
            }
        }
    }

    /**
     * Starts recording.
     */
    private void handleStartRecording(EncoderConfig config) {
        BZLogUtil.d(TAG, "handleStartRecording " + config);
        mTextureId = 0;
        mFrameNum = 0;
        isPrepareSuccess = prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate, config.mNeedFlipVertical);
        if (!isPrepareSuccess) {
            BZLogUtil.w(TAG, "prepareEncoder fail use_ffmpeg");
            BZSpUtils.put("use_ffmpeg_new", true);
        }
        if (null != onVideoEncodeListener) {
            onVideoEncodeListener.onPrepared(isPrepareSuccess);
        }
    }

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     *
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private void handleFrameAvailable(long timestampNanos) {
        int size = willDrawInfoQueue.size();
        if (size <= 0) {
            BZLogUtil.d("willDrawInfoQueue.isEmpty() return");
            return;
        }
        if (size > 5) {
            do {
                RecordDrawInfo recordDrawInfo = willDrawInfoQueue.get(0);
                if (null != recordDrawInfo) {
                    drawInfoPond.add(recordDrawInfo);
                }
                willDrawInfoQueue.remove(0);
                size = willDrawInfoQueue.size();
                BZLogUtil.w(TAG, "willDrawInfoQueue size=" + size + " remove");
            } while (size > 5);
        }

        long startTime = System.currentTimeMillis();

        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (null != encoderConfig) {
            glViewport(0, 0, encoderConfig.mWidth, encoderConfig.mHeight);
        }
        RecordDrawInfo recordDrawInfo = willDrawInfoQueue.get(0);
        if (null == recordDrawInfo) {
            return;
        }
        FrameBufferUtil frameBufferUtil = recordDrawInfo.getFrameBufferUtil();
        mFullScreen.draw(frameBufferUtil.getFrameBufferTextureID());

        long drawTime = System.currentTimeMillis() - startTime;
//        if (drawTime > 30) {
//            //防止消息阻塞
//            BZLogUtil.w(TAG, "drawTime > 30 removeMessages MSG_FRAME_AVAILABLE");
//            mHandler.removeMessages(MSG_FRAME_AVAILABLE);
//        }
        if (mFrameNum % 15 == 0) {
            BZLogUtil.v(TAG, "mFullScreen.draw 耗时=" + drawTime + " willDrawInfoQueue size=" + size);
        }
//        drawBox(mFrameNum);
//        if (mFrameNum < 4) {
//            mInputWindowSurface.saveFrame(new File("/sdcard/bzmedia/frame_" + mFrameNum + ".png"));
//        }
        //忽略掉第一帧脏数据
        if (mFrameNum > 0) {
            //drainEncoder 要放在 setPresentationTime 与 swapBuffers之前?
            long nanoTime = System.nanoTime();
            mInputWindowSurface.setPresentationTime(nanoTime);
            mVideoEncoder.drainEncoder(false, recordDrawInfo.getTimeStamp(), nanoTime);
            mInputWindowSurface.swapBuffers();
        }
        willDrawInfoQueue.remove(recordDrawInfo);
        drawInfoPond.add(recordDrawInfo);
        mFrameNum++;
    }

    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        BZLogUtil.d(TAG, "handleStopRecording");
        try {
            if (null != mVideoEncoder) {
                mVideoEncoder.drainEncoder(true, System.currentTimeMillis(), System.nanoTime());
            }
            releaseEncoder();
            if (null != onVideoEncodeListener) {
                onVideoEncodeListener.onStopped(true);
                onVideoEncodeListener = null;
            }
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private void handleSetTexture(int id) {
        //BZLogUtil.d(TAG, "handleSetTexture " + id);
        mTextureId = id;
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        mTextureId = 0;
        BZLogUtil.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        mFullScreen.release();
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // Create new programs and such for the new context.
        if (null != encoderConfig) {
            mFullScreen = new BaseProgram(encoderConfig.mNeedFlipVertical);
        } else {
            mFullScreen = new BaseProgram(true);
        }
    }

    private boolean prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
                                   boolean mNeedFlipVertical) {
        mVideoEncoder = new VideoEncoderCore(width, height, bitRate);
        mVideoEncoder.setOnVideoPacketAvailableListener(onVideoPacketAvailableListener);
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);


        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        baseProgramEncode = new BaseProgram(false);

        mFullScreen = new BaseProgram(mNeedFlipVertical);
        return mVideoEncoder.isPrepareSuccess();
    }

    private void releaseEncoder() {
        if (null != mVideoEncoder) {
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release();
            mFullScreen = null;
        }
        if (null != baseProgramEncode) {
            baseProgramEncode.release();
            baseProgramEncode = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    /**
     * Draws a box, with position offset.
     */
    private void drawBox(int posn) {
        final int width = mInputWindowSurface.getWidth();
        int xpos = (posn * 4) % (width - 50);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 0, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    public void setOnVideoEncodeListener(OnVideoEncodeListener onVideoEncodeListener) {
        this.onVideoEncodeListener = onVideoEncodeListener;
    }

    public void setOnVideoPacketAvailableListener(OnVideoPacketAvailableListener onVideoPacketAvailableListener) {
        this.onVideoPacketAvailableListener = onVideoPacketAvailableListener;
    }


}
