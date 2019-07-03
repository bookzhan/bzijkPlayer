package com.ufotosoft.bzmedia.bean;

/**
 * Created by zhandalin on 2019-02-22 10:39.
 * 说明:
 */
public class TextureInfo {
    private int textureID = 0;
    private int textureWidth = 0;
    private int textureHeight = 0;
    //单位毫秒,纹理的呈现时间
    private long texturePts = 0;

    public int getTextureID() {
        return textureID;
    }

    public void setTextureID(int textureID) {
        this.textureID = textureID;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public void setTextureWidth(int textureWidth) {
        this.textureWidth = textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public void setTextureHeight(int textureHeight) {
        this.textureHeight = textureHeight;
    }

    public long getTexturePts() {
        return texturePts;
    }

    public void setTexturePts(long texturePts) {
        this.texturePts = texturePts;
    }
}
