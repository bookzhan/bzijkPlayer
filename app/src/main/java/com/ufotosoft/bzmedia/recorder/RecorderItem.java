package com.ufotosoft.bzmedia.recorder;

import com.ufotosoft.bzmedia.bean.FilterInfo;

/**
 * Created by zhandalin on 2017-04-24 14:09.
 * 说明:记录录制的单个视频信息
 */
public class RecorderItem {
    private String videoPath;
    private long videoRecordTime;
    private boolean isSelected;
    private FilterInfo filterInfo;

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public long getVideoRecordTime() {
        return videoRecordTime;
    }

    public void setVideoRecordTime(long videoRecordTime) {
        this.videoRecordTime = videoRecordTime;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public FilterInfo getFilterInfo() {
        return filterInfo;
    }

    public void setFilterInfo(FilterInfo filterInfo) {
        this.filterInfo = filterInfo;
    }
}
