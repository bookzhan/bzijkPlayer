package com.ufotosoft.bzmedia.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by zhandalin on 2017-11-29 16:03.
 * 说明:
 */

public class BZVector3 implements Parcelable {
    public float x, y, z;

    public BZVector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.x);
        dest.writeFloat(this.y);
        dest.writeFloat(this.z);
    }

    protected BZVector3(Parcel in) {
        this.x = in.readFloat();
        this.y = in.readFloat();
        this.z = in.readFloat();
    }

    public static final Creator<BZVector3> CREATOR = new Creator<BZVector3>() {
        @Override
        public BZVector3 createFromParcel(Parcel source) {
            return new BZVector3(source);
        }

        @Override
        public BZVector3[] newArray(int size) {
            return new BZVector3[size];
        }
    };
}
