package com.ufotosoft.bzmedia.recorder;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.SurfaceHolder;

import com.ufotosoft.bzmedia.BZMedia;
import com.ufotosoft.bzmedia.utils.BZDeviceUtils;
import com.ufotosoft.bzmedia.utils.BZFileUtils;
import com.ufotosoft.bzmedia.utils.BZLogUtil;
import com.ufotosoft.bzmedia.utils.BZStringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.ufotosoft.bzmedia.recorder.OnRecorderErrorListener.ERROR_CAMERA_AUTO_FOCUS;
import static com.ufotosoft.bzmedia.recorder.OnRecorderErrorListener.ERROR_CAMERA_PREVIEW;
import static com.ufotosoft.bzmedia.recorder.OnRecorderErrorListener.ERROR_CAMERA_SET_PREVIEW_DISPLAY;

/**
 * Created by zhandalin on 2017-04-21 15:45.
 * 说明:视频录制
 */
public abstract class VideoRecorderBase implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public final static String TAG = "bz_VideoRecorder";
    public final static String VIDOE_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/JustShot/Video/temp/";
    /**
     * 预期能录制的宽,这个值只会被native修改,其它地方不得修改
     * 目前取值会有1080,720,480
     */
    public static int EXPECT_RECORD_WIDTH = 1080;

    /**
     * 预期能录制的视频帧率,这个值只会被native修改,其它地方不得修改
     * 目前取值有24,15
     */
    public static int EXPECT_RECORD_RATE = 15;


    int mRecordWidth = EXPECT_RECORD_WIDTH, mRecordHeight = EXPECT_RECORD_WIDTH;

    static int PREVIEW_WIDTH = EXPECT_RECORD_WIDTH;
    static int PREVIEW_HEIGHT = EXPECT_RECORD_WIDTH;


    /**
     * 开始转码
     */
    protected static final int MESSAGE_ENCODE_START = 0;
    /**
     * 转码进度
     */
    protected static final int MESSAGE_ENCODE_PROGRESS = 1;
    /**
     * 转码完成
     */
    protected static final int MESSAGE_ENCODE_COMPLETE = 2;
    /**
     * 转码失败
     */
    protected static final int MESSAGE_ENCODE_ERROR = 3;

    protected static final int START_RECORD = 20;

    //TODO 优化
    protected static List<RecorderItem> recorderItemList = new ArrayList<>();

    protected boolean synEncode = false;
    /**
     * 摄像头对象
     */
    private Camera camera;
    /**
     * 摄像头参数
     */
    private Camera.Parameters mParameters = null;

    /**
     * 画布
     */
    private SurfaceHolder mSurfaceHolder;

    /**
     * 录制错误监听
     */
    OnRecorderErrorListener mOnRecorderErrorListener;
    /**
     * 录制已经准备就绪的监听
     */
    private OnPreparedListener mOnPreparedListener;

    /**
     * 摄像头类型（前置/后置），默认后置
     */
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    /**
     * 状态标记
     */
    private boolean mPrepared, mStartPreview, mSurfaceCreated;
    /**
     * 是否正在录制
     */
    volatile boolean mRecording;

    volatile boolean mediaMuxerStarted;
    /**
     * PreviewFrame调用次数，测试用
     */
    private volatile long mPreviewFrameCallCount = 0;
    private PreviewFrameCallBackListener previewFrameCallBackListener;

    private boolean isNexusPhone;
    private SurfaceTexture surfaceTexture;

    //拍照最小尺寸
    private static final int mPictureWidth = 1000;
    private static final int mPictureHeight = 1000;
    //实际录制的帧率
    int mFrameRate = 30;
    //视频旋转角度
    int videoRotate = 0;
    BZMedia.PixelFormat pixelFormat = BZMedia.PixelFormat.TEXTURE;
    private Point lastRecordRatio = new Point(16, 9);
    private volatile boolean isMergeVideoing;
    protected GLSurfaceView glSuerfaceView = null;
    protected float recordSpeed = 1.0f;
    protected boolean avPacketFromMediaCodec = false;

    //第一帧不要
    protected boolean isFirstFrame = true;

    protected Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case START_RECORD:
                    handler.removeMessages(START_RECORD);
                    BZLogUtil.d(TAG, "handler 执行录制");
                    if (mRecording) {
                        Message message = new Message();
                        message.what = START_RECORD;
                        message.obj = msg.obj;
                        handler.sendMessageDelayed(message, 1000);
                        BZLogUtil.d(TAG, "有线程在录制视频稍后启动录制");
                    } else {
                        startRecord((String) msg.obj);
                    }
                    break;
            }
            return true;
        }
    });
    protected OnVideoRecorderStateListener onVideoRecorderStateListener;

    protected boolean needFlipVertical;
    protected boolean needAudio = true;
    //在这里配置即可生效
    protected boolean allFrameIsKey = false;
    protected long videoBitRate = -1;
    //每一帧的持续时间
    protected int frameDuration = 33;
    protected long lastUpdateTextureTime;
    protected OnRecordPCMListener onRecordPCMListener;
    protected boolean recordFromCamera = true;

    public VideoRecorderBase() {
        initSeting();
    }

    public VideoRecorderBase(GLSurfaceView glSuerfaceView) {
        this.glSuerfaceView = glSuerfaceView;
        initSeting();
    }

    private void initSeting() {
//        if (null != Build.MODEL)
//            isNexusPhone = Build.MODEL.contains("Nexus") || Build.MODEL.contains("nexus");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            pixelFormat = BZMedia.PixelFormat.TEXTURE;
        }
    }

    /**
     * 设置预览输出SurfaceHolder
     *
     * @param sh
     */
    @SuppressWarnings("deprecation")
    public void setSurfaceHolder(SurfaceHolder sh) {
        if (sh != null) {
            sh.addCallback(this);
            if (!BZDeviceUtils.hasHoneycomb()) {
                sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        }
    }

    /**
     * 设置预处理监听
     */
    public void setOnPreparedListener(OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    /**
     * 设置错误监听
     */
    public void setOnRecorderErrorListener(OnRecorderErrorListener onRecorderErrorListener) {
        mOnRecorderErrorListener = onRecorderErrorListener;
    }

    /**
     * 是否前置摄像头
     */
    public boolean isFrontCamera() {
        return mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    /**
     * 是否支持前置摄像头
     */
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static boolean isSupportFrontCamera() {
        if (!BZDeviceUtils.hasGingerbread()) {
            return false;
        }
        int numberOfCameras = Camera.getNumberOfCameras();
        if (2 == numberOfCameras) {
            return true;
        }
        return false;
    }


    public void setRecordRatio(Point point) {
        if (null == point || point.x == 0 || point.y == 0) return;

        lastRecordRatio = point;

        float result = 1.0f * point.x / point.y;

        //用短边来算,防止溢出
        int temp = Math.min(PREVIEW_HEIGHT, PREVIEW_WIDTH);

        //由于Android是倒置的,要注意
        if (result == 1 / 1.0f) {//1:1
            mRecordWidth = mRecordHeight = temp;
        } else if (result == 4 / 3.0f) {//4:3
            mRecordWidth = 3 * temp / 4;
            mRecordHeight = temp;
        } else if (result == 3 / 4.0f) {//3:4
            mRecordWidth = temp;
            mRecordHeight = 3 * temp / 4;
        } else if (result == 16 / 9.0f) {//16:9
            mRecordWidth = 9 * temp / 16;
            mRecordHeight = temp;
        }
        BZLogUtil.d(TAG, "preview_width=" + PREVIEW_WIDTH + "---mRecordWidth=" + mRecordWidth + "---preview_height=" + PREVIEW_HEIGHT + "---mRecordHeight=" + mRecordHeight);
    }

    /**
     * 切换前置/后置摄像头
     */
    public void switchCamera(int cameraFacingFront) {
        switch (cameraFacingFront) {
            case Camera.CameraInfo.CAMERA_FACING_FRONT:
            case Camera.CameraInfo.CAMERA_FACING_BACK:
                mCameraId = cameraFacingFront;
                stopPreview();
                if (null != mSurfaceHolder)
                    startPreview();
                else
                    startPreview(surfaceTexture);
                break;
        }
    }

    /**
     * 切换前置/后置摄像头
     */
    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            switchCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        } else {
            switchCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
    }

    /**
     * 切换前置/后置摄像头
     */
    public void switchCamera(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
        switchCamera();
    }

    /**
     * 自动对焦
     *
     * @param cb
     * @return
     */
    public boolean autoFocus(Camera.AutoFocusCallback cb) {
        if (camera != null) {
            try {
                camera.cancelAutoFocus();

                if (mParameters != null) {
                    String mode = getAutoFocusMode();
                    if (BZStringUtils.isNotEmpty(mode)) {
                        mParameters.setFocusMode(mode);
                        camera.setParameters(mParameters);
                    }
                }
                camera.autoFocus(cb);
                return true;
            } catch (Exception e) {
                if (mOnRecorderErrorListener != null) {
                    mOnRecorderErrorListener.onVideoError(ERROR_CAMERA_AUTO_FOCUS, 0);
                }
                BZLogUtil.e("autoFocus", e);
            }
        }
        return false;
    }

    /**
     * 连续自动对焦
     */
    private String getAutoFocusMode() {
        if (mParameters != null) {
            //持续对焦是指当场景发生变化时，相机会主动去调节焦距来达到被拍摄的物体始终是清晰的状态。
            List<String> focusModes = mParameters.getSupportedFocusModes();
            if ((Build.MODEL.startsWith("GT-I950") || Build.MODEL.endsWith("SCH-I959") || Build.MODEL.endsWith("MEIZU MX3")) && isSupported(focusModes, "continuous-picture")) {
                return "continuous-picture";
            } else if (isSupported(focusModes, "continuous-video")) {
                return "continuous-video";
            } else if (isSupported(focusModes, "auto")) {
                return "auto";
            }
        }
        return null;
    }

    /**
     * 手动对焦
     *
     * @param focusAreas 对焦区域
     * @return
     */
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public boolean manualFocus(Camera.AutoFocusCallback cb, List<Camera.Area> focusAreas) {
        if (camera != null && focusAreas != null && mParameters != null && BZDeviceUtils.hasICS()) {
            try {
                camera.cancelAutoFocus();
                // getMaxNumFocusAreas检测设备是否支持
                if (mParameters.getMaxNumFocusAreas() > 0) {
                    // mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);//
                    // Macro(close-up) focus mode
                    mParameters.setFocusAreas(focusAreas);
                }

                if (mParameters.getMaxNumMeteringAreas() > 0)
                    mParameters.setMeteringAreas(focusAreas);

                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                camera.setParameters(mParameters);
                camera.autoFocus(cb);
                return true;
            } catch (Exception e) {
                if (mOnRecorderErrorListener != null) {
                    mOnRecorderErrorListener.onVideoError(ERROR_CAMERA_AUTO_FOCUS, 0);
                }
                BZLogUtil.e(TAG, "autoFocus", e);
            }
        }
        return false;
    }

    /**
     * 切换闪关灯，默认关闭
     */
    public boolean toggleFlashMode() {
        if (mParameters != null) {
            try {
                final String mode = mParameters.getFlashMode();
                if (TextUtils.isEmpty(mode) || Camera.Parameters.FLASH_MODE_OFF.equals(mode))
                    setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                else
                    setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                return true;
            } catch (Exception e) {
                BZLogUtil.e(TAG, "toggleFlashMode", e);
            }
        }
        return false;
    }

    /**
     * 设置闪光灯
     */
    private boolean setFlashMode(String value) {
        if (mParameters != null && camera != null) {
            try {
                if (Camera.Parameters.FLASH_MODE_TORCH.equals(value) || Camera.Parameters.FLASH_MODE_OFF.equals(value)) {
                    mParameters.setFlashMode(value);
                    camera.setParameters(mParameters);
                }
                return true;
            } catch (Exception e) {
                BZLogUtil.e(TAG, "setFlashMode", e);
            }
        }
        return false;
    }

    /**
     * 开始预览
     */
    public void prepare() {
        mPrepared = true;
        if (mSurfaceCreated)
            startPreview();
    }

    /**
     * 开始预览
     */
    public void prepare(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
        mPrepared = true;
        startPreview(surfaceTexture);
    }


    /**
     * 检测是否支持指定特性
     */
    private boolean isSupported(List<String> list, String key) {
        return list != null && list.contains(key);
    }

    /**
     * 预处理一些拍摄参数
     * 注意：自动对焦参数cam_mode和cam-mode可能有些设备不支持，导致视频画面变形，需要判断一下，已知有"GT-N7100", "GT-I9308"会存在这个问题
     */
    private void prepareCameraParaments() {
        if (mParameters == null)
            return;

        List<Integer> rates = mParameters.getSupportedPreviewFrameRates();
        if (rates != null) {
            if (rates.contains(EXPECT_RECORD_RATE)) {
                mFrameRate = EXPECT_RECORD_RATE;
            } else {
                Collections.sort(rates);
                for (int i = rates.size() - 1; i >= 0; i--) {
                    if (rates.get(i) <= EXPECT_RECORD_RATE) {
                        mFrameRate = rates.get(i);
                        break;
                    }
                }
            }
        }
        //容错处理
        if (null != rates && rates.size() > 0 && !rates.contains(mFrameRate)) {
            mFrameRate = rates.get(rates.size() - 1);
        }
        mParameters.setPreviewFrameRate(mFrameRate);
        List<Camera.Size> mSupportedPreviewSizes = mParameters.getSupportedPreviewSizes();//	获取支持的尺寸
        Collections.sort(mSupportedPreviewSizes, comparatorBigger);
        Camera.Size preSizes = null;
        for (Camera.Size previewSize : mSupportedPreviewSizes) {
//            BZLogUtil.d(TAG, String.format(Locale.SIMPLIFIED_CHINESE, "Supported Preview size: %d x %d", previewSize.width, previewSize.height));
            if (preSizes == null || (previewSize.width >= EXPECT_RECORD_WIDTH && previewSize.height >= EXPECT_RECORD_WIDTH)) {
                preSizes = previewSize;
            }
        }
        if (null != preSizes) {
            PREVIEW_WIDTH = preSizes.width;
            PREVIEW_HEIGHT = preSizes.height;
            setRecordRatio(lastRecordRatio);
            BZLogUtil.d(TAG, "preview_width=" + PREVIEW_WIDTH + "--preview_height=" + PREVIEW_HEIGHT);
        }

        mParameters.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);

        List<Camera.Size> picSizes = mParameters.getSupportedPictureSizes();
        Camera.Size picSz = null;

        Collections.sort(picSizes, comparatorBigger);
        for (Camera.Size sz : picSizes) {
//            BZLogUtil.d(TAG, String.format(Locale.SIMPLIFIED_CHINESE, "Supported picture size: %d x %d", sz.width, sz.height));
            if (picSz == null || (sz.width >= mPictureWidth && sz.height >= mPictureHeight)) {
                picSz = sz;
            }
        }
        if (null != picSz) {
            mParameters.setPictureSize(picSz.width, picSz.height);
            mParameters.setPictureFormat(ImageFormat.JPEG);
        }

        // 设置输出视频流尺寸，采样率
        mParameters.setPreviewFormat(ImageFormat.YV12);

        //设置自动连续对焦
        String mode = getAutoFocusMode();
        if (BZStringUtils.isNotEmpty(mode)) {
            mParameters.setFocusMode(mode);
        }

        //设置人像模式，用来拍摄人物相片，如证件照。数码相机会把光圈调到最大，做出浅景深的效果。而有些相机还会使用能够表现更强肤色效果的色调、对比度或柔化效果进行拍摄，以突出人像主体。
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT && isSupported(mParameters.getSupportedSceneModes(), Camera.Parameters.SCENE_MODE_PORTRAIT))
            mParameters.setSceneMode(Camera.Parameters.SCENE_MODE_PORTRAIT);

        if (isSupported(mParameters.getSupportedWhiteBalance(), "auto"))
            mParameters.setWhiteBalance("auto");

        //是否支持视频防抖
        if ("true".equals(mParameters.get("video-stabilization-supported")))
            mParameters.set("video-stabilization", "true");

        //		mParameters.set("recording-hint", "false");
        //
        //		mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        if (!BZDeviceUtils.isDevice("GT-N7100", "GT-I9308", "GT-I9300")) {
            mParameters.set("cam_mode", 1);
        }
    }

    //保证从大到小排列
    private Comparator<Camera.Size> comparatorBigger = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            int h = rhs.height - lhs.height;
            if (h == 0)
                return rhs.width - lhs.width;
            return h;
        }
    };


    /**
     * 开始预览
     */
    public void startPreview(SurfaceTexture surfaceTexture) {
        if (null == surfaceTexture) return;
        this.surfaceTexture = surfaceTexture;
        mStartPreview = true;
        try {
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
                camera = Camera.open();
            else
                camera = Camera.open(mCameraId);

            try {
                camera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                if (mOnRecorderErrorListener != null) {
                    mOnRecorderErrorListener.onVideoError(ERROR_CAMERA_SET_PREVIEW_DISPLAY, 0);
                }
                BZLogUtil.e(TAG, "setPreviewDisplay fail " + e.getMessage());
            }

            //设置摄像头参数
            mParameters = camera.getParameters();
            prepareCameraParaments();

            if (isFrontCamera() && isNexusPhone) {//google系列手机特殊处理
                camera.setDisplayOrientation(270);
                mParameters.setRotation(270);
            } else {
                camera.setDisplayOrientation(90);
                mParameters.setRotation(90);
            }

            camera.setParameters(mParameters);
            setPreviewCallback();
            camera.startPreview();

            onStartPreviewSuccess();
            if (mOnPreparedListener != null)
                mOnPreparedListener.onPrepared();
        } catch (Exception e) {
            e.printStackTrace();
            if (mOnRecorderErrorListener != null) {
                mOnRecorderErrorListener.onVideoError(ERROR_CAMERA_PREVIEW, 0);
            }
            BZLogUtil.e(TAG, "startPreview fail :" + e.getMessage());
        }
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        if (mStartPreview || mSurfaceHolder == null || !mPrepared)
            return;
        else
            mStartPreview = true;

        try {

            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
                camera = Camera.open();
            else
                camera = Camera.open(mCameraId);


            try {
                camera.setPreviewDisplay(mSurfaceHolder);
            } catch (IOException e) {
                if (mOnRecorderErrorListener != null) {
                    mOnRecorderErrorListener.onVideoError(ERROR_CAMERA_SET_PREVIEW_DISPLAY, 0);
                }
                BZLogUtil.e(TAG, "setPreviewDisplay fail " + e.getMessage());
            }

            //设置摄像头参数
            mParameters = camera.getParameters();
            prepareCameraParaments();
//            BZLogUtil.d(TAG," Build.MODEL="+ Build.MODEL+"\tBuild.MANUFACTURER="+Build.MANUFACTURER);


            if (isFrontCamera() && isNexusPhone) {//google系列手机特殊处理
                camera.setDisplayOrientation(270);
                mParameters.setRotation(270);
            } else {
                camera.setDisplayOrientation(90);
                mParameters.setRotation(90);
            }
            camera.setParameters(mParameters);
            setPreviewCallback();
            camera.startPreview();

            onStartPreviewSuccess();
            if (mOnPreparedListener != null)
                mOnPreparedListener.onPrepared();
        } catch (Exception e) {
            e.printStackTrace();
            if (mOnRecorderErrorListener != null) {
                mOnRecorderErrorListener.onVideoError(ERROR_CAMERA_PREVIEW, 0);
            }
            BZLogUtil.e(TAG, "startPreview fail :" + e.getMessage());
        }
    }

    /**
     * 预览调用成功，子类可以做一些操作
     */
    protected void onStartPreviewSuccess() {

    }

    /**
     * 设置回调
     */
    private void setPreviewCallback() {
        Camera.Size size = mParameters.getPreviewSize();
        if (size != null) {
            int buffSize = size.width * size.height * 3 / 2;
            try {
                camera.addCallbackBuffer(new byte[buffSize]);
                camera.addCallbackBuffer(new byte[buffSize]);
                camera.addCallbackBuffer(new byte[buffSize]);
                camera.setPreviewCallbackWithBuffer(this);
            } catch (OutOfMemoryError e) {
                BZLogUtil.e(TAG, "startPreview...setPreviewCallback...", e);
            }
            BZLogUtil.e(TAG, "startPreview...setPreviewCallbackWithBuffer...width:" + size.width + " height:" + size.height);
        } else {
            camera.setPreviewCallback(this);
        }
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewCallback(null);
                // camera.lock();
                camera.release();
            } catch (Exception e) {
                BZLogUtil.e(TAG, "stopPreview...");
            }
            camera = null;
        }
        stopRecord();
        mStartPreview = false;
    }

    /**
     * 释放资源
     */
    public void release() {
        // 停止视频预览
        stopPreview();
        stopRecord();

        mSurfaceHolder = null;
        mPrepared = false;
        mSurfaceCreated = false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.mSurfaceHolder = holder;
        this.mSurfaceCreated = true;
        if (mPrepared && !mStartPreview)
            startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
        mSurfaceCreated = false;
    }

    public void onAudioError(int what, String message) {
        if (mOnRecorderErrorListener != null)
            mOnRecorderErrorListener.onAudioError(what, message);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (null != previewFrameCallBackListener && data.length > 0) {
            previewFrameCallBackListener.onPreviewFrameCallBack();
            previewFrameCallBackListener = null;
        }
        mPreviewFrameCallCount++;
        camera.addCallbackBuffer(data);
    }

    /**
     * 测试PreviewFrame回调次数，时间1分钟
     */
    private void testPreviewFrameCallCount() {
        new CountDownTimer(1 * 60 * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                BZLogUtil.e("[Vitamio Recorder]", "testFrameRate..." + mPreviewFrameCallCount);
                mPreviewFrameCallCount = 0;
            }

            @Override
            public void onFinish() {

            }

        }.start();
    }

    public void setAllFrameIsKey(boolean allFrameIsKey) {
        this.allFrameIsKey = allFrameIsKey;
    }

    public void recordFromCamera(boolean recordFromCamera) {
        this.recordFromCamera = recordFromCamera;
    }


    /**
     * 预处理监听
     */
    public interface OnPreparedListener {
        /**
         * 预处理完毕，可以开始录制了
         */
        void onPrepared();
    }

    /**
     * 转码接口
     */
    public interface OnMergeVideoListener {
        /**
         * 开始转码
         */
        void onMergeVideoStart();

        /**
         * 转码进度
         */
        void onMergeVideoProgress(float progress);

        /**
         * 转码完成
         *
         * @param outPutPath 视频最终的路径
         */
        void onMergeVideoComplete(String outPutPath);

        /**
         * 转码失败
         */
        void onMergeVideoError();
    }


    public interface TakePictureCallback {
        //传入的bmp可以由接收者recycle
        void takePictureOK(Bitmap bmp);
    }

    /**
     * 拍照
     */
    public synchronized void takePicture(final TakePictureCallback photoCallback, Camera.ShutterCallback shutterCallback, final boolean isFrontMirror) {
        if (null == camera) return;
        Camera.Parameters params = camera.getParameters();
        if (photoCallback == null || params == null) {
            BZLogUtil.e(TAG, "takePicture after release!");
            if (photoCallback != null) {
                photoCallback.takePictureOK(null);
            }
            return;
        }
        try {
            params.setRotation(90);
            camera.setParameters(params);
        } catch (Exception e) {
            BZLogUtil.e(TAG, "Error when takePicture: " + e.toString());
            photoCallback.takePictureOK(null);
            return;
        }
        camera.takePicture(shutterCallback, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                Camera.Parameters params = camera.getParameters();
                Camera.Size sz = params.getPictureSize();

                boolean shouldRotate;
                int width, height;
                //当拍出相片不为正方形时， 可以判断图片是否旋转
                //默认数据格式已经设置为 JPEG
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                width = bmp.getWidth();
                height = bmp.getHeight();
                shouldRotate = (sz.width > sz.height && width > height) || (sz.width < sz.height && width < height);

                Bitmap bmp2;
                if (shouldRotate) {
                    bmp2 = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);

                    Canvas canvas = new Canvas(bmp2);
                    if (!isFrontCamera()) {
                        Matrix mat = new Matrix();
                        int halfLen = Math.min(width, height) / 2;
                        mat.setRotate(90, halfLen, halfLen);
                        canvas.drawBitmap(bmp, mat, null);
                    } else {
                        Matrix mat = new Matrix();
                        if (isFrontMirror) {
                            mat.postTranslate(-width / 2, -height / 2);
                            mat.postScale(-1.0f, 1.0f);
                            mat.postTranslate(width / 2, height / 2);
                            int halfLen = Math.min(width, height) / 2;
                            mat.postRotate(90, halfLen, halfLen);
                        } else {
                            int halfLen = Math.max(width, height) / 2;
                            mat.postRotate(-90, halfLen, halfLen);
                        }

                        canvas.drawBitmap(bmp, mat, null);
                    }

                    bmp.recycle();
                } else {
                    if (!isFrontCamera()) {
                        bmp2 = bmp;
                    } else {
                        bmp2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bmp2);
                        Matrix mat = new Matrix();
                        if (isFrontMirror) {
                            mat.postTranslate(-width / 2, -height / 2);
                            mat.postScale(1.0f, -1.0f);
                            mat.postTranslate(width / 2, height / 2);
                        } else {
                            mat.postTranslate(-width / 2, -height / 2);
                            mat.postScale(-1.0f, -1.0f);
                            mat.postTranslate(width / 2, height / 2);
                        }
                        canvas.drawBitmap(bmp, mat, null);
                    }

                }
                photoCallback.takePictureOK(bmp2);
            }
        });
    }

    /**
     * @param exposure 0-99
     */
    public void setExposureCompensation(int exposure) {
        if (null == camera) return;
        try {
            Camera.Parameters parameters = camera.getParameters();
            int maxExposureCompensation = parameters.getMaxExposureCompensation();
            int minExposureCompensation = parameters.getMinExposureCompensation();

            HashMap<Integer, Integer> hashMap = new HashMap<>();
            int index = 0;
            for (int i = minExposureCompensation; i <= maxExposureCompensation; i++) {
                hashMap.put(index, i);
                index++;
            }
            int temp = hashMap.get(exposure * index / 100);
            BZLogUtil.d(TAG, "maxExpos=" + maxExposureCompensation + "--minExpo=" + minExposureCompensation + "---temp=" + temp);
            parameters.setExposureCompensation(temp);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 有的手机权限判断不了,通过这样的方式来判断
     */
    public interface PreviewFrameCallBackListener {
        void onPreviewFrameCallBack();
    }

    public void setPreviewFrameCallBackListener(PreviewFrameCallBackListener previewFrameCallBackListener) {
        this.previewFrameCallBackListener = previewFrameCallBackListener;
    }

    public boolean isNexusPhone() {
        return isNexusPhone;
    }

    public Camera getCamera() {
        return camera;
    }

    public int getRecordWidth() {
        return mRecordWidth;
    }

    public int getRecordHeight() {
        return mRecordHeight;
    }

    public int getPreviewWidth() {
        return PREVIEW_WIDTH;
    }

    public int getPreviewHeight() {
        return PREVIEW_HEIGHT;
    }

    public void setRecordWidth(int mRecordWidth) {
        this.mRecordWidth = mRecordWidth;
    }

    public void setRecordPixelFormat(BZMedia.PixelFormat pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    public void setRecordHeight(int mRecordHeight) {
        this.mRecordHeight = mRecordHeight;
    }

    public void setPreviewWidth(int preview_width) {
        PREVIEW_WIDTH = preview_width;
    }

    public void setPreviewHeight(int preview_height) {
        PREVIEW_HEIGHT = preview_height;
    }

    public void setVideoRate(int videoRate) {
        mFrameRate = videoRate;
        frameDuration = (int) (1000.0f / mFrameRate * recordSpeed);
    }

    public void setVideoRotate(int videoRotate) {
        this.videoRotate = videoRotate;
    }

    public void setVideoBitRate(long videoBitRate) {
        this.videoBitRate = videoBitRate;
    }

    public void updateTexture(int textureId) {

    }

    public void setNeedFlipVertical(boolean needFlipVertical) {
        this.needFlipVertical = needFlipVertical;
    }

    public void startRecord(final String outputPath) {
        startRecordInner(outputPath);
    }

    public long getBitRate() {
        if (videoBitRate <= 0) {//如果没有设置才自己计算
            //bit_rate 设置
            int bitrateP = Math.max(mRecordWidth, mRecordHeight);
            //以720p为标准,每秒1M
            long bit_rate = 1024 * 1024 * 8 * 2;//每秒1M;
            if (!allFrameIsKey) {//如果不全是关键帧,那么码率降低
                bit_rate = bit_rate / 3 * 2;
            }
            return (long) (bitrateP / 720.f * bit_rate);
        }
        return videoBitRate;
    }

    public void setNeedAudio(boolean needAudio) {
        this.needAudio = needAudio;
    }


    public abstract void startRecordInner(String outputPath);

    public abstract void stopRecord();

    /**
     * 1秒内点击多次只会有一次有效
     */
    public void startNewRecord(final String outputPath) {
        if (mRecording) {
            Message message = new Message();
            message.what = START_RECORD;
            message.obj = outputPath;
            handler.sendMessageDelayed(message, 1000);
        } else {
            startRecord(outputPath);
        }
    }

    public void setGLSuerfaceView(GLSurfaceView glSuerfaceView) {
        this.glSuerfaceView = glSuerfaceView;
    }

    public void removeLastRecordItem() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (recorderItemList.size() > 0) {
                    removeRecordItem(recorderItemList.size() - 1);
                }
            }
        });
    }

    public void clearRecordItem() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            recorderItemList.clear();
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    recorderItemList.clear();
                }
            });
        }
    }

    public void removeRecordItem(final int position) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (position < recorderItemList.size()) {
                recorderItemList.remove(position);
            }
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    recorderItemList.remove(position);
                }
            });
        }
    }

    public RecorderItem getLastRecordItem() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return null;
        }
        if (recorderItemList.size() > 0) {
            return getRecordItem(recorderItemList.size() - 1);
        }
        return null;
    }

    public RecorderItem getRecordItem(int position) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return null;
        }
        if (position < recorderItemList.size()) {
            return recorderItemList.get(position);
        }
        return null;
    }

    public void mergeVideo(String outPutPath, OnMergeVideoListener mOnMergeVideoListener) {
        BZLogUtil.d(TAG, "mergeVideo=" + outPutPath);
        if (recorderItemList.isEmpty() || null == outPutPath)
            return;

        if (null != mOnMergeVideoListener) {
            mOnMergeVideoListener.onMergeVideoStart();
        }
        while (mRecording) {
            try {
                Thread.sleep(30);
            } catch (Throwable e) {
                BZLogUtil.e(TAG, e);
            }
            BZLogUtil.v(TAG, "mRecording waiting_merge_video");
        }
        mergeVideoInner(outPutPath, mOnMergeVideoListener);
    }

    /**
     * @param synEncode 视频流是否同步编码
     */
    public void setSynEncode(boolean synEncode) {
        this.synEncode = synEncode;
    }

    private void mergeVideoInner(final String outPutPath, final OnMergeVideoListener mOnMergeVideoListener) {
        if (recorderItemList.isEmpty() || null == outPutPath || isMergeVideoing) {
            BZLogUtil.e(TAG, "isMergeVideoing 无效的重复调用");
            return;
        }
        isMergeVideoing = true;
        if (recorderItemList.size() == 1) {
            BZFileUtils.fileCopy(recorderItemList.get(0).getVideoPath(), outPutPath);
            if (null != mOnMergeVideoListener)
                mOnMergeVideoListener.onMergeVideoComplete(outPutPath);
            isMergeVideoing = false;
            return;
        }
        final String[] temp = new String[recorderItemList.size()];
        for (int i = 0; i < recorderItemList.size(); i++) {
            temp[i] = recorderItemList.get(i).getVideoPath();
        }
        BZMedia.mergeVideo(temp, outPutPath, new BZMedia.OnActionListener() {
            public void progress(final float progress) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != mOnMergeVideoListener)
                            mOnMergeVideoListener.onMergeVideoProgress(progress);
                    }
                });
            }

            @Override
            public void fail() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != mOnMergeVideoListener)
                            mOnMergeVideoListener.onMergeVideoError();
                        isMergeVideoing = false;
                    }
                });
            }

            @Override
            public void success() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != mOnMergeVideoListener)
                            mOnMergeVideoListener.onMergeVideoComplete(outPutPath);
                        isMergeVideoing = false;
                        BZLogUtil.d(TAG, "onMergeVideoComplete 如有必要请调用 clearRecordItem");
                    }
                });
            }
        });
    }

    /**
     * @return 录制的总时间
     */
    public long getRecordTime() {
        long recordTime = 0;
        for (RecorderItem recorderItem : recorderItemList) {
            recordTime += recorderItem.getVideoRecordTime();
        }
        return recordTime;
    }

    public float getRecordSpeed() {
        return recordSpeed;
    }

    public void setRecordSpeed(float recordSpeed) {
        this.recordSpeed = recordSpeed;
        frameDuration = (int) (1000.0f / mFrameRate * recordSpeed);
    }

    public interface OnVideoRecorderStateListener {
        void onVideoRecorderStarted(boolean success);

        void onVideoRecorderStopped(List<RecorderItem> recorderItemList, boolean success);
    }

    public void setOnVideoRecorderStateListener(OnVideoRecorderStateListener onVideoRecorderStateListener) {
        this.onVideoRecorderStateListener = onVideoRecorderStateListener;
    }

    public void setOnRecordPCMListener(OnRecordPCMListener onRecordPCMListener) {
        this.onRecordPCMListener = onRecordPCMListener;
    }

    public boolean isAvPacketFromMediaCodec() {
        return avPacketFromMediaCodec;
    }

    public void setAvPacketFromMediaCodec(boolean avPacketFromMediaCodec) {
        this.avPacketFromMediaCodec = avPacketFromMediaCodec;
    }
}
