package com.ufotosoft.bzmedia.bean;

import com.ufotosoft.bzmedia.videomagic.VideoMagicUtil;

/**
 * Created by zhandalin on 2018-04-18 17:50.
 * 说明:
 */
public class MagicVideoEditInfo extends VideoEditInfo{
    private VideoMagicUtil.VideoMagicType videoMagicType=VideoMagicUtil.VideoMagicType.GHOST;

    public VideoMagicUtil.VideoMagicType getVideoMagicType() {
        return videoMagicType;
    }

    public void setVideoMagicType(VideoMagicUtil.VideoMagicType videoMagicType) {
        this.videoMagicType = videoMagicType;
    }
}
