package com.ufotosoft.bzmedia.bean;

/**
 * Created by luoye on 2017/6/18.
 * 录制参数封装
 */
public class VideoRecordParams {
    /**
     * output_path      视频输出路径
     * srcWidth         视频原始的宽,注意是否是倒置的
     * srcHeight        视频原始的高
     * targetWidth      最终视频输出的宽
     * targetHeight     最终视频输出的高
     * videoRate        视频帧率
     * nbSamples        每一个音频帧有多少采样
     * sampleRate       音频的采样率
     * videoRotate      视频旋转角度
     * extraFilterParam 额外的滤镜参数,保留参数,可以传ffmpeg支持的参数
     * pixelFormat      录制的像素格式
     * hasAudio         是否有音频
     */
    private String output_path;
    private int srcWidth;
    private int srcHeight;
    private int targetWidth;
    private int targetHeight;
    private int videoRate;
    private int nbSamples;
    private int sampleRate;
    private int videoRotate;
    private String extraFilterParam;
    private int pixelFormat;
    private boolean hasAudio = true;
    private boolean needFlipVertical = false;
    //视频是否全是关键帧
    private boolean allFrameIsKey = false;
    private long bitRate;

    //视频是否同步编码
    private boolean synEncode = false;

    private boolean avPacketFromMediaCodec = false;

    public String getOutput_path() {
        return output_path;
    }

    public void setOutput_path(String output_path) {
        this.output_path = output_path;
    }

    public int getSrcWidth() {
        return srcWidth;
    }

    public void setSrcWidth(int srcWidth) {
        this.srcWidth = srcWidth;
    }

    public int getSrcHeight() {
        return srcHeight;
    }

    public void setSrcHeight(int srcHeight) {
        this.srcHeight = srcHeight;
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

    public int getVideoRate() {
        return videoRate;
    }

    public void setVideoRate(int videoRate) {
        this.videoRate = videoRate;
    }

    public int getNbSamples() {
        return nbSamples;
    }

    public void setNbSamples(int nbSamples) {
        this.nbSamples = nbSamples;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getVideoRotate() {
        return videoRotate;
    }

    public void setVideoRotate(int videoRotate) {
        this.videoRotate = videoRotate;
    }

    public String getExtraFilterParam() {
        return extraFilterParam;
    }

    public void setExtraFilterParam(String extraFilterParam) {
        this.extraFilterParam = extraFilterParam;
    }

    public int getPixelFormat() {
        return pixelFormat;
    }

    public void setPixelFormat(int pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    public boolean isHasAudio() {
        return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public boolean isNeedFlipVertical() {
        return needFlipVertical;
    }

    public void setNeedFlipVertical(boolean needFlipVertical) {
        this.needFlipVertical = needFlipVertical;
    }

    public boolean isAllFrameIsKey() {
        return allFrameIsKey;
    }

    public void setAllFrameIsKey(boolean allFrameIsKey) {
        this.allFrameIsKey = allFrameIsKey;
    }

    public long getBitRate() {
        return bitRate;
    }

    public void setBitRate(long bitRate) {
        this.bitRate = bitRate;
    }

    public boolean isSynEncode() {
        return synEncode;
    }

    public void setSynEncode(boolean synEncode) {
        this.synEncode = synEncode;
    }

    public boolean isAvPacketFromMediaCodec() {
        return avPacketFromMediaCodec;
    }

    public void setAvPacketFromMediaCodec(boolean avPacketFromMediaCodec) {
        this.avPacketFromMediaCodec = avPacketFromMediaCodec;
    }
}
