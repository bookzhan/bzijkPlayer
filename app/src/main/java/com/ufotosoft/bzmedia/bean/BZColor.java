package com.ufotosoft.bzmedia.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by zhandalin on 2017-10-27 13:31.
 * 说明:
 */
public class BZColor implements Parcelable {
    public float r, g, b, a;

    public BZColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public float getR() {
        return r;
    }

    public void setR(float r) {
        this.r = r;
    }

    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        this.b = b;
    }

    public float getA() {
        return a;
    }

    public void setA(float a) {
        this.a = a;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.r);
        dest.writeFloat(this.g);
        dest.writeFloat(this.b);
        dest.writeFloat(this.a);
    }

    protected BZColor(Parcel in) {
        this.r = in.readFloat();
        this.g = in.readFloat();
        this.b = in.readFloat();
        this.a = in.readFloat();
    }

    public static final Creator<BZColor> CREATOR = new Creator<BZColor>() {
        @Override
        public BZColor createFromParcel(Parcel source) {
            return new BZColor(source);
        }

        @Override
        public BZColor[] newArray(int size) {
            return new BZColor[size];
        }
    };
}
