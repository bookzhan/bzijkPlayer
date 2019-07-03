package com.ufotosoft.bzmedia.recorder;

import android.os.Build;
import android.text.TextUtils;

import com.ufotosoft.bzmedia.bean.VideoSize;
import com.ufotosoft.bzmedia.utils.BZCPUTool;
import com.ufotosoft.bzmedia.utils.BZLogUtil;
import com.ufotosoft.bzmedia.glutils.BZOpenGlUtils;
import com.ufotosoft.bzmedia.utils.BZSpUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhandalin on 2017-08-22 13:33.
 * 说明:根据手机CPU与手机版本,以及原始视频宽高给出合适的视频分辨率
 */
public class VideoTacticsManager {
    private static final int CPU_DIVIDE_VALUE_FLOOR = 1500000;//1.5GHz
    private static final int CPU_DIVIDE_VALUE_CEIL = 2100000;//3.1GHz
    private static final String TAG = "bz_VideoSize";

    private static int isHighEndModle = 0;
    /**
     * 联发科	Helio X30	IMG	PowerVR 7XTP-MT4	800MHz
     * 联发科	MT6797T	ARM	Mali T880 MP4	850MHz
     * 松果	Surge S1	ARM	Mali T860 MP4	922MHz
     */
    private static final String HARDWARE_SAMSUNG_PRE = "exynos";
    private static final String HARDWARE_HUAWEI_PRE = "kirin";
    private static final String HARDWARE_QUALCOMM_PRE_1 = "msm";
    private static final String HARDWARE_QUALCOMM_PRE_835 = "qcom";
    private static final String HARDWARE_QUALCOMM_PRE_2 = "apq";
    private static final String HARDWARE_MTK_PRE_1 = "helio";
    private static final String HARDWARE_MTK_PRE_2 = "mt";
    private static final String HARDWARE_SURGE_PRE = "surge";

