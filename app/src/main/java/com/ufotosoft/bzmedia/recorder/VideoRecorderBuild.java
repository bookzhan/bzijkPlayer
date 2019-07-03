package com.ufotosoft.bzmedia.recorder;

/**
 * Created by zhandalin on 2017-08-24 19:42.
 * 说明:
 */
public class VideoRecorderBuild {
    public static VideoRecorderBase build() {
        if (VideoTacticsManager.isUseFFmpegRecorder()) {
            return new VideoRecorderNative();
        } else {
            return new VideoRecorderMediaCodec();
        }
    }
}
