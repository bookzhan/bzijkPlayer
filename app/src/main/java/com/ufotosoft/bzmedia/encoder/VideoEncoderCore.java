/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ufotosoft.bzmedia.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.ufotosoft.bzmedia.utils.BZLogUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * This class wraps up the core components used for surface-input video encoding.
 * <p>
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 * <p>
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoEncoderCore {
    private static final String TAG = "bz_VideoEncoderCore";
    private static final boolean VERBOSE = true;

    // TODO: these ought to be configurable as well
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 1;           // 5 seconds between I-frames
//    private MediaMuxer mMuxer;

    private Surface mInputSurface;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private boolean mMuxerStarted = false;
    private long logCount = 0;
    private boolean prepareSuccess = false;
    private OnVideoPacketAvailableListener onVideoPacketAvailableListener;
    private byte[] yuvMaxData = null;
    private long startRecordTime = -1;
    private ArrayList<Long> timeStampList = new ArrayList<>();

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    public VideoEncoderCore(int width, int height, int bitRate) {
        try {
            prepareSuccess = false;
            mBufferInfo = new MediaCodec.BufferInfo();

            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            if (VERBOSE) BZLogUtil.d(TAG, "format: " + format);

            // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
            // we can use for input and wrap it with a class that handles the EGL work.
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            //码率模式 支持的不好,不设置

            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();

            // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
            // because our MediaFormat doesn't have the Magic Goodies.  These can only be
            // obtained from the encoder after it has started processing data.
            //
            // We're not actually interested in multiplexing audio.  We just want to convert
            // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
            yuvMaxData = new byte[width * height * 3 / 2];


//            mMuxer = new MediaMuxer("/sdcard/bzmedia/Muxer_" + System.currentTimeMillis() + ".mp4",
//                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mTrackIndex = -1;
            mMuxerStarted = false;
            prepareSuccess = true;
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        if (VERBOSE) BZLogUtil.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            try {
                mEncoder.flush();
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
            try {
                mEncoder.stop();
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
            try {
                mEncoder.release();
                mEncoder = null;
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
        }
//        if (mMuxer != null) {
//            // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
//            //       of frames submitted, and don't call stop() if we haven't written anything.
//            try {
//                mMuxer.stop();
//            } catch (Throwable e) {
//                BZLogUtil.e(TAG, e);
//            }
//            try {
//                mMuxer.release();
//                mMuxer = null;
//            } catch (Throwable e) {
//                BZLogUtil.e(TAG, e);
//            }
//        }
        yuvMaxData = null;
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream, long timeStamp, long pts) {
        try {
            if (startRecordTime < 0) {
                startRecordTime = timeStamp;
            }
            timeStampList.add(timeStamp);

            final int TIMEOUT_USEC = 10000;
            if (logCount % 30 == 0) BZLogUtil.v(TAG, "drainEncoder(" + endOfStream + ")");

            if (endOfStream) {
                if (VERBOSE) BZLogUtil.d(TAG, "sending EOS to encoder");
                mEncoder.signalEndOfInputStream();
            }
            logCount++;
            mBufferInfo.presentationTimeUs = pts;
//            BZLogUtil.e(TAG, "presentationTimeUs before pts=" + pts);
            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (!endOfStream) {
                        break;      // out of while
                    } else {
                        if (VERBOSE) {
                            BZLogUtil.d(TAG, "no output available, spinning to await EOS break now");
                        }
                        //不能 break 有很多缓存在里面要刷出来
//                        break;
                    }
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // should happen before receiving buffers, and should only happen once
                    if (mMuxerStarted) {
                        BZLogUtil.e(TAG, "format changed twice");
                        break;
                    }
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    BZLogUtil.w(TAG, "encoder output format changed: " + newFormat);

                    // now that we have the Magic Goodies, start the muxer
//                    mTrackIndex = mMuxer.addTrack(newFormat);
//                    mMuxer.start();
                    mMuxerStarted = true;
                } else if (encoderStatus < 0) {
                    BZLogUtil.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        BZLogUtil.e(TAG, "encoderOutputBuffer " + encoderStatus +
                                " was null");
                        break;
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // The codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                        if (VERBOSE) BZLogUtil.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        if (null != onVideoPacketAvailableListener && null != yuvMaxData) {
                            encodedData.get(yuvMaxData, 0, mBufferInfo.size);
                            onVideoPacketAvailableListener.onVideoPacketAvailable(yuvMaxData, mBufferInfo.size, 0);
                        }
                        mBufferInfo.size = 0;
                    }
                    if (mBufferInfo.size != 0) {
                        if (!mMuxerStarted) {
                            BZLogUtil.e(TAG, "muxer hasn't started");
                            break;
                        }

                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                        if (null != onVideoPacketAvailableListener && null != yuvMaxData) {
                            encodedData.get(yuvMaxData, 0, mBufferInfo.size);
                            long temp = timeStamp;
                            if (!timeStampList.isEmpty()) {
                                temp = timeStampList.get(0);
                                timeStampList.remove(0);
                            }
                            onVideoPacketAvailableListener.onVideoPacketAvailable(yuvMaxData, mBufferInfo.size, temp - startRecordTime);
                        }
//                        mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                        if (logCount % 15 == 0) {
                            BZLogUtil.v(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                    mBufferInfo.presentationTimeUs);
                        }
                    }
//                    BZLogUtil.e(TAG, "presentationTimeUs final pts=" + mBufferInfo.presentationTimeUs);
                    mEncoder.releaseOutputBuffer(encoderStatus, false);

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (!endOfStream) {
                            BZLogUtil.w(TAG, "reached end of stream unexpectedly");
                        } else {
                            if (VERBOSE) BZLogUtil.d(TAG, "end of stream reached");
                        }
                        break;      // out of while
                    }
                }
            }
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
    }

    public boolean isPrepareSuccess() {
        return prepareSuccess;
    }

    public void setOnVideoPacketAvailableListener(OnVideoPacketAvailableListener onVideoPacketAvailableListener) {
        this.onVideoPacketAvailableListener = onVideoPacketAvailableListener;
    }
}
