package com.ufotosoft.bzmedia.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by zhandalin on 2018-12-28 10:20.
 * 说明:
 */
public class ParticleEditInfo implements Parcelable {
    private VideoEditItem videoEditItem = null;
    private ParticleBean particleBean = null;
    private long engineHandel = 0;

    public VideoEditItem getVideoEditItem() {
        return videoEditItem;
    }

    public void setVideoEditItem(VideoEditItem videoEditItem) {
        this.videoEditItem = videoEditItem;
    }

    public ParticleBean getParticleBean() {
        return particleBean;
    }

    public void setParticleBean(ParticleBean particleBean) {
        this.particleBean = particleBean;
    }

    public long getEngineHandel() {
        return engineHandel;
    }

    public void setEngineHandel(long engineHandel) {
        this.engineHandel = engineHandel;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.videoEditItem, flags);
        dest.writeParcelable(this.particleBean, flags);
        dest.writeLong(this.engineHandel);
    }

    public ParticleEditInfo() {
    }

    protected ParticleEditInfo(Parcel in) {
        this.videoEditItem = in.readParcelable(VideoEditItem.class.getClassLoader());
        this.particleBean = in.readParcelable(ParticleBean.class.getClassLoader());
        this.engineHandel = in.readLong();
    }

    public static final Creator<ParticleEditInfo> CREATOR = new Creator<ParticleEditInfo>() {
        @Override
        public ParticleEditInfo createFromParcel(Parcel source) {
            return new ParticleEditInfo(source);
        }

        @Override
        public ParticleEditInfo[] newArray(int size) {
            return new ParticleEditInfo[size];
        }
    };
}
