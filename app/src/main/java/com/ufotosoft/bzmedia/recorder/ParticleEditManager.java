package com.ufotosoft.bzmedia.recorder;

import android.os.Parcel;
import android.os.Parcelable;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.bean.ParticleEditInfo;
import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhandalin on 2017-12-13 16:17.
 * 说明:粒子视频记录器
 */

public class ParticleEditManager implements Parcelable {
    private static final String TAG = "bz_ParticleVideoManager";
    private final static Object objectLock = new Object();
    private List<ParticleEditInfo> videoEditItemList = new ArrayList<>();
    private List<ParticleEditInfo> tempItemList = new ArrayList<>();
    private volatile long pathManagerNativeHandle = 0;
    private int x = 0;
    private int y = 0;
    private int width = 720;
    private int height = 720;

    public ParticleEditManager() {
        pathManagerNativeHandle = BZMedia.initParticlePathManager();
    }

    public ParticleEditManager(long pathManagerNativeHandle) {
        this.pathManagerNativeHandle = pathManagerNativeHandle;
    }

    public long getPathManagerNativeHandle() {
        return pathManagerNativeHandle;
    }

    public void setPathManagerNativeHandle(long pathManagerNativeHandle) {
        this.pathManagerNativeHandle = pathManagerNativeHandle;
    }

    public void addParticleEditInfoItem(ParticleEditInfo particleEditInfo) {
        if (null == particleEditInfo
                || null == particleEditInfo.getParticleBean()
                || null == particleEditInfo.getVideoEditItem()) {
            return;
        }
        synchronized (objectLock) {
            for (ParticleEditInfo editInfo : tempItemList) {
                BZMedia.particlesOnRelease(editInfo.getEngineHandel());
            }
            tempItemList.clear();
            long handel = BZMedia.particlesOnSurfaceCreated(particleEditInfo.getParticleBean(), pathManagerNativeHandle, true);
            BZMedia.particlesOnSurfaceChanged(handel, x, y, width, height);
            particleEditInfo.setEngineHandel(handel);

            if (!videoEditItemList.isEmpty()) {
                //前一个让它自动消失
                ParticleEditInfo particleEditInfoEnd = videoEditItemList.get(videoEditItemList.size() - 1);
                BZMedia.particlesEnableAddParticle(particleEditInfoEnd.getEngineHandel(), false);
            }
            videoEditItemList.add(particleEditInfo);
        }
    }

    public void particlesOnSurfaceCreated4CachePath() {
        synchronized (objectLock) {
            for (int i = 0; i < videoEditItemList.size(); i++) {
                ParticleEditInfo particleEditInfo = videoEditItemList.get(i);
                long handle = BZMedia.particlesOnSurfaceCreated4CachePath(pathManagerNativeHandle, i);
                particleEditInfo.setEngineHandel(handle);
            }
        }
    }

    public void removeVideoInfoItem(int i) {
        synchronized (objectLock) {
            if (i < videoEditItemList.size())
                videoEditItemList.remove(i);
        }
    }


    public void removeCurrentVideoInfoItem() {
        synchronized (objectLock) {
            if (videoEditItemList.size() > 0) {
                //维护一个栈
                ParticleEditInfo particleEditInfo = videoEditItemList.get(videoEditItemList.size() - 1);
                tempItemList.add(0, particleEditInfo);
                videoEditItemList.remove(particleEditInfo);
                BZMedia.particlesPathManagerRemoveCurrent(pathManagerNativeHandle);
            }
        }
    }

    public int revertCurrentVideoInfoItem() {
        synchronized (objectLock) {
            if (tempItemList.size() > 0) {
                //维护一个栈
                ParticleEditInfo videoEditItem = tempItemList.get(0);
                videoEditItemList.add(videoEditItem);
                tempItemList.remove(videoEditItem);
                BZMedia.particlesPathManagerRevertCurrent(pathManagerNativeHandle);
            }
        }
        return tempItemList.size();
    }


    public void particlesOnDrawFrame(long pts) {
        synchronized (objectLock) {
            if (!videoEditItemList.isEmpty()) {
                ParticleEditInfo particleEditInfoEnd = videoEditItemList.get(videoEditItemList.size() - 1);
                BZMedia.particlesOnDrawFrame(particleEditInfoEnd.getEngineHandel(), pts);
            }
        }
    }

    public void particlesTouchEvent(float finalTouchX, float finalTouchY) {
        synchronized (objectLock) {
            if (!videoEditItemList.isEmpty()) {
                ParticleEditInfo particleEditInfoEnd = videoEditItemList.get(videoEditItemList.size() - 1);
                BZMedia.particlesTouchEvent(particleEditInfoEnd.getEngineHandel(), finalTouchX, finalTouchY);
            }
        }
    }

    public void particlesSeek(long pts, boolean skipLastItem) {
        synchronized (objectLock) {
            for (int i = 0; i < videoEditItemList.size(); i++) {
                if (i != videoEditItemList.size() - 1 || !skipLastItem) {
                    ParticleEditInfo particleEditInfo = videoEditItemList.get(i);
                    BZMedia.particlesSeek(particleEditInfo.getEngineHandel(), pts);
                }
            }
        }
    }

    public void particlesOnRelease() {
        synchronized (objectLock) {
            for (ParticleEditInfo particleEditInfo : videoEditItemList) {
                BZMedia.particlesOnRelease(particleEditInfo.getEngineHandel());
                particleEditInfo.setEngineHandel(0);
            }
        }
    }

    public int getVideoInfoItemSize() {
        synchronized (objectLock) {
            return videoEditItemList.size();
        }
    }

    public ParticleEditInfo getCurrentParticleEditInfo() {
        synchronized (objectLock) {
            if (videoEditItemList.size() > 0) {
                return videoEditItemList.get(videoEditItemList.size() - 1);
            }
            return null;
        }
    }

    public void particlesOnSurfaceChanged(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        synchronized (objectLock) {
            for (ParticleEditInfo particleEditInfo : videoEditItemList) {
                if (null != particleEditInfo.getParticleBean()) {
                    BZMedia.particlesOnSurfaceChanged(particleEditInfo.getEngineHandel(), x, y, width, height);
                }
            }
        }
    }

    public List<ParticleEditInfo> getParticleEditInfoItemList() {
        synchronized (objectLock) {
            return videoEditItemList;
        }
    }

    public void release() {
        BZLogUtil.d(TAG, "release start");
        synchronized (objectLock) {
            videoEditItemList.clear();
            tempItemList.clear();
            BZMedia.releaseParticlePathManager(pathManagerNativeHandle);
            pathManagerNativeHandle = 0;
            BZLogUtil.d(TAG, "release finish");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.videoEditItemList);
        dest.writeTypedList(this.tempItemList);
        dest.writeLong(this.pathManagerNativeHandle);
        dest.writeInt(this.x);
        dest.writeInt(this.y);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    protected ParticleEditManager(Parcel in) {
        this.videoEditItemList = in.createTypedArrayList(ParticleEditInfo.CREATOR);
        this.tempItemList = in.createTypedArrayList(ParticleEditInfo.CREATOR);
        this.pathManagerNativeHandle = in.readLong();
        this.x = in.readInt();
        this.y = in.readInt();
        this.width = in.readInt();
        this.height = in.readInt();
    }

    public static final Creator<ParticleEditManager> CREATOR = new Creator<ParticleEditManager>() {
        @Override
        public ParticleEditManager createFromParcel(Parcel source) {
            return new ParticleEditManager(source);
        }

        @Override
        public ParticleEditManager[] newArray(int size) {
            return new ParticleEditManager[size];
        }
    };
}
