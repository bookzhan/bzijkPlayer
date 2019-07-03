package com.ufotosoft.bzmedia.encoder;


/**
 * Created by zhandalin on 2019-01-11 20:41.
 * 说明:
 */
public interface OnVideoEncodeListener {
    void onPrepared(boolean success);

    void onStopped(boolean success);
}
