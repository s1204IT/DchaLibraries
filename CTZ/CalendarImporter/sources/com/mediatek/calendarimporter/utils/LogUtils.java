package com.mediatek.calendarimporter.utils;

import android.util.Log;

public final class LogUtils {
    private static final String TAG = "LogUtils";
    private static final boolean XLOG_ENABLED = true;

    private LogUtils() {
    }

    public static void v(String str, String str2) {
        Log.v("@M_" + str, str2);
    }

    public static void v(String str, String str2, Throwable th) {
        Log.v("@M_" + str, str2, th);
    }

    public static void d(String str, String str2) {
        Log.d("@M_" + str, str2);
    }

    public static void d(String str, String str2, Throwable th) {
        Log.d("@M_" + str, str2, th);
    }

    public static void i(String str, String str2) {
        Log.i("@M_" + str, str2);
    }

    public static void i(String str, String str2, Throwable th) {
        Log.i("@M_" + str, str2, th);
    }

    public static void w(String str, String str2) {
        Log.w("@M_" + str, str2);
    }

    public static void w(String str, String str2, Throwable th) {
        Log.w("@M_" + str, str2, th);
    }

    public static void e(String str, String str2) {
        Log.e("@M_" + str, str2);
    }

    public static void e(String str, String str2, Throwable th) {
        Log.e("@M_" + str, str2, th);
    }
}