    /**
     * @return false使用原生视频录制
     */
    public static boolean isUseFFmpegRecorder() {
        if (BZSpUtils.getBoolean("use_ffmpeg_new", false)) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return false;
        }
        if ((BZOpenGlUtils.detectOpenGLES30() && BZCPUTool.getNumberOfCPUCores() > 4 &&
                BZCPUTool.getMaxCpuFreq() >= CPU_DIVIDE_VALUE_CEIL && !isHighEndModle()) || Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return true;
        }
        return false;
    }

    /**
     * @return 最大分辨率为720p
     */
    private static VideoSize getFFmpegVideoSize(int width, int height) {
        BZLogUtil.d(TAG, "use ffmpeg recorder");
        if (width <= 0 || height <= 0) {
            BZLogUtil.e(TAG, "width <= 0 || height <= 0");
            return new VideoSize(480, 480);
        }
        int maxCpuFreq = BZCPUTool.getMaxCpuFreq();
        int targetWith = 720;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BZLogUtil.d(TAG, "android 4.3以下 版本 最大为480");
            targetWith = 480;//最大为480
            if (maxCpuFreq < CPU_DIVIDE_VALUE_FLOOR) {
                targetWith = 320;
                BZLogUtil.d(TAG, "cpu跟不上只支持320");
            }
        } else {
            if (BZOpenGlUtils.detectOpenGLES30()) {
                BZLogUtil.d(TAG, "支持OpenGL3.0");
                if (maxCpuFreq < CPU_DIVIDE_VALUE_FLOOR) {
                    targetWith = 480;
                    BZLogUtil.d(TAG, "cpu跟不上只支持480");
                } else {
                    BZLogUtil.d(TAG, "maxCpuFreq=" + maxCpuFreq + "--720p");
                    targetWith = 720;
                }
//                慎重处理1080p的视频
//                else if (maxCpuFreq < CPU_DIVIDE_VALUE_CEIL) {
//                    BZLogUtil.d(TAG, "maxCpuFreq=" + maxCpuFreq + "--720p");
//                    targetWith = 720;
//                } else {
//                    if (BZCPUTool.getNumberOfCPUCores() <= 4) {
//                        BZLogUtil.d(TAG, "maxCpuFreq=" + maxCpuFreq + "--CPUCores=" + BZCPUTool.getNumberOfCPUCores() + "--720p");
//                        targetWith = 720;
//                    } else {
//                        BZLogUtil.d(TAG, "maxCpuFreq=" + maxCpuFreq + "--1080p");
//                        targetWith = 1080;
//                    }
//                }
            } else {
                BZLogUtil.d(TAG, "不支持 OpenGL3.0");
                if (maxCpuFreq < CPU_DIVIDE_VALUE_FLOOR) {
                    BZLogUtil.d(TAG, "cpu跟不上只支持320");
                    targetWith = 320;
                } else {
                    targetWith = 480;
                }
            }
        }
        if (targetWith > width) {
            targetWith = width;
            BZLogUtil.d(TAG, "targetWidth >width 取width值");
        }
        VideoSize videoSize = new VideoSize();
        videoSize.width = targetWith;
        videoSize.height = targetWith * height / width;

        if (videoSize.height > height) {
            BZLogUtil.d(TAG, "targetHeight >height 以height为准取值 即保持原值不变");
            videoSize.height = height;
            videoSize.width = width * height / height;
        }
        return videoSize;

    }

    /**
     * @return 按照一定规则判断是否是高端机型 白名单
     */
    private static boolean isHighEndModle() {
        if (isHighEndModle > 0) {
            return true;
        } else if (isHighEndModle < 0) {
            return false;
        }

        boolean res = false;
        String hardware = Build.HARDWARE;
        if (TextUtils.isEmpty(hardware)) {//获取不到处理器的
            return false;
        } else {
            hardware = hardware.toLowerCase();
        }

        int num = getNumFromString(hardware);
        if (hardware.contains(HARDWARE_SAMSUNG_PRE)) {
            if (num == 7420) {
                res = true;
            } else if (num == 8890) {
                res = true;
            } else if (num == 8895) {
                res = true;
            } else if (num == 9810) {
                res = true;
            }
        } else if (hardware.contains(HARDWARE_HUAWEI_PRE)) {
            if (num == 960) {
                res = true;
            } else if (num == 970) {
                res = true;
            } else if (num == 980) {
                res = true;
            }
        } else if (hardware.contains(HARDWARE_QUALCOMM_PRE_1)) {
            if (num == 8994) {
                res = true;
            } else if (num == 8996) {
                res = true;
            } else if (num == 8998) {
                res = true;
            }
        } else if (!hardware.contains(HARDWARE_QUALCOMM_PRE_2) && !hardware.contains(HARDWARE_MTK_PRE_1)) {
            if (hardware.contains(HARDWARE_MTK_PRE_2)) {
                if (num == 6799) {
                    res = true;
                }
            } else if (hardware.contains(HARDWARE_QUALCOMM_PRE_835)) {
                res = true;
            }
        }
        boolean isHighDevice = res && BZCPUTool.getMaxCpuFreq() > CPU_DIVIDE_VALUE_CEIL;
        if (isHighDevice) {
            isHighEndModle = 1;
        } else {
            isHighEndModle = -1;
        }
        return isHighDevice;

    }

    private static int getNumFromString(String s) {
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(s);
        String num = m.replaceAll("").trim();
        int res = 0;
        try {
            res = Integer.valueOf(num);
        } catch (Exception e) {
//            BZLogUtil.e(TAG, e);
        }
        return res;
    }


    private static VideoSize getMediaCodeVideoSize(int width, int height) {
        BZLogUtil.d(TAG, "use MediaCode recorder");
        if (width <= 0 || height <= 0) {
            BZLogUtil.e(TAG, "width <= 0 || height <= 0");
            return new VideoSize(480, 480);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BZLogUtil.e(TAG, "Build.VERSION is " + Build.VERSION.SDK_INT + " can't use MediaCode");
            return new VideoSize(480, 480);
        }
        int targetWith;
        if (BZCPUTool.getMaxCpuFreq() < CPU_DIVIDE_VALUE_FLOOR) {
            targetWith = 480;
        } else if (isHighEndModle()) {
            targetWith = 1080;
        } else {
            targetWith = 720;
        }

        VideoSize videoSize = new VideoSize();
        videoSize.width = targetWith;
        videoSize.height = targetWith * height / width;

        if (videoSize.height > height) {
            BZLogUtil.d(TAG, "targetHeight >height 以height为准取值 即保持原值不变");
            videoSize.height = height;
            videoSize.width = width * height / height;
        }
        return videoSize;
    }

    /**
     * android 4.3是一个大的区分点
     * android 4.3以下最大分辨率为480
     * android 4.3以上最大分辨率为1080
     * 不支持 OpenGL3.0最大分辨率为480
     */
    public static VideoSize getFitVideoSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            BZLogUtil.e(TAG, "width <= 0 || height <= 0");
            return new VideoSize(720, 720);
        }
        BZLogUtil.d(TAG, "input width=" + width + "--height=" + height);
        VideoSize videoSize;
        if (isUseFFmpegRecorder()) {
            videoSize = getFFmpegVideoSize(width, height);
        } else {
            videoSize = getMediaCodeVideoSize(width, height);
        }

        BZLogUtil.d(TAG, "final size width=" + videoSize.width + "--height=" + videoSize.height);
        return videoSize;
    }
}
