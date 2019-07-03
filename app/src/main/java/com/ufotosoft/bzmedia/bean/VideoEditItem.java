package com.ufotosoft.bzmedia.bean;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

/**
 * Created by zhandalin on 2017-12-13 13:44.
 * 说明:记录粒子视频录制信息的类
 */

public class VideoEditItem implements Parcelable {
    private Bitmap particleIconNormal;
    private Bitmap particleIconSelected;

    private float startPosition;
    private float endPosition;

    private long audioEndPts;

    private long particleEngineHandle = 0;

    public Bitmap getParticleIconNormal() {
        return particleIconNormal;
    }

    public void setParticleIconNormal(Bitmap particleIconNormal) {
        this.particleIconNormal = particleIconNormal;
    }

    public Bitmap getParticleIconSelected() {
        return particleIconSelected;
    }

    public void setParticleIconSelected(Bitmap particleIconSelected) {
        this.particleIconSelected = particleIconSelected;
    }

    public float getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(float startPosition) {
        this.startPosition = startPosition;
    }

    public float getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(float endPosition) {
        //只能越来越大
        if (endPosition >= this.endPosition) {
            this.endPosition = endPosition;
        } else {
            BZLogUtil.e("endPosition< this.endPosition");
        }
    }

    public long getAudioEndPts() {
        return audioEndPts;
    }

    public void setAudioEndPts(long audioEndPts) {
        this.audioEndPts = audioEndPts;
    }

    public long getParticleEngineHandle() {
        return particleEngineHandle;
    }

    public void setParticleEngineHandle(long particleEngineHandle) {
        this.particleEngineHandle = particleEngineHandle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.startPosition);
        dest.writeFloat(this.endPosition);
        dest.writeLong(this.audioEndPts);
        dest.writeLong(this.particleEngineHandle);
    }

    public VideoEditItem() {
    }

    protected VideoEditItem(Parcel in) {
        this.startPosition = in.readFloat();
        this.endPosition = in.readFloat();
        this.audioEndPts = in.readLong();
        this.particleEngineHandle = in.readLong();
    }

    public static final Creator<VideoEditItem> CREATOR = new Creator<VideoEditItem>() {
        @Override
        public VideoEditItem createFromParcel(Parcel source) {
            return new VideoEditItem(source);
        }

        @Override
        public VideoEditItem[] newArray(int size) {
            return new VideoEditItem[size];
        }
    };
}
