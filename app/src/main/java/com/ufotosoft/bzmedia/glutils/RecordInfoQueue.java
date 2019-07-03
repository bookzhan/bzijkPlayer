package com.ufotosoft.bzmedia.glutils;

import com.ufotosoft.bzmedia.bean.RecordDrawInfo;
import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.util.ArrayList;

/**
 * Created by zhandalin on 2019-04-28 16:15.
 * 说明:
 */
public class RecordInfoQueue {
    private static final String TAG = "bz_RecordInfoQueue";
    private ArrayList<RecordDrawInfo> arrayList = new ArrayList<>();

    public synchronized void add(RecordDrawInfo recordDrawInfo) {
        if (null == recordDrawInfo) {
            return;
        }
        arrayList.add(recordDrawInfo);
    }

    public synchronized int size() {
        return arrayList.size();
    }

    public synchronized void remove(int index) {
        if (index >= arrayList.size()) {
            return;
        }
        arrayList.remove(index);
    }

    public synchronized void remove(RecordDrawInfo recordDrawInfo) {
        if (null == recordDrawInfo) {
            return;
        }
        arrayList.remove(recordDrawInfo);
    }

    public synchronized RecordDrawInfo getFront() {
        if (arrayList.isEmpty()) {
            return null;
        }
        return arrayList.get(0);
    }

    public synchronized RecordDrawInfo getBack() {
        if (arrayList.isEmpty()) {
            return null;
        }
        return arrayList.get(arrayList.size() - 1);
    }

    public synchronized RecordDrawInfo get(int index) {
        if (arrayList.isEmpty()) {
            return null;
        }
        return arrayList.get(index);
    }

    /**
     * @param size 需要保留的个数,其它的释放
     */
    public synchronized void release4Remain(int size) {
        if (arrayList.isEmpty() || arrayList.size() <= size) {
            return;
        }
        int releaseCount = arrayList.size() - size;
        BZLogUtil.d(TAG, "releaseCount=" + releaseCount);
        for (int i = 0; i < releaseCount; i++) {
            RecordDrawInfo recordDrawInfo = arrayList.get(0);
            if (null != recordDrawInfo.getFrameBufferUtil()) {
                recordDrawInfo.getFrameBufferUtil().release();
            }
            arrayList.remove(0);
        }
    }

    public synchronized void clear() {
        arrayList.clear();
    }

    public synchronized void release() {
        if (arrayList.isEmpty()) {
            return;
        }
        for (RecordDrawInfo recordDrawInfo : arrayList) {
            if (null != recordDrawInfo.getFrameBufferUtil()) {
                recordDrawInfo.getFrameBufferUtil().release();
            }
        }
        arrayList.clear();
    }

}
