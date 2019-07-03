package com.ufotosoft.bzmedia.recorder;

import com.ufotosoft.bzmedia.bean.VideoRecordParams;

/**
 * Created by zhandalin on 2018-12-10 14:05.
 * 说明:底层类VideoRecorder 的上层Java类,方便其他库调用,而不用关心,是调用的GL2,还是GL3
 */
public class VideoRecorder {
    public native long initVideoRecorder();

    public native int startRecord(long nativeHandle, VideoRecordParams videoRecordParams);

    public native long addAudioData(long nativeHandle, byte[] data, int dataLength, long audioPts);

    public native int updateTexture(long nativeHandle, int textureId, long videoPts);

    public native int setStopRecordFlag(long nativeHandle);

    public native int stopRecord(long nativeHandle);

    public native void releaseRecorder(long nativeHandle);
}