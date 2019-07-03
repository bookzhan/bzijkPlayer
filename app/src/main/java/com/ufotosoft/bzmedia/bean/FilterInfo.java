package com.ufotosoft.bzmedia.bean;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.ufotosoft.bzmedia.BZMedia;

/**
 * Created by luoye on 2017/6/18.
 * 滤镜基本参数封装
 */
public class FilterInfo implements Parcelable {
    private String filterName;
    private int filterType;
    private int filterIntensity;
    private int filterIntensity_1;
    private int filterIntensity_2;
    private String filterExtraParam;

    private transient Bitmap icon_bitmap;

    private boolean isSelected;

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public BZMedia.FilterType getFilterType() {
        return BZMedia.FilterType.values()[filterType];
    }

    public void setFilterType(BZMedia.FilterType filterType) {
        this.filterType = filterType.ordinal();
    }

    public int getFilterIntensity() {
        return filterIntensity;
    }

    public void setFilterIntensity(int filterIntensity) {
        this.filterIntensity = filterIntensity;
    }

    public int getFilterIntensity_1() {
        return filterIntensity_1;
    }

    public void setFilterIntensity_1(int filterIntensity_1) {
        this.filterIntensity_1 = filterIntensity_1;
    }

    public int getFilterIntensity_2() {
        return filterIntensity_2;
    }

    public void setFilterIntensity_2(int filterIntensity_2) {
        this.filterIntensity_2 = filterIntensity_2;
    }

    public String getFilterExtraParam() {
        return filterExtraParam;
    }

    public void setFilterExtraParam(String filterExtraParam) {
        this.filterExtraParam = filterExtraParam;
    }


    public Bitmap getIcon_bitmap() {
        return icon_bitmap;
    }

    public void setIcon_bitmap(Bitmap icon_bitmap) {
        this.icon_bitmap = icon_bitmap;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.filterName);
        dest.writeInt(this.filterType);
        dest.writeInt(this.filterIntensity);
        dest.writeInt(this.filterIntensity_1);
        dest.writeInt(this.filterIntensity_2);
        dest.writeString(this.filterExtraParam);
        dest.writeByte(this.isSelected ? (byte) 1 : (byte) 0);
    }

    public FilterInfo() {
    }

    protected FilterInfo(Parcel in) {
        this.filterName = in.readString();
        this.filterType = in.readInt();
        this.filterIntensity = in.readInt();
        this.filterIntensity_1 = in.readInt();
        this.filterIntensity_2 = in.readInt();
        this.filterExtraParam = in.readString();
        this.isSelected = in.readByte() != 0;
    }

    public static final Creator<FilterInfo> CREATOR = new Creator<FilterInfo>() {
        @Override
        public FilterInfo createFromParcel(Parcel source) {
            return new FilterInfo(source);
        }

        @Override
        public FilterInfo[] newArray(int size) {
            return new FilterInfo[size];
        }
    };
}
