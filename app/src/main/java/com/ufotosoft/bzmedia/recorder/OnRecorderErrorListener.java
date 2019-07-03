package com.ufotosoft.bzmedia.recorder;

/**
 * Created by zhandalin on 2017-09-04 12:56.
 * 说明:
 */

public interface OnRecorderErrorListener {
    /**
     * 未知错误
     */
    int ERROR_UNKNOWN = 0x210;
    /**
     * 预览画布设置错误
     */
    int ERROR_CAMERA_SET_PREVIEW_DISPLAY = 0x211;
    /**
     * 预览错误
     */
    int ERROR_CAMERA_PREVIEW = 0x212;
    /**
     * 自动对焦错误
     */
    int ERROR_CAMERA_AUTO_FOCUS = 0x213;


    int RECORD_AUDIO_ERROR_UNKNOWN = 0x214;
    /**
     * 最小缓存获取失败
     */
    int RECORD_AUDIO__ERROR_GET_MIN_BUFFER_SIZE_NOT_SUPPORT = 0x215;
    /**
     * 创建AudioRecord失败
     */
    int RECORD_AUDIO_ERROR_CREATE_FAILED = 0x216;

    /**
     * 视频录制错误
     */
    void onVideoError(int what, int extra);

    /**
     * 音频录制错误
     */
    void onAudioError(int what, String message);
}
