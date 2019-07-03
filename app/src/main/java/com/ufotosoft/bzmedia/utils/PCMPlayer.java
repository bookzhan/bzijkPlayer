package com.ufotosoft.bzmedia.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Created by zhandalin on 2018-11-28 15:54.
 * 说明: call from jni
 */
public class PCMPlayer {
    private AudioTrack audioTrack = null;
    private String TAG = "bz_PCMPlayer";
    private boolean isRelease = false;
    private float volume = 1;

    private void setVideoPlayerVolume(float volume) {
        BZLogUtil.d(TAG, "setVideoPlayerVolume volume=" + volume + " --" + this);
        this.volume = volume;
        try {
            if (null != audioTrack)
                audioTrack.setStereoVolume(volume, volume);
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    /**
     * call from jni
     */
    protected void onPCMDataAvailable(byte[] pcmData, int length) {
//        BZLogUtil.d(TAG, "onPCMDataAvailable length=" + length);
        if (null == audioTrack && !isRelease) {
            try {
                int audioBufSize = AudioTrack.getMinBufferSize(44100,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        audioBufSize,
                        AudioTrack.MODE_STREAM);
                audioTrack.play();
                audioTrack.setStereoVolume(volume, volume);
            } catch (Exception e) {
                BZLogUtil.e(TAG, e);
            }
        }
        try {
            if (null != audioTrack) {
                audioTrack.write(pcmData, 0, length);
            }
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    private void pause() {
        try {
            if (null != audioTrack)
                audioTrack.pause();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    private void start() {
        try {
            if (null != audioTrack)
                audioTrack.play();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    private void stopAudioTrack() {
        BZLogUtil.d(TAG, "stopAudioTrack=" + this);
        isRelease = true;
        if (null != audioTrack) {
            try {
                audioTrack.flush();
                audioTrack.pause();
                audioTrack.stop();
                audioTrack.release();
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
        }
        audioTrack = null;
    }
}
