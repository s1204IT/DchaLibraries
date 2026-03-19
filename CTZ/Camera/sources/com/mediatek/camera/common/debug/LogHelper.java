package com.mediatek.camera.common.debug;

import android.util.Log;
import com.mediatek.camera.common.debug.LogUtil;

public class LogHelper {
    public static void ui(LogUtil.Tag tag, String str) {
        i(tag, "[CamUI] " + str);
    }

    public static void d(LogUtil.Tag tag, String str) {
        if (LogUtil.isLoggable(tag, 3)) {
            Log.d(tag.toString(), str);
        }
    }

    public static void e(LogUtil.Tag tag, String str) {
        if (LogUtil.isLoggable(tag, 6)) {
            Log.e(tag.toString(), str);
        }
    }

    public static void e(LogUtil.Tag tag, String str, Throwable th) {
        if (LogUtil.isLoggable(tag, 6)) {
            Log.e(tag.toString(), str, th);
        }
    }

    public static void i(LogUtil.Tag tag, String str) {
        if (LogUtil.isLoggable(tag, 4)) {
            Log.i(tag.toString(), str);
        }
    }

    public static void v(LogUtil.Tag tag, String str) {
        if (LogUtil.isLoggable(tag, 2)) {
            Log.v(tag.toString(), str);
        }
    }

    public static void w(LogUtil.Tag tag, String str) {
        if (LogUtil.isLoggable(tag, 5)) {
            Log.w(tag.toString(), str);
        }
    }

    public static void w(LogUtil.Tag tag, String str, Throwable th) {
        if (LogUtil.isLoggable(tag, 5)) {
            Log.w(tag.toString(), str, th);
        }
    }
}
