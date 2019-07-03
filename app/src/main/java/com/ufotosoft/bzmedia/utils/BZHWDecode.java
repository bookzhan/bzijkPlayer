package com.ufotosoft.bzmedia.utils;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.glutils.BaseProgram;
import com.ufotosoft.bzmedia.glutils.ExternalTextureProgram;
import com.ufotosoft.bzmedia.glutils.FrameBufferUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_VIEWPORT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glGetIntegerv;
import static android.opengl.GLES20.glViewport;

/**
 * Created by zhandalin on 2019-03-22 19:42.
 * 说明:硬解,传入AVPacket数据,输出成纹理,需要在GL线程调用
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BZHWDecode {
    private static final String TAG = "bz_BZHWDecode";
    private static final int TIMEOUT_USEC = 10000;

    private SurfaceTexture surfaceTexture = null;
    private Surface mSurface = null;
    private long externalTextureProgramHandle = 0;
    private MediaFormat mediaFormat = null;
    private MediaCodec mediaCodec = null;
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private int videoWidth = 0;
    private int videoHeight = 0;
    private int rotate = 0;
    private long decodeIndex = 0;
    private long onFrameAvailableCount = 0;
    private List<Long> ptsList = Collections.synchronizedList(new ArrayList<Long>());
    private long cropTextureHandle;
    private int mOutVideoWidth;
    private int mOutVideoHeight;
    private FrameBufferUtil frameBufferUtil = null;
    private BaseProgram baseProgram = null;
    private int[] viewPortSize = new int[4];
    private int finalTextureId = 0;
    private boolean requestFlushDecode = false;


    public synchronized void onSurfaceCreate() {
        BZLogUtil.d(TAG, "onSurfaceCreate this=" + this);
        externalTextureProgramHandle = ExternalTextureProgram.initNative(true);
        int textureID = ExternalTextureProgram.initGlResource(externalTextureProgramHandle);
        ExternalTextureProgram.setVideoRotation(externalTextureProgramHandle, rotate);

        if (null == surfaceTexture || null == mSurface) {
            surfaceTexture = new SurfaceTexture(textureID);
            mSurface = new Surface(surfaceTexture);
            surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    onFrameAvailableCount++;
                }
            });
        } else {
            surfaceTexture.attachToGLContext(textureID);
        }
    }

    public synchronized void onSurfaceDestroy() {
        BZLogUtil.d(TAG, "onSurfaceDestroy this=" + this);
        if (null != surfaceTexture) {
            surfaceTexture.detachFromGLContext();
        }
        if (externalTextureProgramHandle != 0) {
            ExternalTextureProgram.releaseGlResource(externalTextureProgramHandle);
            externalTextureProgramHandle = 0;
        }
        if (cropTextureHandle != 0) {
            BZMedia.cropTextureOnPause(cropTextureHandle);
        }
        if (null != frameBufferUtil) {
            frameBufferUtil.release();
            frameBufferUtil = null;
        }
        if (null != baseProgram) {
            baseProgram.release();
            baseProgram = null;
        }
    }

    public synchronized int mediacodecInit(int mimetype, int width, int height, int rotate, byte[] csd0,
                                           byte[] csd1) {
        BZLogUtil.d(TAG, "mediacodecInit mimetype=" + mimetype + " width=" + width + " height=" + height + " rotate=" + rotate);
        this.videoWidth = width;
        this.videoHeight = height;
        if (rotate == 90 || rotate == 270) {
            this.videoWidth = height;
            this.videoHeight = width;
        }
        this.rotate = rotate;
        decodeIndex = 0;
        if (externalTextureProgramHandle != 0) {
            ExternalTextureProgram.setVideoRotation(externalTextureProgramHandle, rotate);
        }
        int ret = 0;
        if (mSurface != null) {
            try {
                String mtype = getMimeType(mimetype);
                mediaFormat = MediaFormat.createVideoFormat(mtype, width, height);
                mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
                mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
                mediaFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
                //手动创建MediaFormat对象一定要设置csd参数，否则添加进MediaMuxer的MediaFormat会导致MediaMuxer调用stop()时抛出异常。对于H.264来说，"csd-0"和"csd-1"分别对应sps和pps；对于AAC来说，"csd-0"对应ADTS
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
                mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1));
                mediaCodec = MediaCodec.createDecoderByType(mtype);
                if (mSurface != null) {
                    mediaCodec.configure(mediaFormat, mSurface, null, 0);
                    mediaCodec.start();
                }

            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
                ret = -1;
            }
        } else {
            BZLogUtil.d(TAG, "mediacodecInit Error null==mSurface");
        }
        return ret;
    }

    private synchronized String getMimeType(int type) {
        if (type == 1) {
            return "video/avc";
        } else if (type == 2) {
            return "video/hevc";
        } else if (type == 3) {
            return "video/mp4v-es";
        } else if (type == 4) {
            return "video/x-ms-wmv";
        }
        return "";
    }

    public synchronized int reDraw() {
        if (externalTextureProgramHandle == 0) {
            return -1;
        }
        if (finalTextureId > 0 && null != baseProgram) {
            baseProgram.draw(finalTextureId);
        } else {
            ExternalTextureProgram.onDrawFrame(externalTextureProgramHandle);
        }
        if (decodeIndex % 3 == 0) {
            BZLogUtil.v(TAG, "reDraw cropTextureHandle=" + cropTextureHandle + " rotate=" + rotate + " onFrameAvailableCount=" + onFrameAvailableCount + " time=" + System.currentTimeMillis());
        }
        decodeIndex++;
        return 0;
    }

    public synchronized void flushDecode() {
        BZLogUtil.d(TAG, "flushDecode");
        requestFlushDecode = true;
    }

    /**
     * @return 返回时间戳
     */
    public synchronized long mediacodecDecode(byte[] bytes, int size, long pts) {
        if (externalTextureProgramHandle == 0 || null == surfaceTexture || null == mSurface) {
            BZLogUtil.w(TAG, "mediacodecDecode onSurfaceCreate");
            onSurfaceCreate();
        }
        if (requestFlushDecode) {
            if (mediaCodec != null) {
                try {
                    mediaCodec.flush();
                } catch (Throwable e) {
                    BZLogUtil.e(TAG, e);
                }
            }
            ptsList.clear();
            requestFlushDecode = false;
        }
        ptsList.add(pts);
        long startTime = System.currentTimeMillis();
        long resultPts = -1;
        if (mediaCodec != null && info != null) {
            try {
                if (null == bytes) {
                    BZLogUtil.e(TAG, "null == bytes");
                    mediaCodec.flush();
                }
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);//-1表示一直等待
                if (inputBufferIndex >= 0) {
                    //fill inputBuffers[inputBufferIndex] with valid data
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffers()[inputBufferIndex];
                    byteBuffer.clear();
                    byteBuffer.put(bytes);
                    //喂数据，pts只要保证一直递增就行
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, size, pts, 0);
                }
                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, TIMEOUT_USEC);

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outputFormat = mediaCodec.getOutputFormat();
                    mOutVideoWidth = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
                    mOutVideoHeight = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);

                    if (rotate == 90 || rotate == 270) {
                        int temp = mOutVideoWidth;
                        mOutVideoWidth = mOutVideoHeight;
                        mOutVideoHeight = temp;
                    }

                    BZLogUtil.d(TAG, "decoder output format changed: " + outputFormat);
                    //需要裁切纹理了
                    if (mOutVideoWidth != videoWidth || mOutVideoHeight != videoHeight) {
                        if (cropTextureHandle == 0) {
                            cropTextureHandle = BZMedia.initCropTexture();
                        }
                        if (null != frameBufferUtil) {
                            frameBufferUtil.release();
                        }
                        frameBufferUtil = new FrameBufferUtil(mOutVideoWidth, mOutVideoHeight);
                    }
                }

                resultPts = info.presentationTimeUs;
                while (outputBufferIndex >= 0) {
                    //不再需要解码出来的数据了，需要释放掉
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, TIMEOUT_USEC);
                }
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
        }

        updateTexImage();

        if (null != surfaceTexture) {
            resultPts = surfaceTexture.getTimestamp();
            if (resultPts <= 0) {
                return -1;
            }
        }
        decodeIndex++;

        //需要剪裁纹理
        if (cropTextureHandle != 0) {
            if (null == frameBufferUtil) {
                frameBufferUtil = new FrameBufferUtil(mOutVideoWidth, mOutVideoHeight);
            }
            if (null == baseProgram) {
                baseProgram = new BaseProgram(true);
            }
            glGetIntegerv(GL_VIEWPORT, viewPortSize, 0);
            frameBufferUtil.bindFrameBuffer();
            glClear(GL_COLOR_BUFFER_BIT);
            glViewport(0, 0, mOutVideoWidth, mOutVideoHeight);
            ExternalTextureProgram.onDrawFrame(externalTextureProgramHandle);
            frameBufferUtil.unbindFrameBuffer();
            //2是为了防止精度误差
            int space = 2;
            //FIXME 待验证
            if (rotate == 90) {
                finalTextureId = BZMedia.cropTexture(cropTextureHandle, frameBufferUtil.getFrameBufferTextureID(), mOutVideoWidth, mOutVideoHeight, 0, mOutVideoHeight - videoHeight + space, videoWidth, videoHeight);
            } else if (rotate == 180) {
                finalTextureId = BZMedia.cropTexture(cropTextureHandle, frameBufferUtil.getFrameBufferTextureID(), mOutVideoWidth, mOutVideoHeight, mOutVideoWidth - videoWidth + space, 0, videoWidth, videoHeight);
            } else if (rotate == 270) {
                //已验证
                finalTextureId = BZMedia.cropTexture(cropTextureHandle, frameBufferUtil.getFrameBufferTextureID(), mOutVideoWidth, mOutVideoHeight, 0, 0, videoWidth, videoHeight - space);
            } else {
                //以验证
                finalTextureId = BZMedia.cropTexture(cropTextureHandle, frameBufferUtil.getFrameBufferTextureID(), mOutVideoWidth, mOutVideoHeight, 0, 0, videoWidth - space, videoHeight);
            }

            glViewport(viewPortSize[0], viewPortSize[1], viewPortSize[2], viewPortSize[3]);
            baseProgram.draw(finalTextureId);
        } else {
            //转换成普通纹理
            ExternalTextureProgram.onDrawFrame(externalTextureProgramHandle);
        }

        if (!ptsList.isEmpty()) {
            resultPts = ptsList.get(0);
            ptsList.remove(0);
        }
        if (decodeIndex % 10 == 0) {
            BZLogUtil.v(TAG, "MediaCodec 处理一帧 耗时=" + (System.currentTimeMillis() - startTime) + " resultPts=" + resultPts + " ptsList.size=" + ptsList.size() + " cropTextureHandle=" + cropTextureHandle + " rotate=" + rotate + " onFrameAvailableCount=" + onFrameAvailableCount);
        }

        return pts;
    }

    private void updateTexImage() {
        if (onFrameAvailableCount <= 0) {
            return;
        }
        try {
            if (null != surfaceTexture)
                surfaceTexture.updateTexImage();
        } catch (Throwable e) {
            BZLogUtil.e(TAG, e);
        }
        onFrameAvailableCount--;
    }

    public synchronized void release() {
        BZLogUtil.d(TAG, "---release--");
        if (null != surfaceTexture) {
            try {
                surfaceTexture.release();
                surfaceTexture = null;
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
        }
        if (null != mSurface) {
            try {
                mSurface.release();
                mSurface = null;
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
        }
        if (cropTextureHandle != 0) {
            BZMedia.cropTextureRelease(cropTextureHandle);
            cropTextureHandle = 0;
        }
        if (mediaCodec != null) {
            try {
                mediaCodec.flush();
                mediaCodec.stop();
                mediaCodec.release();
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
            mediaCodec = null;
            mediaFormat = null;
        }
    }
}
