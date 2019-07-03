package com.ufotosoft.bzmedia.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhandalin on 2017-10-30 11:20.
 * 说明:
 */
public class ParticleBean implements Parcelable {
    private String name;
    private int particleID;
    private List<ParticleAttribute> particleAttribute;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getParticleID() {
        return particleID;
    }

    public void setParticleID(int particleID) {
        this.particleID = particleID;
    }

    public List<ParticleAttribute> getParticleAttribute() {
        return particleAttribute;
    }

    public void setParticleAttribute(List<ParticleAttribute> particleAttribute) {
        this.particleAttribute = particleAttribute;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.particleID);
        dest.writeList(this.particleAttribute);
    }

    public ParticleBean() {
    }

    protected ParticleBean(Parcel in) {
        this.name = in.readString();
        this.particleID = in.readInt();
        this.particleAttribute = new ArrayList<ParticleAttribute>();
        in.readList(this.particleAttribute, ParticleAttribute.class.getClassLoader());
    }

    public static final Creator<ParticleBean> CREATOR = new Creator<ParticleBean>() {
        @Override
        public ParticleBean createFromParcel(Parcel source) {
            return new ParticleBean(source);
        }

        @Override
        public ParticleBean[] newArray(int size) {
            return new ParticleBean[size];
        }
    };
}
