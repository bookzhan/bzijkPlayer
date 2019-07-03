package com.ufotosoft.bzmedia.bean;

/**
 * Created by zhandalin on 2018-11-16 15:40.
 * 说明:决定纹理应该怎么处理,填充类型,填充背景,缩放信息
 */
public class TextureHandleInfo {
    private int bgFillType = 0;
    private BZColor bgColor = null;
    private int scaleType = 0;

    public BgFillType getBgFillType() {
        return BgFillType.values()[bgFillType];
    }

    public void setBgFillType(BgFillType bgFillType) {
        this.bgFillType = bgFillType.ordinal();
    }

    public BZColor getBgColor() {
        return bgColor;
    }

    public void setBgColor(BZColor bgColor) {
        this.bgColor = bgColor;
    }

    public BZScaleType getScaleType() {
        return BZScaleType.values()[scaleType];
    }

    public void setScaleType(BZScaleType scaleType) {
        this.scaleType = scaleType.ordinal();
    }
}
