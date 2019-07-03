package com.ufotosoft.bzmedia.utils;

public class BZLogUtil {
    private static boolean gIsLog = false;
    private static final String TAG = "bz_";

    public static void setLog(boolean isLog) {
        BZLogUtil.gIsLog = isLog;
    }

    public static boolean getIsLog() {
        return gIsLog;
    }

    public static void v(String tag, String msg) {
        if (gIsLog) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void v(String msg) {
        if (gIsLog) {
            android.util.Log.v(TAG, msg);
        }

    }

    public static void d(String tag, String msg) {
        if (gIsLog) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void d(String msg) {
        if (gIsLog) {
            android.util.Log.d(TAG, msg);
        }

    }

    public static void w(String tag, String msg) {
        if (gIsLog) {
            android.util.Log.w(tag, msg);
        }
    }

    public static void w(String msg) {
        if (gIsLog) {
            android.util.Log.w(TAG, msg);
        }

    }

    /**
     * Send a {@link #gIsLog} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void d(String tag, String msg, Throwable tr) {
        if (gIsLog) {
            android.util.Log.d(tag, msg, tr);
        }
    }

    public static void i(String tag, String msg) {
        if (gIsLog) {
            android.util.Log.i(tag, msg);
        }
    }

    /**
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void i(String tag, String msg, Throwable tr) {
        if (gIsLog) {
            android.util.Log.i(tag, msg, tr);
        }

    }

    /**
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
        if (gIsLog) {
            android.util.Log.e(tag, msg);
        }
    }

    public static void e(String msg) {
        if (gIsLog) {
            android.util.Log.e(TAG, msg);
        }
    }

    /**
     * @param tag Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static void e(String tag, String msg, Throwable tr) {
        if (gIsLog) {
            android.util.Log.e(tag, msg, tr);
        }
    }

    public static void e(String msg, Throwable tr) {
        if (gIsLog) {
            android.util.Log.e(TAG, msg, tr);
        }
    }
}
