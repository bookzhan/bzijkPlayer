package com.ufotosoft.bzmedia.recorder;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.bean.VideoRecordParams;
import com.ufotosoft.bzmedia.bean.VideoSize;
import com.ufotosoft.bzmedia.glutils.BaseProgram;
import com.ufotosoft.bzmedia.glutils.FrameBufferUtil;
import com.ufotosoft.bzmedia.utils.BZLogUtil;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static com.ufotosoft.bzmedia.recorder.OnRecorderErrorListener.ERROR_UNKNOWN;


/**
 * Created by zhandalin on 2017-04-21 16:49.
 * 说明:
 */
public class VideoRecorderNative extends VideoRecorderBase implements AudioCapture.OnAudioFrameCapturedListener {
    public final static String TAG = "bz_VideoRecorderNative";
    private long glThreadId = 0;
    private long nativeHandle = 0;
    private BaseProgram baseProgram;
    private FrameBufferUtil frameBufferUtil;

    public VideoRecorderNative() {
        super();
    }

    public VideoRecorderNative(GLSurfaceView glSuerfaceView) {
        super(glSuerfaceView);
    }

    /**
     * 声音录制
     */
    private AudioCapture mAudioRecorder;


    @Override
    public void startRecord(final String outputPath) {
        if (pixelFormat == BZMedia.PixelFormat.TEXTURE) {
            if (null != glSuerfaceView) {
                glSuerfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        glThreadId = Thread.currentThread().getId();
                        startRecordInner(outputPath);
                    }
                });
            } else {
                BZLogUtil.w(TAG, "please set glSuerfaceView");
                startRecordInner(outputPath);
            }
        } else {
            startRecordInner(outputPath);
        }
    }

    public void startRecordInner(String outputPath) {
        BZLogUtil.d(TAG, "startRecord outputPath=" + outputPath);
        final RecorderItem recorderItem = new RecorderItem();
        recorderItem.setVideoPath(outputPath);
        handler.post(new Runnable() {
            @Override
            public void run() {
                recorderItemList.add(recorderItem);
            }
        });


        String extraFilterParam = null;
        if (isFrontCamera() && isNexusPhone()) {
            extraFilterParam = "vflip";
        } else if (isFrontCamera()) {
            extraFilterParam = "vflip,rotate=PI";
        }
        VideoSize fitVideoSize;
        if (recordFromCamera) {
            fitVideoSize = VideoTacticsManager.getFitVideoSize(mRecordWidth, mRecordHeight);
        } else {
            fitVideoSize = new VideoSize(mRecordWidth, mRecordHeight);
        }
        VideoRecordParams videoRecordParams = new VideoRecordParams();
        videoRecordParams.setOutput_path(outputPath);
        if (pixelFormat == BZMedia.PixelFormat.NV21 || pixelFormat == BZMedia.PixelFormat.YV12) {
            videoRecordParams.setSrcWidth(getPreviewWidth());
            videoRecordParams.setSrcHeight(getPreviewHeight());
        } else {
            videoRecordParams.setSrcWidth(fitVideoSize.width);
            videoRecordParams.setSrcHeight(fitVideoSize.height);
        }
        videoRecordParams.setTargetWidth(fitVideoSize.width);
        videoRecordParams.setTargetHeight(fitVideoSize.height);
        videoRecordParams.setVideoRate(mFrameRate);
        videoRecordParams.setNbSamples(AudioCapture.getNbSamples());
        videoRecordParams.setSampleRate(44100);
        videoRecordParams.setVideoRotate(videoRotate);
        videoRecordParams.setExtraFilterParam(extraFilterParam);
        videoRecordParams.setPixelFormat(pixelFormat.ordinal());
        videoRecordParams.setHasAudio(needAudio);
        videoRecordParams.setNeedFlipVertical(needFlipVertical);
        videoRecordParams.setAllFrameIsKey(allFrameIsKey);
        videoRecordParams.setBitRate(getBitRate());
        videoRecordParams.setSynEncode(synEncode);
        videoRecordParams.setAvPacketFromMediaCodec(avPacketFromMediaCodec);

        isFirstFrame = true;
        nativeHandle = BZMedia.startRecord(videoRecordParams);
        if (nativeHandle < 0) {
            mRecording = false;
            if (null != mOnRecorderErrorListener)
                mOnRecorderErrorListener.onVideoError(ERROR_UNKNOWN, ERROR_UNKNOWN);

            if (null != onVideoRecorderStateListener)
                onVideoRecorderStateListener.onVideoRecorderStarted(false);
            BZLogUtil.d(TAG, "startRecord fail");
        } else {
            BZLogUtil.d(TAG, "Record start success");
            if (null != onVideoRecorderStateListener)
                onVideoRecorderStateListener.onVideoRecorderStarted(true);
        }
        if (needAudio) {
            if (null != mAudioRecorder)
                mAudioRecorder.stopCapture();
            mAudioRecorder = new AudioCapture();
            mAudioRecorder.setOnAudioFrameCapturedListener(this);
            mAudioRecorder.startCapture();
        }
        mRecording = true;
    }

    /**
     * 数据回调
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mRecording) {
            int ret = BZMedia.addVideoData(nativeHandle, data);
            if (ret < 0) {
                BZLogUtil.d(TAG, "addVideoData fail");
            }
        }
        super.onPreviewFrame(data, camera);
    }

    public void addVideoData4Bitmap(Bitmap bitmap) {
        if (null == bitmap || bitmap.isRecycled() || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            BZLogUtil.e(TAG, "null==bitmap||bitmap.isRecycled()||bitmap.getWidth()<=0||bitmap.getHeight()<=0");
            return;
        }
        BZMedia.addVideoData4Bitmap(nativeHandle, bitmap, bitmap.getWidth(), bitmap.getHeight());
    }

    @Override
    public void onAudioFrameCaptured(byte[] audioData, int length) {
        if (!mRecording || null == audioData) return;
        if (null != onRecordPCMListener) {
            audioData = onRecordPCMListener.onRecordPCM(audioData);
        }
        if (null == audioData) {
            return;
        }
        long addAudioData = BZMedia.addAudioData(nativeHandle, audioData, length);
        if (addAudioData > 0 && !recorderItemList.isEmpty())
            recorderItemList.get(recorderItemList.size() - 1).setVideoRecordTime(addAudioData);
    }

    public void stopRecord() {
//        BZLogUtil.v(TAG, "stopRecord 主动调用 堆栈::" + Log.getStackTraceString(new Throwable()));
        BZLogUtil.v(TAG, "stopRecord 主动调用");
        if (null != mAudioRecorder) {
            mAudioRecorder.stopCapture();
            mAudioRecorder = null;
        }
        if (null != handler)
            handler.removeMessages(START_RECORD);

        if (!mRecording) return;

        if (pixelFormat == BZMedia.PixelFormat.TEXTURE) {
            if (null != glSuerfaceView) {
                if (glThreadId == Thread.currentThread().getId()) {
                    BZLogUtil.d(TAG, "当前是GL线程 glThreadId=" + glThreadId);
                    stopAll();
                } else {
                    glSuerfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            stopAll();
                        }
                    });
                }
            } else {
                BZLogUtil.w(TAG, "please set glSuerfaceView");
                BZMedia.setStopRecordFlag(nativeHandle);
                int ret = BZMedia.stopRecord(nativeHandle);
                nativeHandle = 0;
                mRecording = false;
                BZLogUtil.d(TAG, "stopRecord success");
                if (null != onVideoRecorderStateListener)
                    onVideoRecorderStateListener.onVideoRecorderStopped(recorderItemList, ret >= 0);
            }
        } else {
            BZMedia.stopRecord(nativeHandle);
            nativeHandle = 0;
            mRecording = false;
            if (null != onVideoRecorderStateListener)
                onVideoRecorderStateListener.onVideoRecorderStopped(recorderItemList, true);
        }
    }

    private void stopAll() {
        //由于开始启动子线程了,设置这个标记很重要,否则会崩溃
        if (null != frameBufferUtil) {
            frameBufferUtil.release();
            frameBufferUtil = null;
        }
        if (null != baseProgram) {
            baseProgram.release();
            baseProgram = null;
        }
        BZMedia.setStopRecordFlag(nativeHandle);
        if (synEncode) {
            int ret = BZMedia.stopRecord(nativeHandle);
            nativeHandle = 0;
            mRecording = false;
            BZLogUtil.d(TAG, "stopRecord success");
            if (null != onVideoRecorderStateListener) {
                onVideoRecorderStateListener.onVideoRecorderStopped(recorderItemList, ret >= 0);
            }
            glSuerfaceView = null;
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int ret = BZMedia.stopRecord(nativeHandle);
                    nativeHandle = 0;
                    mRecording = false;
                    BZLogUtil.d(TAG, "stopRecord success");
                    if (null != onVideoRecorderStateListener) {
                        onVideoRecorderStateListener.onVideoRecorderStopped(recorderItemList, ret >= 0);
                    }
                    glSuerfaceView = null;
                }
            }, "StopRecordThread").start();
        }
    }

    @Override
    public void updateTexture(int textureId) {
        super.updateTexture(textureId);
        if (isFirstFrame) {
            isFirstFrame = false;
            return;
        }
        if (needFlipVertical) {
            if (null == baseProgram) {
                baseProgram = new BaseProgram(true);
            }
            if (null == frameBufferUtil) {
                frameBufferUtil = new FrameBufferUtil(getRecordWidth(), getRecordHeight());
            }
            frameBufferUtil.bindFrameBuffer();
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            glViewport(0, 0, getRecordWidth(), getRecordHeight());
            baseProgram.draw(textureId);
            frameBufferUtil.unbindFrameBuffer();
            textureId = frameBufferUtil.getFrameBufferTextureID();
        }
//        BZMedia.updateVideoRecorderTexture(textureId);

        if (recordFromCamera) {
            long timeMillis = System.currentTimeMillis();
            if (timeMillis - lastUpdateTextureTime >= frameDuration) {
                BZMedia.updateVideoRecorderTexture(nativeHandle, textureId);
                lastUpdateTextureTime = timeMillis;
            }
        } else {
            BZMedia.updateVideoRecorderTexture(nativeHandle, textureId);
        }
    }

    public void addVideoPacketData(byte[] videoPacket, long size, long pts) {
        BZMedia.addVideoPacketData(nativeHandle, videoPacket, size, pts);
    }
}
