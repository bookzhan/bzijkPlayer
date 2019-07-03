package com.ufotosoft.bzmedia.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by zhandalin on 2017-04-20 18:43.
 * 说明:
 */
public class BZCPUTool {
    private final static String kCpuInfoMaxFreqFilePath = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";

    private static int maxCpuFreq = 0;

    private static int CPU_CORES = 0;
    /**
     * The default return value of any method in this class when an
     * error occurs or when processing fails (Currently set to -1). Use this to check if
     * the information about the device in question was successfully obtained.
     */
    private static final int DEVICEINFO_UNKNOWN = -1;

    /**
     * Reads the number of CPU cores from the first available information from
     * {@code /sys/devices/system/cpu/possible}, {@code /sys/devices/system/cpu/present},
     * then {@code /sys/devices/system/cpu/}.
     *
     * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
     */
    public static int getNumberOfCPUCores() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Gingerbread doesn't support giving a single application access to both cores, but a
            // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
            // chipset and Gingerbread; that can let an app in the background run without impacting
            // the foreground application. But for our purposes, it makes them single core.
            return 1;
        }
        if (CPU_CORES > 0) return CPU_CORES;

        int cores;
        try {
            cores = getCoresFromFileInfo("/sys/devices/system/cpu/possible");
            if (cores == DEVICEINFO_UNKNOWN) {
                cores = getCoresFromFileInfo("/sys/devices/system/cpu/present");
            }
            if (cores == DEVICEINFO_UNKNOWN) {
                cores = getCoresFromCPUFileList();
            }
        } catch (Throwable e) {
            cores = DEVICEINFO_UNKNOWN;
        }
        CPU_CORES = cores;
        return cores;
    }

    /**
     * Tries to read file contents from the file location to determine the number of cores on device.
     *
     * @param fileLocation The location of the file with CPU information
     * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
     */
    private static int getCoresFromFileInfo(String fileLocation) {
        InputStream is = null;
        try {
            is = new FileInputStream(fileLocation);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String fileContents = buf.readLine();
            buf.close();
            return getCoresFromFileString(fileContents);
        } catch (Throwable e) {
            return DEVICEINFO_UNKNOWN;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e) {
                    // Do nothing.
                }
            }
        }
    }

    /**
     * Converts from a CPU core information format to number of cores.
     *
     * @param str The CPU core information string, in the format of "0-N"
     * @return The number of cores represented by this string
     */
    static int getCoresFromFileString(String str) {
        if (str == null || !str.matches("0-[\\d]+$")) {
            return DEVICEINFO_UNKNOWN;
        }
        int cores = Integer.valueOf(str.substring(2)) + 1;
        return cores;
    }

    private static int getCoresFromCPUFileList() {
        return new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (!Character.isDigit(path.charAt(i))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    /**
     * Method for reading the clock speed of a CPU core on the device. Will read from either
     * {@code /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq} or {@code /proc/cpuinfo}.
     *
     * @return Clock speed of a core on the device, or -1 in the event of an error.
     */
    public static int getMaxCpuFreq() {
        if (maxCpuFreq > 0)
            return maxCpuFreq;
        int maxFreq = DEVICEINFO_UNKNOWN;
        try {
            for (int i = 0; i < getNumberOfCPUCores(); i++) {
                String filename =
                        "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
                File cpuInfoMaxFreqFile = new File(filename);
                if (cpuInfoMaxFreqFile.exists() && cpuInfoMaxFreqFile.canRead()) {
                    byte[] buffer = new byte[128];
                    FileInputStream stream = new FileInputStream(cpuInfoMaxFreqFile);
                    try {
                        stream.read(buffer);
                        int endIndex = 0;
                        //Trim the first number out of the byte buffer.
                        while (Character.isDigit(buffer[endIndex]) && endIndex < buffer.length) {
                            endIndex++;
                        }
                        String str = new String(buffer, 0, endIndex);
                        Integer freqBound = Integer.parseInt(str);
                        if (freqBound > maxFreq) {
                            maxFreq = freqBound;
                        }
                    } catch (Throwable e) {
                        //Fall through and use /proc/cpuinfo.
                    } finally {
                        stream.close();
                    }
                }
            }
            if (maxFreq == DEVICEINFO_UNKNOWN) {
                FileInputStream stream = new FileInputStream("/proc/cpuinfo");
                try {
                    int freqBound = parseFileForValue("cpu MHz", stream);
                    freqBound *= 1000; //MHz -> kHz
                    if (freqBound > maxFreq) maxFreq = freqBound;
                } finally {
                    stream.close();
                }
            }
            if (maxFreq == DEVICEINFO_UNKNOWN) {
                String readFile = BZFileUtils.readFile(new File("/proc/cpufreq/cpufreq_freq"));
                readFile = readFile.toLowerCase();
                if (readFile.contains("khz")) {
                    readFile = readFile.replace("khz", "").trim();

                    double freqBound = Double.parseDouble(readFile);
                    if (freqBound > maxFreq)
                        maxFreq = (int) freqBound;
                } else if (readFile.contains("mhz")) {
                    readFile = readFile.replace("mhz", "").trim();

                    double freqBound = Double.parseDouble(readFile);
                    freqBound *= 1000; //MHz -> kHz
                    if (freqBound > maxFreq)
                        maxFreq = (int) freqBound;
                } else if (readFile.contains("ghz")) {
                    readFile = readFile.replace("ghz", "").trim();

                    double freqBound = Double.parseDouble(readFile);
                    freqBound *= 1000 * 1000; //GHz -> kHz
                    if (freqBound > maxFreq)
                        maxFreq = (int) freqBound;
                } else {
                    readFile = readFile.split(" ")[0].trim();
                    maxFreq = Integer.decode(readFile);
                }
            }

        } catch (Throwable e) {
            maxFreq = DEVICEINFO_UNKNOWN; //Fall through and return unknown.
        }
        maxCpuFreq = maxFreq;
        return maxFreq;
    }

    /**
     * Calculates the total RAM of the device through Android API or /proc/meminfo.
     *
     * @param c - Context object for current running activity.
     * @return Total RAM that the device has, or DEVICEINFO_UNKNOWN = -1 in the event of an error.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static long getTotalMemory(Context c) {
        // memInfo.totalMem not supported in pre-Jelly Bean APIs.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            am.getMemoryInfo(memInfo);
            if (memInfo != null) {
                return memInfo.totalMem;
            } else {
                return DEVICEINFO_UNKNOWN;
            }
        } else {
            long totalMem = DEVICEINFO_UNKNOWN;
            try {
                FileInputStream stream = new FileInputStream("/proc/meminfo");
                try {
                    totalMem = parseFileForValue("MemTotal", stream);
                    totalMem *= 1024;
                } finally {
                    stream.close();
                }
            } catch (Throwable e) {
            }
            return totalMem;
        }
    }

    /**
     * Helper method for reading values from system files, using a minimised buffer.
     *
     * @param textToMatch - Text in the system files to read for.
     * @param stream      - FileInputStream of the system file being read from.
     * @return A numerical value following textToMatch in specified the system file.
     * -1 in the event of a failure.
     */
    private static int parseFileForValue(String textToMatch, FileInputStream stream) {
        byte[] buffer = new byte[1024];
        try {
            int length = stream.read(buffer);
            for (int i = 0; i < length; i++) {
                if (buffer[i] == '\n' || i == 0) {
                    if (buffer[i] == '\n') i++;
                    for (int j = i; j < length; j++) {
                        int textIndex = j - i;
                        //Text doesn't match query at some point.
                        if (buffer[j] != textToMatch.charAt(textIndex)) {
                            break;
                        }
                        //Text matches query here.
                        if (textIndex == textToMatch.length() - 1) {
                            return extractValue(buffer, j);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            //Ignore any Throwables and fall through to return unknown value.
        }
        return DEVICEINFO_UNKNOWN;
    }

    /**
     * Helper method used by {@link #parseFileForValue(String, FileInputStream) parseFileForValue}. Parses
     * the next available number after the match in the file being read and returns it as an integer.
     *
     * @param index - The index in the buffer array to begin looking.
     * @return The next number on that line in the buffer, returned as an int. Returns
     * DEVICEINFO_UNKNOWN = -1 in the event that no more numbers exist on the same line.
     */
    private static int extractValue(byte[] buffer, int index) {
        while (index < buffer.length && buffer[index] != '\n') {
            if (Character.isDigit(buffer[index])) {
                int start = index;
                index++;
                while (index < buffer.length && Character.isDigit(buffer[index])) {
                    index++;
                }
                String str = new String(buffer, 0, start, index - start);
                return Integer.parseInt(str);
            }
            index++;
        }
        return DEVICEINFO_UNKNOWN;
    }


    public static boolean isArmCpuArchitecture() {
        boolean isArm = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String[] supportedAbis = Build.SUPPORTED_ABIS;
            if (null != supportedAbis && supportedAbis.length > 0) {
                for (String supportedAbi : supportedAbis) {
                    if (null != supportedAbi) {
                        if (supportedAbi.toLowerCase().contains("arm") || supportedAbi.toLowerCase().contains("neon")) {
                            isArm = true;
                            break;
                        }
                    }
                }
            }
        } else {
            String cpu_abi = Build.CPU_ABI;
            if (null != cpu_abi) {
                if (cpu_abi.toLowerCase().contains("arm") || cpu_abi.toLowerCase().contains("neon")) {
                    isArm = true;
                }
            }
            cpu_abi = Build.CPU_ABI2;
            if (null != cpu_abi) {
                if (cpu_abi.toLowerCase().contains("arm") || cpu_abi.toLowerCase().contains("neon")) {
                    isArm = true;
                }
            }
        }
        if (!isArm) {
            try {
                String strContent = BZFileUtils.readFile("/proc/cpuinfo");
                String lowerCaseContent = strContent.toLowerCase();
                if (lowerCaseContent.contains("arm") || lowerCaseContent.contains("neon")) {
                    isArm = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isArm;
    }
}
