package com.ufotosoft.bzmedia.encoder;

/**
 * Created by zhandalin on 2019-03-14 13:19.
 * 说明:
 */
public interface OnVideoPacketAvailableListener {
    void onVideoPacketAvailable(byte[] videoPacket, long size, long pts);
}
