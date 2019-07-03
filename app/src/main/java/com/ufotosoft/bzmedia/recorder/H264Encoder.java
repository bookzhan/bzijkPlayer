package com.ufotosoft.bzmedia.recorder;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class H264Encoder {
    private static String TAG = "bz_AvcEncoder";
    private MediaCodec mediaCodec;

    private int m_width;
    private int m_height;
    //boolean RecordEncDataFlag = true;
    private byte[] m_info = null;
    private byte[] yuv420 = null;

    public H264Encoder(int width, int height, int framerate, int bitrate) {
        BZLogUtil.d(TAG, "H264Encoder IN");
        m_width = width;
        m_height = height;
        yuv420 = new byte[width * height * 3 / 2];

        getSupportColorFormat();
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    private int getSupportColorFormat() {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo codecInfo = null;
        for (int i = 0; i < numCodecs && codecInfo == null; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            String[] types = info.getSupportedTypes();
            boolean found = false;
            for (int j = 0; j < types.length && !found; j++) {
                if (types[j].equals("video/avc")) {
                    System.out.println("found");
                    found = true;
                }
            }
            if (!found)
                continue;
            codecInfo = info;
        }

        BZLogUtil.e(TAG, "Found " + codecInfo.getName() + " supporting " + "video/avc");

        // Find a color profile that the codec supports
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
        BZLogUtil.e(TAG,
                "length-" + capabilities.colorFormats.length + "==" + Arrays.toString(capabilities.colorFormats));

        for (int i = 0; i < capabilities.colorFormats.length; i++) {

            switch (capabilities.colorFormats[i]) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:
                    BZLogUtil.e(TAG, "supported color format::" + capabilities.colorFormats[i]);
                    break;//return capabilities.colorFormats[i];
                default:
                    BZLogUtil.e(TAG, "other color format " + capabilities.colorFormats[i]);
                    break;
            }
        }

        return 0;
    }

    public void close() {
        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int offerEncoder(byte[] input, byte[] output) {
        BZLogUtil.d(TAG, "Encoder in");
        int pos = 0;
        yuv420 = input;
        try {
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            BZLogUtil.d(TAG, "inputBufferIndex = " + inputBufferIndex);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(yuv420);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            BZLogUtil.d(TAG, "outputBufferIndex = " + outputBufferIndex);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);

                if (m_info != null) {
                    System.arraycopy(outData, 0, output, 0, outData.length);
                    pos += outData.length;
                    BZLogUtil.d(TAG, "m_info: " + pos);
                } else {
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                    //if (spsPpsBuffer.getInt() == 0x00000001)
                    if (bufferInfo.flags == 2) {
                        m_info = new byte[outData.length];
                        System.arraycopy(outData, 0, m_info, 0, outData.length);
                        System.arraycopy(outData, 0, output, pos, outData.length);
                        pos += outData.length;
                    } else {
                        BZLogUtil.d(TAG, "errrrr: ");
                        return -1;
                    }
                    BZLogUtil.d(TAG, "m_info: " + Arrays.toString(m_info));
                }

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }

            if (bufferInfo.flags == 1)// if( nv12[4] == 0x65) //key frame
            {
                BZLogUtil.d(TAG, "Key frame");
                System.arraycopy(output, 0, yuv420, 0, pos);
                System.arraycopy(m_info, 0, output, 0, m_info.length);
                System.arraycopy(yuv420, 0, output, m_info.length, pos);
                pos += m_info.length;
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        BZLogUtil.d(TAG, "Encoder out");

//		if(RecordEncDataFlag)
//		{
//			try {
//				FileOut.write(nv12,0,pos);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
        return pos;
    }
}
