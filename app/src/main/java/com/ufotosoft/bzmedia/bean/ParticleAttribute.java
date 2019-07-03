package com.ufotosoft.bzmedia.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by zhandalin on 2017-10-27 11:09.
 * 说明:
 */
public class ParticleAttribute implements Parcelable {
    private String imageName;
    private float liveTime;
    private float positionRandom_x;
    private float positionRandom_y;
    private float graduallyScale;
    private float randRotate;
    private float animationInterval;
    private float acceleration;
    private int blendType;
    private BZColor color;
    private BZVector3 direction;
    private float minPointSize;
    private float maxPointSize;
    private float shooterAngle;
    private int maxParticleNum;
    private float particleAddSpeed;
    private float acceleSpeed;
    private BZVector3 initPositionOffset;
    private float gravity;
    private int textureNum;
    private int textureId;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public float getLiveTime() {
        return liveTime;
    }

    public void setLiveTime(float liveTime) {
        this.liveTime = liveTime;
    }

    public float getPositionRandom_x() {
        return positionRandom_x;
    }

    public void setPositionRandom_x(float positionRandom_x) {
        this.positionRandom_x = positionRandom_x;
    }

    public float getPositionRandom_y() {
        return positionRandom_y;
    }

    public void setPositionRandom_y(float positionRandom_y) {
        this.positionRandom_y = positionRandom_y;
    }

    public float getGraduallyScale() {
        return graduallyScale;
    }

    public void setGraduallyScale(float graduallyScale) {
        this.graduallyScale = graduallyScale;
    }

    public float getRandRotate() {
        return randRotate;
    }

    public void setRandRotate(float randRotate) {
        this.randRotate = randRotate;
    }

    public float getAnimationInterval() {
        return animationInterval;
    }

    public void setAnimationInterval(float animationInterval) {
        this.animationInterval = animationInterval;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(float acceleration) {
        this.acceleration = acceleration;
    }

    public int getBlendType() {
        return blendType;
    }

    public void setBlendType(int blendType) {
        this.blendType = blendType;
    }

    public BZColor getColor() {
        return color;
    }

    public void setColor(BZColor color) {
        this.color = color;
    }

    public BZVector3 getDirection() {
        return direction;
    }

    public void setDirection(BZVector3 direction) {
        this.direction = direction;
    }

    public float getMinPointSize() {
        return minPointSize;
    }

    public void setMinPointSize(float minPointSize) {
        this.minPointSize = minPointSize;
    }

    public float getMaxPointSize() {
        return maxPointSize;
    }

    public void setMaxPointSize(float maxPointSize) {
        this.maxPointSize = maxPointSize;
    }

    public float getShooterAngle() {
        return shooterAngle;
    }

    public void setShooterAngle(float shooterAngle) {
        this.shooterAngle = shooterAngle;
    }

    public int getMaxParticleNum() {
        return maxParticleNum;
    }

    public void setMaxParticleNum(int maxParticleNum) {
        this.maxParticleNum = maxParticleNum;
    }

    public float getParticleAddSpeed() {
        return particleAddSpeed;
    }

    public void setParticleAddSpeed(float particleAddSpeed) {
        this.particleAddSpeed = particleAddSpeed;
    }

    public float getAcceleSpeed() {
        return acceleSpeed;
    }

    public void setAcceleSpeed(float acceleSpeed) {
        this.acceleSpeed = acceleSpeed;
    }

    public BZVector3 getInitPositionOffset() {
        return initPositionOffset;
    }

    public void setInitPositionOffset(BZVector3 initPositionOffset) {
        this.initPositionOffset = initPositionOffset;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public int getTextureNum() {
        return textureNum;
    }

    public void setTextureNum(int textureNum) {
        this.textureNum = textureNum;
    }

    public int getTextureId() {
        return textureId;
    }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.imageName);
        dest.writeFloat(this.liveTime);
        dest.writeFloat(this.positionRandom_x);
        dest.writeFloat(this.positionRandom_y);
        dest.writeFloat(this.graduallyScale);
        dest.writeFloat(this.randRotate);
        dest.writeFloat(this.animationInterval);
        dest.writeFloat(this.acceleration);
        dest.writeInt(this.blendType);
        dest.writeParcelable(this.color, flags);
        dest.writeParcelable(this.direction, flags);
        dest.writeFloat(this.minPointSize);
        dest.writeFloat(this.maxPointSize);
        dest.writeFloat(this.shooterAngle);
        dest.writeInt(this.maxParticleNum);
        dest.writeFloat(this.particleAddSpeed);
        dest.writeFloat(this.acceleSpeed);
        dest.writeParcelable(this.initPositionOffset, flags);
        dest.writeFloat(this.gravity);
        dest.writeInt(this.textureNum);
        dest.writeInt(this.textureId);
    }

    public ParticleAttribute() {
    }

    protected ParticleAttribute(Parcel in) {
        this.imageName = in.readString();
        this.liveTime = in.readFloat();
        this.positionRandom_x = in.readFloat();
        this.positionRandom_y = in.readFloat();
        this.graduallyScale = in.readFloat();
        this.randRotate = in.readFloat();
        this.animationInterval = in.readFloat();
        this.acceleration = in.readFloat();
        this.blendType = in.readInt();
        this.color = in.readParcelable(BZColor.class.getClassLoader());
        this.direction = in.readParcelable(BZVector3.class.getClassLoader());
        this.minPointSize = in.readFloat();
        this.maxPointSize = in.readFloat();
        this.shooterAngle = in.readFloat();
        this.maxParticleNum = in.readInt();
        this.particleAddSpeed = in.readFloat();
        this.acceleSpeed = in.readFloat();
        this.initPositionOffset = in.readParcelable(BZVector3.class.getClassLoader());
        this.gravity = in.readFloat();
        this.textureNum = in.readInt();
        this.textureId = in.readInt();
    }

    public static final Creator<ParticleAttribute> CREATOR = new Creator<ParticleAttribute>() {
        @Override
        public ParticleAttribute createFromParcel(Parcel source) {
            return new ParticleAttribute(source);
        }

        @Override
        public ParticleAttribute[] newArray(int size) {
            return new ParticleAttribute[size];
        }
    };
}
