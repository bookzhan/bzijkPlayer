package com.ufotosoft.bzmedia.bean;

/**
 * Created by zhandalin on 2018-01-12 10:01.
 * 说明:
 */

public class VideoTransCodeParams {
    private String inputPath;
    private String outputPath;
    private int gopSize = 30;

    //控制视频最大的宽,由此来控制分辨率
    private int maxWidth = -1;
    //控制开始的时间
    private long startTime = -1;

    //控制结束的时间
    private long endTime = -1;

    private boolean doWithVideo = true;
    private boolean needCallBackVideo = false;

    private boolean doWithAudio = false;

    //优先级最高
    private int targetWidth = -1;
    private int targetHeight = -1;

    //帧率控制
    private int frameRate = -1;

    //视频旋转角度,值支持0,90,180,270
    private int videoRotate = 0;

    private boolean userSoftDecode = false;

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public int getGopSize() {
        return gopSize;
    }

    public void setGopSize(int gopSize) {
        this.gopSize = gopSize;
    }

    public boolean isDoWithVideo() {
        return doWithVideo;
    }

    public void setDoWithVideo(boolean doWithVideo) {
        this.doWithVideo = doWithVideo;
    }

    public boolean isDoWithAudio() {
        return doWithAudio;
    }

    public void setDoWithAudio(boolean doWithAudio) {
        this.doWithAudio = doWithAudio;
    }

    public boolean isNeedCallBackVideo() {
        return needCallBackVideo;
    }

    public void setNeedCallBackVideo(boolean needCallBackVideo) {
        this.needCallBackVideo = needCallBackVideo;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public int getVideoRotate() {
        return videoRotate;
    }

    public void setVideoRotate(int videoRotate) {
        this.videoRotate = videoRotate;
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    public void setTargetHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    public boolean isUserSoftDecode() {
        return userSoftDecode;
    }

    public void setUserSoftDecode(boolean userSoftDecode) {
        this.userSoftDecode = userSoftDecode;
    }
}
