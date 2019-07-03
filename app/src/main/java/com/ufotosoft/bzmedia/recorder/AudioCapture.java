package com.ufotosoft.bzmedia.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.nio.ByteBuffer;

/**
 * Created by zhandalin on 2017-03-29 14:04.
 * 说明:声音录制
 */
public class AudioCapture {

    private static final String TAG = "AudioCapture";

    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int SAMPLES_PER_FRAME = 1024;    // AAC, bytes/frame/channel
    private static final int FRAMES_PER_BUFFER = 30;    // AAC, frame/buffer/sec


    private Thread mCaptureThread;
    private static boolean mIsCaptureStarted = false;
    private volatile boolean mIsLoopExit = false;

    public static int getNbSamples() {
        return SAMPLES_PER_FRAME;
    }

    private OnAudioFrameCapturedListener mAudioFrameCapturedListener;

    public interface OnAudioFrameCapturedListener {
        void onAudioFrameCaptured(byte[] audioData, int length);

        /**
         * 音频错误
         *
         * @param what    错误类型
         * @param message 错误详细信息
         */
        void onAudioError(int what, String message);
    }

    public boolean isCaptureStarted() {
        return mIsCaptureStarted;
    }

    public void setOnAudioFrameCapturedListener(OnAudioFrameCapturedListener listener) {
        mAudioFrameCapturedListener = listener;
    }

    public synchronized boolean startCapture() {
        if (mIsCaptureStarted) {
            BZLogUtil.e(TAG, "Capture already started !");
            return false;
        }
        mIsLoopExit = false;
        mCaptureThread = new Thread(new AudioCaptureRunnable());
        mCaptureThread.start();
        mIsCaptureStarted = true;
        BZLogUtil.d(TAG, "Start audio capture success !");
        return true;
    }

    public synchronized void stopCapture() {
        if (!mIsCaptureStarted) {
            return;
        }
        mIsLoopExit = true;
        try {
//            mCaptureThread.interrupt();
            if (null != mCaptureThread) {
                mCaptureThread.join();
                mCaptureThread = null;
            }
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        mIsCaptureStarted = false;
        mAudioFrameCapturedListener = null;
    }

    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    private class AudioCaptureRunnable implements Runnable {
        private void releaseAudioRecord(AudioRecord audioRecord) {
            BZLogUtil.d(TAG, "releaseAudioRecord");
            if (null != audioRecord) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                } catch (Throwable ex) {
                    BZLogUtil.e(TAG, ex);
                }
            }
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            long startTime = System.currentTimeMillis();
            try {
                final int min_buffer_size = AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT) * 2;
                int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                if (buffer_size < min_buffer_size)
                    buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

                AudioRecord audioRecord = null;
                for (final int source : AUDIO_SOURCES) {
                    try {
                        audioRecord = new AudioRecord(
                                source, SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                            audioRecord = null;
                    } catch (final Exception e) {
                        releaseAudioRecord(audioRecord);
                        audioRecord = null;
                    }
                    if (audioRecord != null) break;
                }
                if (null == audioRecord) {
                    if (null != mAudioFrameCapturedListener)
                        mAudioFrameCapturedListener.onAudioError(OnRecorderErrorListener.RECORD_AUDIO_ERROR_CREATE_FAILED, "failed to initialize AudioRecord");
                    BZLogUtil.e(TAG, "failed to initialize AudioRecord");
                    return;
                }

                try {
                    int sleepCount = 0;
                    while (!mIsCaptureStarted) {
                        BZLogUtil.d(TAG, "!mIsCapturing sleep 10");
                        try {
                            Thread.sleep(10);
                        } catch (Exception e) {
                            BZLogUtil.e(TAG, e);
                        }
                        sleepCount++;
                        if (sleepCount > 100) {
                            BZLogUtil.e("!mIsCapturing sleep 2000 break");
                            break;
                        }
                    }
                    BZLogUtil.d(TAG, "AudioThread:start audio recording");
                    final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                    int readBytes;
                    try {
                        audioRecord.startRecording();
                    } catch (Exception e) {
                        if (null != mAudioFrameCapturedListener)
                            mAudioFrameCapturedListener.onAudioError(OnRecorderErrorListener.RECORD_AUDIO_ERROR_CREATE_FAILED, "startRecording failed.");
                        BZLogUtil.e(TAG, "startRecording failed.", e);
                        releaseAudioRecord(audioRecord);
                        audioRecord = null;
                        return;
                    }
                    try {
                        while (!mIsLoopExit) {
                            // read audio data from internal mic
                            buf.clear();
                            readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                            if (readBytes > 0) {
                                // set audio data to encoder
                                buf.position(readBytes);
                                buf.flip();
                                if (null != mAudioFrameCapturedListener) {
                                    byte[] bytesTemp = new byte[readBytes];
                                    buf.get(bytesTemp, 0, readBytes);
                                    mAudioFrameCapturedListener.onAudioFrameCaptured(bytesTemp, readBytes);
                                }
                            } else {
//                                BZLogUtil.e("readBytes <=0 reaResult=" + readBytes);
                            }
                        }
                    } catch (Throwable e) {
                        BZLogUtil.e(TAG, e);
                    }
                } finally {
                    releaseAudioRecord(audioRecord);
                }
            } catch (final Exception e) {
                BZLogUtil.e(TAG, "AudioThread#run", e);
            }
            BZLogUtil.e(TAG, "AudioThread:finished 录制时间=" + (System.currentTimeMillis() - startTime));
        }
    }

    public int getMinBufferSize() {
        return 0;
    }
}