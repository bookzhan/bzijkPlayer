package com.ufotosoft.bzmedia.bean;

/**
 * Created by zhandalin on 2017-11-08 11:05.
 * 说明:
 */
public class GifAttribute {
    //单位秒
    private float startTime = 0;
    private float durationTime = 5.0f;//单位秒
    private float speed = 1.0f;//0~1;
    //帧率
    private int fps = 10;
    private int width = 240;
    private int height = 240;
    //使用这速度会变慢
    private boolean useHDGif = false;

    public float getStartTime() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
    }

    public float getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(float durationTime) {
        this.durationTime = durationTime;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isUseHDGif() {
        return useHDGif;
    }

    public void setUseHDGif(boolean useHDGif) {
        this.useHDGif = useHDGif;
    }
}
