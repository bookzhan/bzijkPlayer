package com.ufotosoft.bzmedia.recorder;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.bean.VideoSize;
import com.ufotosoft.bzmedia.encoder.OnVideoEncodeListener;
import com.ufotosoft.bzmedia.encoder.OnVideoPacketAvailableListener;
import com.ufotosoft.bzmedia.encoder.TextureMovieEncoder;
import com.ufotosoft.bzmedia.utils.BZFileUtils;
import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.util.List;

/**
 * Created by luoye on 2017/8/20.
 * 基于纹理采集视频
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoRecorderMediaCodec extends VideoRecorderBase {
    public final static String TAG = "bz_VideoMediaCode";
    private long startTime;
    private long videoFrameCount;
    private TextureMovieEncoder textureMovieEncoder = null;
    private VideoRecorderNative audioRecorder = null;
    private volatile boolean videoRecordIsStop = false;
    private volatile boolean videoRecordIsStart = false;
    private volatile boolean audioRecordIsStop = false;
    private EGLContext lastEglContext = null;
    private long glThreadId;
    private long updateTextureIndex = 0;

    public VideoRecorderMediaCodec() {
        super();
    }

    public VideoRecorderMediaCodec(GLSurfaceView glSuerfaceView) {
        super(glSuerfaceView);
    }

    @Override
    public synchronized void startRecordInner(final String outputPath) {
        if (null == outputPath || null == glSuerfaceView) {
            BZLogUtil.e(TAG, "glSuerfaceView is null");
            return;
        }
        VideoSize fitVideoSize = VideoTacticsManager.getFitVideoSize(getRecordWidth(), getRecordHeight());
        setRecordWidth(fitVideoSize.getVideoWidth());
        setRecordHeight(fitVideoSize.getVideoHeight());
        setPreviewWidth(fitVideoSize.getVideoWidth());
        setPreviewHeight(fitVideoSize.getVideoHeight());

        //由于线程原因都在主线程中处理
        handler.post(new Runnable() {
            @Override
            public void run() {
                RecorderItem recorderItem = new RecorderItem();
                recorderItem.setVideoPath(outputPath);
                recorderItemList.add(recorderItem);
            }
        });
        videoFrameCount = 0;
        BZLogUtil.w(TAG, "startRecord");
        try {
            glSuerfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    try {
                        stopAll();
                        updateTextureIndex = 0;
                        textureMovieEncoder = new TextureMovieEncoder();
                        textureMovieEncoder.setOnVideoEncodeListener(new OnVideoEncodeListener() {
                            @Override
                            public void onPrepared(boolean success) {
                                if (null != onVideoRecorderStateListener) {
                                    onVideoRecorderStateListener.onVideoRecorderStarted(success);
                                }
                                videoRecordIsStart = true;
                            }

                            @Override
                            public void onStopped(final boolean success) {
                                BZLogUtil.d(TAG, "videoRecordIsStop");
                                synchronized (VideoRecorderMediaCodec.this) {
                                    videoRecordIsStop = true;
                                    if (audioRecordIsStop || !needAudio) {
                                        handleStopRecord();
                                    }
                                }
                                videoRecordIsStart = false;
                            }
                        });

                        glThreadId = Thread.currentThread().getId();
                        isFirstFrame = true;
                        EGLContext eglContext = EGL14.eglGetCurrentContext();
                        lastEglContext = eglContext;
                        if (needAudio) {
                            BZFileUtils.createNewFile(outputPath);
                            audioRecorder = new VideoRecorderNative();
                            startRecordAudio(outputPath);

                            textureMovieEncoder.setOnVideoPacketAvailableListener(new OnVideoPacketAvailableListener() {
                                @Override
                                public void onVideoPacketAvailable(byte[] videoPacket, long size, long pts) {
                                    if (null != audioRecorder)
                                        audioRecorder.addVideoPacketData(videoPacket, size, pts);
                                }
                            });
                            textureMovieEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                                    mRecordWidth, mRecordHeight, (int) getBitRate(),
                                    eglContext, needFlipVertical));
                            startTime = System.currentTimeMillis();
                        } else {
                            textureMovieEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(mRecordWidth, mRecordHeight, (int) getBitRate(),
                                    eglContext, needFlipVertical));
                            startTime = System.currentTimeMillis();
                            BZLogUtil.d(TAG, "outputPath=" + outputPath);
                        }

                        mRecording = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        BZLogUtil.e(TAG, e);
                    }
                }
            });
        } catch (Exception e) {
            BZLogUtil.e(TAG, e);
        }
    }

    private void startRecordAudio(final String outputPath) {
        audioRecorder.clearRecordItem();
        audioRecorder.setGLSuerfaceView(glSuerfaceView);
        audioRecorder.setPreviewWidth(getPreviewWidth());
        audioRecorder.setPreviewHeight(getPreviewHeight());
        audioRecorder.setRecordWidth(getRecordWidth());
        audioRecorder.setRecordHeight(getRecordHeight());
        audioRecorder.setVideoRotate(0);
        audioRecorder.setVideoRate(30);
        audioRecorder.setSynEncode(true);
        audioRecorder.setNeedFlipVertical(true);
        audioRecorder.setRecordPixelFormat(BZMedia.PixelFormat.YV12);
        audioRecorder.setNeedAudio(true);
        audioRecorder.setAllFrameIsKey(false);
        audioRecorder.setAvPacketFromMediaCodec(true);

        audioRecorder.setOnVideoRecorderStateListener(new OnVideoRecorderStateListener() {
            @Override
            public void onVideoRecorderStarted(boolean success) {
            }

            @Override
            public void onVideoRecorderStopped(List<RecorderItem> recorderItemList, final boolean success) {
                BZLogUtil.d(TAG, "audioRecordIsStop");
                if (null != audioRecorder)
                    audioRecorder.setOnVideoRecorderStateListener(null);
                synchronized (VideoRecorderMediaCodec.this) {
                    audioRecordIsStop = true;
                    if (videoRecordIsStop) {
                        handleStopRecord();
                    }
                }
            }
        });
        audioRecorder.setOnRecordPCMListener(new OnRecordPCMListener() {
            @Override
            public byte[] onRecordPCM(byte[] pcmData) {
                //要保证Video 录制准备好了,不然很难保证音视频同步,因为有的手机在准备的时候耗时比较长
                if (!videoRecordIsStart) {
                    return null;
                }
                if (!recorderItemList.isEmpty())
                    recorderItemList.get(recorderItemList.size() - 1).setVideoRecordTime(audioRecorder.getRecordTime());
                if (null != onRecordPCMListener) {
                    pcmData = onRecordPCMListener.onRecordPCM(pcmData);
                }
                return pcmData;
            }
        });
        audioRecorder.startRecordInner(outputPath);
    }

    @Override
    public void stopRecord() {
//        BZLogUtil.v(TAG, "stopRecord 主动调用 堆栈::" + Log.getStackTraceString(new Throwable()));
        BZLogUtil.v(TAG, "stopRecord 主动调用");
        if (Thread.currentThread().getId() == glThreadId) {
            BZLogUtil.d(TAG, "当前是GL线程 glThreadId=" + glThreadId);
            stopAll();
        } else {
            if (null == glSuerfaceView) {
                BZLogUtil.e(TAG, "stopRecord null == glSuerfaceView");
                stopAll();
                return;
            }
            glSuerfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    stopAll();
                }
            });
        }
    }

    private void stopAll() {
        try {
            mRecording = false;
            if (null != textureMovieEncoder) {
                textureMovieEncoder.stopRecording();
                textureMovieEncoder = null;
            }
            if (needAudio && null != audioRecorder) {
                audioRecorder.stopRecord();
                audioRecorder = null;
            }
            long time = System.currentTimeMillis() - startTime;
            BZLogUtil.e(TAG, "videoFrameCount=" + videoFrameCount + "--总录制时间=" + time + "--平均帧率=" + (videoFrameCount * 1000.f / time));
        } catch (Exception e) {
            BZLogUtil.e(TAG, e);
        }
    }

    @Override
    public void updateTexture(int textureId) {
        super.updateTexture(textureId);
        if (!mRecording) return;
//        if (isFirstFrame) {
//            isFirstFrame = false;
//            return;
//        }
//        updateTextureIndex++;
//        if (updateTextureIndex < 10) {
//            BZLogUtil.v(TAG, "ignore textureId=" + textureId);
//            return;
//        }

        if (!GLES20.glIsTexture(textureId)) {
            BZLogUtil.e(TAG, "updateTexture !glIsTexture textureId=" + textureId);
            return;
        }
        if (null != textureMovieEncoder) {
            EGLContext eglContext = EGL14.eglGetCurrentContext();
            if (null == eglContext) return;
            if (null == lastEglContext || !lastEglContext.equals(eglContext)) {
                lastEglContext = eglContext;
                BZLogUtil.e(TAG, "EglContext Changed updateRenderHandler");
                textureMovieEncoder.updateSharedContext(eglContext);
            }
            if (recordFromCamera) {
                long timeMillis = System.currentTimeMillis();
                //限制帧率
                if (timeMillis - lastUpdateTextureTime >= frameDuration) {
                    textureMovieEncoder.setTextureId(textureId);
                    textureMovieEncoder.frameAvailable();
                    //没有音频,这个时候用视频的时间去处理
                    if (!recorderItemList.isEmpty() && !needAudio)
                        recorderItemList.get(recorderItemList.size() - 1).setVideoRecordTime(System.currentTimeMillis() - startTime);
                    videoFrameCount++;
                    lastUpdateTextureTime = timeMillis;
                }
            } else {
                textureMovieEncoder.setTextureId(textureId);
                textureMovieEncoder.frameAvailable();
                //没有音频,这个时候用视频的时间去处理
                if (!recorderItemList.isEmpty() && !needAudio)
                    recorderItemList.get(recorderItemList.size() - 1).setVideoRecordTime(System.currentTimeMillis() - startTime);
                videoFrameCount++;
            }
        }
    }

    private void handleStopRecord() {
        BZLogUtil.w(TAG, "---handleStopRecord---");
        if (null != onVideoRecorderStateListener) {
            onVideoRecorderStateListener.onVideoRecorderStopped(recorderItemList, true);
        }
        videoRecordIsStop = false;
        audioRecordIsStop = false;
        glSuerfaceView = null;
    }
}
