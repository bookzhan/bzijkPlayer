package com.ufotosoft.bzmedia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.ufotosoft.bzmedia.bean.GifAttribute;
import com.ufotosoft.bzmedia.bean.ParticleBean;
import com.ufotosoft.bzmedia.bean.VideoRecordParams;
import com.ufotosoft.bzmedia.bean.VideoTransCodeParams;
import com.ufotosoft.bzmedia.glutils.BZOpenGlUtils;
import com.ufotosoft.bzmedia.recorder.VideoRecorderBase;
import com.ufotosoft.bzmedia.utils.BZLogUtil;
import com.ufotosoft.bzmedia.utils.BZResourceParserUtil;
import com.ufotosoft.bzmedia.utils.BZSpUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ShortBuffer;

/**
 * Created by zhandalin on 2017-03-16 14:12.
 * 说明:
 */
public class BZMedia {
    private static final String TAG = "bz_BZMedia";
    private static String KEY_VIDEO_EXPLORE_RATE = "video_explore_rate";
    private static String KEY_VIDEO_EXPLORE_WIDTH = "video_explore_width";
    private static final String KEY_COPY_RESOURCE_FLAG = "copy_resource_flag";

    private static boolean hasInit = false;

    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static SharedPreferences sp;

    public static native String stringFromJNI();


    public static int init(Context context, boolean showLog) {
        if (hasInit) return 0;
//        if (!BZCPUTool.isArmCpuArchitecture()) {
//            Log.e(TAG, "only support arm cpu,init fail");
//            return -1;
//        }
        System.loadLibrary("bzffmpeg");
        System.loadLibrary("bzffmpegcmd");
        System.loadLibrary("bzmedia");

        BZOpenGlUtils.detectOpenGLES30(context);
        BZLogUtil.setLog(showLog);
        BZResourceParserUtil.init(context);
        BZSpUtils.init(context);

        BZMedia.context = context;
        int ret = initNative(context, showLog);

        hasInit = true;
        return ret;
    }

    private static native int initNative(Context context, boolean showLog);

