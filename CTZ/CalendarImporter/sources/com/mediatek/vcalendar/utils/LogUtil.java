package com.mediatek.vcalendar.utils;

import android.util.Log;

public final class LogUtil {
    public static final String TAG = "vCalendar---";
    private static final boolean XLOG_ENABLED = true;

    private LogUtil() {
    }

    public static void v(String str, String str2) {
        Log.v("@M_vCalendar---" + str, str2);
    }

    public static void v(String str, String str2, Throwable th) {
        Log.v("@M_vCalendar---" + str, str2, th);
    }

    public static void d(String str, String str2) {
        Log.d("@M_vCalendar---" + str, str2);
    }

    public static void d(String str, String str2, Throwable th) {
        Log.d("@M_vCalendar---" + str, str2, th);
    }

    public static void i(String str, String str2) {
        Log.i("@M_vCalendar---" + str, str2);
    }

    public static void i(String str, String str2, Throwable th) {
        Log.i("@M_vCalendar---" + str, str2, th);
    }

    public static void w(String str, String str2) {
        Log.w("@M_vCalendar---" + str, str2);
    }

    public static void w(String str, String str2, Throwable th) {
        Log.w("@M_vCalendar---" + str, str2, th);
    }

    public static void e(String str, String str2) {
        Log.e("@M_vCalendar---" + str, str2);
    }

    public static void e(String str, String str2, Throwable th) {
        Log.e("@M_vCalendar---" + str, str2, th);
    }
}
