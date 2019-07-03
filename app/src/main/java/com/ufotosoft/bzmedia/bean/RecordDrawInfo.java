package com.ufotosoft.bzmedia.bean;

import com.ufotosoft.bzmedia.glutils.FrameBufferUtil;

/**
 * Created by zhandalin on 2019-04-28 14:24.
 * 说明:
 */
public class RecordDrawInfo {
    private FrameBufferUtil frameBufferUtil;
    private long timeStamp;

    public RecordDrawInfo(FrameBufferUtil frameBufferUtil, long timeStamp) {
        this.frameBufferUtil = frameBufferUtil;
        this.timeStamp = timeStamp;
    }

    public FrameBufferUtil getFrameBufferUtil() {
        return frameBufferUtil;
    }

    public void setFrameBufferUtil(FrameBufferUtil frameBufferUtil) {
        this.frameBufferUtil = frameBufferUtil;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
