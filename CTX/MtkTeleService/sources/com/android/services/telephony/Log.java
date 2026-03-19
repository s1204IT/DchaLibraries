package com.android.services.telephony;

import android.content.Context;
import com.mediatek.services.telephony.MtkLogUtils;

public final class Log {
    public static final boolean FORCE_LOGGING = false;
    private static final String TAG = "Telephony";
    public static final boolean DEBUG = isLoggable(3);
    public static final boolean INFO = isLoggable(4);
    public static final boolean VERBOSE = isLoggable(2);
    public static final boolean WARN = isLoggable(5);
    public static final boolean ERROR = isLoggable(6);

    private Log() {
    }

    public static boolean isLoggable(int i) {
        return android.util.Log.isLoggable(TAG, i);
    }

    public static void initLogging(Context context) {
        MtkLogUtils.initLogging(context);
    }

    public static void d(String str, String str2, Object... objArr) {
        android.telecom.Log.d(str, str2, objArr);
    }

    public static void d(Object obj, String str, Object... objArr) {
        android.telecom.Log.d(obj, str, objArr);
    }

    public static void i(String str, String str2, Object... objArr) {
        android.telecom.Log.i(str, str2, objArr);
    }

    public static void i(Object obj, String str, Object... objArr) {
        android.telecom.Log.i(obj, str, objArr);
    }

    public static void v(String str, String str2, Object... objArr) {
        android.telecom.Log.v(str, str2, objArr);
    }

    public static void v(Object obj, String str, Object... objArr) {
        android.telecom.Log.v(obj, str, objArr);
    }

    public static void w(String str, String str2, Object... objArr) {
        android.telecom.Log.w(str, str2, objArr);
    }

    public static void w(Object obj, String str, Object... objArr) {
        android.telecom.Log.w(obj, str, objArr);
    }

    public static void e(String str, Throwable th, String str2, Object... objArr) {
        android.telecom.Log.e(str, th, str2, objArr);
    }

    public static void e(Object obj, Throwable th, String str, Object... objArr) {
        android.telecom.Log.e(obj, th, str, objArr);
    }

    public static void wtf(String str, Throwable th, String str2, Object... objArr) {
        android.telecom.Log.wtf(str, th, str2, objArr);
    }

    public static void wtf(Object obj, Throwable th, String str, Object... objArr) {
        android.telecom.Log.wtf(obj, th, str, objArr);
    }

    public static void wtf(String str, String str2, Object... objArr) {
        android.telecom.Log.wtf(str, str2, objArr);
    }

    public static void wtf(Object obj, String str, Object... objArr) {
        android.telecom.Log.wtf(obj, str, objArr);
    }

    public static String pii(Object obj) {
        return android.telecom.Log.pii(obj);
    }
}
