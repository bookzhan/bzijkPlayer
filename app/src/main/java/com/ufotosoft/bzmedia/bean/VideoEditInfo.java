package com.ufotosoft.bzmedia.bean;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;

/**
 * Created by zhandalin on 2017-12-15 11:19.
 * 说明:
 */

public class VideoEditInfo {
    private String name;
    private String thumbnailPath;
    private String thumbnailLessNormalPath;
    private String thumbnailLessSelectedPath;

    private transient Bitmap thumbnailBitmapLessNormal;
    private transient Bitmap thumbnailBitmapLessSelected;



    private transient Bitmap iconBitmap;
    private transient AnimationDrawable gifAnimationDrawable;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getThumbnailLessSelectedPath() {
        return thumbnailLessSelectedPath;
    }

    public void setThumbnailLessSelectedPath(String thumbnailLessSelectedPath) {
        this.thumbnailLessSelectedPath = thumbnailLessSelectedPath;
    }



    public String getThumbnailLessNormalPath() {
        return thumbnailLessNormalPath;
    }

    public void setThumbnailLessNormalPath(String thumbnailLessNormalPath) {
        this.thumbnailLessNormalPath = thumbnailLessNormalPath;
    }

    public Bitmap getThumbnailBitmapLessNormal() {
        return thumbnailBitmapLessNormal;
    }

    public void setThumbnailBitmapLessNormal(Bitmap thumbnailBitmapLessNormal) {
        this.thumbnailBitmapLessNormal = thumbnailBitmapLessNormal;
    }

    public Bitmap getThumbnailBitmapLessSelected() {
        return thumbnailBitmapLessSelected;
    }

    public void setThumbnailBitmapLessSelected(Bitmap thumbnailBitmapLessSelected) {
        this.thumbnailBitmapLessSelected = thumbnailBitmapLessSelected;
    }

    public Bitmap getIconBitmap() {
        return iconBitmap;
    }

    public void setIconBitmap(Bitmap iconBitmap) {
        this.iconBitmap = iconBitmap;
    }

    public void setGifAnimationDrawable(AnimationDrawable gifAnimationDrawable) {
        this.gifAnimationDrawable = gifAnimationDrawable;
    }

    public AnimationDrawable getGifAnimationDrawable() {
        return gifAnimationDrawable;
    }
}