    /**
     * copy 所需要的资源文件, 这个耗时100多毫秒就不考虑线程同步了
     */
    private static void copyResource(final Context context, final String resourceFileParent) {
        try {
            final String[] filters = context.getResources().getAssets().list("filter");
            if (null == filters || filters.length == 0) return;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    File file = new File(resourceFileParent);
                    if (!file.exists())
                        if (!file.mkdirs()) return;
                    for (String filterName : filters) {
                        copyFile(context, "filter/" + filterName, resourceFileParent + "/" + filterName);
                    }
                    BZLogUtil.d(TAG, "copy 滤镜资源文件耗时=" + (System.currentTimeMillis() - startTime));
                    sp.edit().putBoolean(KEY_COPY_RESOURCE_FLAG, true).apply();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getVersioncode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        int versionCode = 1;
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 拷贝资源到sdcard
     */
    private static boolean copyFile(final Context ctx, String fileName, String target) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = ctx.getAssets().open(fileName);
            out = new FileOutputStream(target);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                BZLogUtil.e(TAG, e);
            }
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                BZLogUtil.e(TAG, e);
            }
        }
        return true;
    }

    /**
     * @return Configure: FFmpeg类库的配置信息
     */
    public static native String getFFmpegConfigure();


    /**
     * Protocol:  FFmpeg类库支持的协议
     * AVFormat:  FFmpeg类库支持的封装格式
     * AVCodec:   FFmpeg类库支持的编解码器
     * AVFilter:  FFmpeg类库支持的滤镜
     */
    public static native String getFFmpegSupportProtocol();

    public static native String getFFmpegSupportAVFormat();

    public static native String getFFmpegSupportAVCodec();

    public static native String getFFmpegSupportAVFilter();

    public synchronized static native int executeFFmpegCommand(String command, OnActionListener onActionListener);

    public static native int saveVideo2Ppm(String input_path, String output_path);

    public static native int saveVideo2Yuv(String input_path, String output_path);

    public static native int avioReading(String input_path);

    public static native int filterAudio(float duration);

    public static native int getMetadata(String input_path);

    public static native int resampling_audio(String output_path);

    public static native int avioDirCmd(String output_path);

    public static native int scalingVideo(String dst_filename, String dst_size);

    public static native int extractMvs(String input_filename);

    public static native int httpMulticlient(String in_uri, String out_uri);

    public static native int remuxing(String in_filename, String out_filename);

    public static native int demuxingDecoding(String src_filename, String video_dst_filename, String audio_dst_filename);

    public static native int muxing(String output_filename);

    public static native int transcoding(String input_path, String output_path);


    /**
     * @param videoRecordParams 录制参数
     * @return 录制的时间, 单位毫秒, 小于0启动录制失败
     */
    public static native long startRecord(VideoRecordParams videoRecordParams);

    public static native int addVideoData(long nativeHandle, byte[] data);

    public static native int addVideoPacketData(long nativeHandle, byte[] data, long size, long pts);

    public static native int addVideoData4Bitmap(long nativeHandle, Bitmap bitmap, int width, int height);

    public static native int updateVideoRecorderTexture(long nativeHandle, int textureId);

    /**
     * @return 当前录制的时间
     */
    public static native long addAudioData(long nativeHandle, byte[] data, int dataLength);


    public static native long getRecordTime(long nativeHandle);


    public static native int mergeVideoAndAudio(String videoPath, String audioPath, String outputPath);

    /**
     * 同步合并
     */
    public synchronized static native int mergeVideo(String[] inputPaths, String outPutPath, OnActionListener onActionListener);

    public static native int adjustPts(String srcVideo, String targetVideo);

    public static native int setStopRecordFlag(long nativeHandle);

    public static native int stopRecord(long nativeHandle);

    public static native int yuvToMp4(String yuvPath);

    public static native int audioEncoder();

    public static native int startAudioCapturer();

    public static native int addCapturerAudioData(byte[] data);

    public static native int stopAudioCapturer();

    public static native long initVideoTransCode();

    public static native int startVideoTransCode(long nativeHandle, VideoTransCodeParams videoTransCodeParams, OnVideoTransCodeListener onVideoTransCodeListener);

    public static native int stopVideoTransCode(long nativeHandle);

    public static native int recordAudioFrame(ShortBuffer audioBuffer, int size);

    public static native int addBackgroundMusic(String inputPath, String outputPath,
                                                String musicPath, float srcMusicVolume, float bgMusicVolume, OnActionListener onActionListener);

    public static native int mixAudios2Video(String outputPath, String videoStreamInputPath, String[] audios, OnActionListener onActionListener);

    /**
     * 在底层初始化GL环境
     *
     * @return Native Handle
     */
    public native static long initGLContext(int width, int height);

    public native static int releaseEGLContext(long nativeHandle);

    /**
     * 编码速度探测
     */
    public static native int encodeSpeedExplore();


    public static native int audioFadeIn(String videoPath, String outPath);

    public static native int getAudioFeatureInfo(String audioPath, int samples, AudioFeatureInfoListener audioFeatureInfoListener);

    /**
     * @param outImageParentPath 存放图片的父目录
     * @param imageCount         需要截图的数量,会自动平均截取的
     * @param scale2Width        把宽缩放到对应的值
     * @return >=0 success
     */
    public static native int getImageFromVideo(String videoPath, String outImageParentPath, int imageCount, int scale2Width, OnGetImageFromVideoListener onGetImageFromVideoListener);

    public static native int getBitmapFromVideo(String videoPath, int imageCount, int scale2Width, OnGetBitmapFromVideoListener onGetBitmapFromVideoListener);

    /**
     * @param time 需要截图的位置,单位毫秒
     * @return >=0 success
     */
    public static native int getImageFromVideoAtTime(String videoPath, String outImagePath, long time);


    public static native void cropYUV(byte src[], byte dis[], int srcWidth, int srcHeight, int startX, int startY, int disWidth, int disHeight);

    public static native long initCropTexture();

    public static native void cropTextureOnPause(long handle);

    public static native int cropTexture(long handle, int srcTexture, int srcWidth, int srcHeight,
                                         int startX, int startY, int targetWidth, int targetHeight);

    public static native int cropTextureRelease(long handle);


    public interface OnGetImageFromVideoListener {
        void onGetImageFromVideo(int index, String imagePath);
    }

    public interface OnGetBitmapFromVideoListener {
        void onGetBitmapFromVideo(int index, Bitmap bitmap);
    }

    /**
     * 视频录制的像素格式,目前支持两种
     * yv12 是YUV排列,只不过,UV是反的
     * nv21 是正常的YUV420p像素格式
     */
    public static enum PixelFormat {
        YV12, NV21, TEXTURE, RGBA
    }


    public static native long initSaveMultiInputVideo();

    public static native int startSaveMultiInputVideo(long nativeHandle, String[] videoPaths, String outputPath,
                                                      int type, OnMultiInputVideoSaveListener onMultiInputVideoSaveListener);

    public static native int stopSaveMultiInputVideo(long nativeHandle);

    public static enum MultiInputVideoLayoutType {
        INPUTS_1_NORMAL, INPUTS_1_CIRCLE, INPUTS_1_RHOMBUS, INPUTS_2_H, INPUTS_2_V, INPUTS_3_H, INPUTS_3_V,
        INPUTS_4, INPUTS_9
    }

    public static native long initParticlePathManager();

    public static native int releaseParticlePathManager(long nativeHandle);

    public static native void particlesPathManagerRemoveCurrent(long nativeHandle);

    public static native int particlesPathManagerRevertCurrent(long nativeHandle);

    public static native long particlesOnSurfaceCreated4CachePath(long pathManagerNativeHandle, int index);


    public static native long particlesOnSurfaceCreated(ParticleBean particleBean, long pathManagerNativeHandle, boolean needRecordPath);

    public static native void particlesOnSurfaceChanged(long particleEngineNativeHandel, int x, int y, int width, int height);

    /**
     * @param pts 当前视频的pts -2,结束粒子轨迹录制,-1 失败,0 重复绘制,>0正常绘制
     */
    public static native void particlesOnDrawFrame(long particleEngineNativeHandel, long pts);

    public static native void particlesTouchEvent(long particleEngineNativeHandel, float touchX, float touchY);

    public static native int particlesSeek(long particleEngineNativeHandel, long pts);

    public static native void particlesEnableAddParticle(long particleEngineNativeHandel, boolean enable);

    public static native void particlesOnRelease(long particleEngineNativeHandel);

    public static native long replaceVideoSegment(String srcVideo, String segmentVideo, String outputVideo, long startPts, long endPts);

    public static native int adjustVideoSpeed(String srcVideoPath, String outputPath, float speed);


    public static native int clipAudio(String audioPath, String outPath, long startTime,
                                       long endTime);

    public static native int clipVideo(String videoPath, String outPath, long startTime,
                                       long endTime);

    public enum FilterType {
        NORMAL, COLOR_INVERT, COLOR_MATRIX, CROSS_HATCH, BEAUTY_FACE,
        WHITE_BALANCE, COLOR_LEVEL, RGB, VIGNETTE, CONTRAST,
        COLOR_BALANCE, ADD_IMAGE, BRIGHTNESS, LOOK_UP, LOOK_UP_1,
        LOOK_UP_2, LOOK_UP_3, LOOK_UP_4, LOOK_UP_5, LOOK_UP_6,
        LOOK_UP_7, LOOK_UP_8, LOOK_UP_9, LOOK_UP_10, LOOK_UP_11,
        LOOK_UP_12, LOOK_UP_13, LOOK_UP_14
    }

    /**
     * call from native
     *
     * @param imageName      在资源目录下的图片名字,如果是自己的bitmap的话,转码的时候imageName就设置成null
     *                       然后通过回调函数自己给bitmap
     * @param videoRotate    视频原始旋转的角度
     * @param flipHorizontal >0水平翻转
     * @param flipVertical   >0垂直翻转
     * @return ImageTextureID
     */
    public static int getImageTextureByName(String imageName, int videoRotate, int flipHorizontal, int flipVertical) {
        if (null == context) return -1;
        try {
            BZLogUtil.d(TAG, "videoRotate=" + videoRotate + "--flipHorizontal=" + flipHorizontal + "--flipVertical=" + flipVertical + "--imageName=" + imageName);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE7);
            Bitmap bitmap;
            boolean needRecycle;
//            if (BZStringUtils.isEmpty(imageName) && null != onLoadImageListener) {
//                bitmap = onLoadImageListener.loadImage();
//                needRecycle = false;
//            } else {
            needRecycle = true;
            bitmap = BitmapFactory.decodeStream(context.getAssets().open("filter/" + imageName));
//            }
            if (null == bitmap) return -1;

            Matrix matrix = new Matrix();
            matrix.postRotate(videoRotate);
            if (flipHorizontal > 0)
                matrix.postScale(-1, 1);   //镜像水平翻转
            if (flipVertical > 0)
                matrix.postScale(1, -1);   //镜像垂直翻转

            Bitmap result = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            int texture = BZOpenGlUtils.loadTexture(result, BZOpenGlUtils.NO_TEXTURE, false);
            if (result != bitmap)
                result.recycle();
            if (needRecycle)
                bitmap.recycle();
            return texture;
        } catch (Exception e) {
            BZLogUtil.e(TAG, e);
            e.printStackTrace();
        }
        return -1;
    }

    private static final int CALLBACK_TYPE_CMD = 0;
    private static final int CALLBACK_TYPE_VIDEO_MERGE = 10;
    private static final int CALLBACK_TYPE_VIDEO_FILTER = 11;
    private static final int CALLBACK_TYPE_ADD_BACKGROUND_MUSIC = 12;
    private static final int CALLBACK_TYPE_CLOSE_VIDEO_AUDIO = 13;
    private static final int CALLBACK_TYPE_REPLACE_BACKGROUND_MUSIC = 14;
    private static final int CALLBACK_TYPE_VIDEO_TRANSCODE = 15;
    private static final int CALLBACK_TYPE_VIDEO_BACK_AND_FORTH = 16;


    private static final int CALLBACK_WHAT_MESSAGE_ERROR = 0;
    private static final int CALLBACK_WHAT_MESSAGE_PROGRESS = 1;
    private static final int CALLBACK_WHAT_MESSAGE_FINISH = 2;

    /**
     * call from native
     */
    private static void exploreVideoParame(int rate, int videoWidth) {
        if (null == sp) {
            return;
        }
        VideoRecorderBase.EXPECT_RECORD_RATE = rate;
        VideoRecorderBase.EXPECT_RECORD_WIDTH = videoWidth;
        BZLogUtil.d(TAG, "exploreVideoParame--rate=" + rate + "--videoWidth=" + videoWidth);

        SharedPreferences.Editor edit = sp.edit();
        edit.putInt(KEY_VIDEO_EXPLORE_RATE, rate);
        edit.putInt(KEY_VIDEO_EXPLORE_WIDTH, videoWidth);
        edit.apply();
    }

    public static final int MEDIA_INFO_WHAT_VIDEO_DURATION = 1;
    public static final int MEDIA_INFO_WHAT_VIDEO_ROTATE = 2;
    public static final int MEDIA_INFO_WHAT_VIDEO_WIDTH = 3;
    public static final int MEDIA_INFO_WHAT_VIDEO_HEIGHT = 4;

    public native static int getVideoWidth(String videoPath);

    public native static int getVideoHeight(String videoPath);

    public native static int getVideoRotate(String videoPath);

    public native static long getMediaDuration(String mediaPath);

    public native static int getVideoInfo(String videoPath, OnSendMediaInfoListener sendMediaInfoListener);

    public native static int printVideoTimeStamp(String videoPath);

    public native static int printVideoFrameInfo(String videoPath);

    public native static void packetReplaceTest();

    public static native int getGifFromVideo(String videoPath, String gifOutputPath, GifAttribute gifAttribute);

    public synchronized static int parseVideo4Gif(String videoPath, int fps, OnGifBitmapParseListener onGifBitmapParseListener) {
        return parseVideo4Gif(videoPath, fps, -1, onGifBitmapParseListener);
    }

    public synchronized static native int alignmentMusic2Video(String videoPath, String musicPath, String outputPath);

    public static native int parseVideo4Gif(String videoPath, int fps, int frameCount, OnGifBitmapParseListener onGifBitmapParseListener);


    public static native int adjustGifSpeed(String gifSrcPath, String gifOutPath, float speed);

    public static native long initGifEncoder(String gifPath, int width, int height, int frameDuration, int quality);

    public static int addGifData(long nativeHandle, Bitmap bitmap) {
        if (null == bitmap || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0 || bitmap.isRecycled()) {
            BZLogUtil.d(TAG, "null == bitmap || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0|| bitmap.isRecycled()");
            return -1;
        }
        return nativeAddGifData(nativeHandle, bitmap, bitmap.getWidth(), bitmap.getHeight());
    }

    private static native int nativeAddGifData(long nativeHandle, Bitmap bitmap, int width, int height);

    public static native int stopGifEncoder(long nativeHandle);


    public static native boolean videoIsSupport(String videoPath, boolean isSoftDecode);

    public static native boolean audioIsSupport(String audioPath);


    public synchronized static native int closeVideoAudio(String inputPath, String outputPath, OnActionListener onActionListener);

    public synchronized static native int replaceBackgroundMusic(String videoPath, String musicPath, String outputPath, OnActionListener onActionListener);

    public synchronized static native int handleBackAndForth(String inputPath, String outputPath, float speed, OnActionListener onActionListener);

    public static native int stopHandleBackAndForth();

    public static native long[] getVideoPts(String videoPath);

    public static native float getVideoAverageDuration(String videoPath);

    public static Bitmap bzReadPixels(int startX, int startY, int width, int height) {
        Bitmap bitmap = bzReadPixelsNative(startX, startY, width, height);
        try {
            Matrix matrix = new Matrix();
            matrix.postScale(1, -1); //镜像垂直翻转
            Bitmap bitmapTemp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            Canvas canvas = new Canvas(bitmapTemp);
            canvas.drawBitmap(bitmap, 0, 0, new Paint());

            Bitmap bitmapFinal = Bitmap.createBitmap(bitmapTemp, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            bitmapTemp.recycle();
            bitmap.recycle();
            return bitmapFinal;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private static native Bitmap bzReadPixelsNative(int startX, int startY, int width, int height);

    public interface OnSendMediaInfoListener {
        void sendMediaInfo(int what, int extra);
    }


    public interface OnVideoTransCodeListener {
        int onTextureCallBack(int textureId, int width, int height, long pts, long videoTime);

        byte[] onPcmCallBack(byte[] pcmData);

        void videoTransCodeProgress(float progress);

        void videoTransCodeFinish();
    }

    public interface OnMultiInputVideoSaveListener {
        int onTextureCallBack(int textureId, int width, int height, long pts, long videoTime);

        void onGLContextWillDestroy();
    }


    public interface OnVideoStopRecoderListener {
        void onVideoStopRecoder();
    }

    public interface OnActionListener {
        void progress(float progress);

        void fail();

        void success();
    }

    public interface OnGifBitmapParseListener {
        void onBitmapParseSuccess(Bitmap bitmap);
    }

    public interface AudioFeatureInfoListener {
        void onAudioFeatureInfo(long audioTime, float featureValue);
    }
}
