package com.android.mms.service;

import android.util.Log;

public class LogUtil {
    public static void i(String str, String str2) {
        Log.i("MmsService", "[" + str + "] " + str2);
    }

    public static void i(String str) {
        Log.i("MmsService", str);
    }

    public static void d(String str, String str2) {
        Log.d("MmsService", "[" + str + "] " + str2);
    }

    public static void d(String str) {
        Log.d("MmsService", str);
    }

    public static void v(String str, String str2) {
        Log.v("MmsService", "[" + str + "] " + str2);
    }

    public static void v(String str) {
        Log.v("MmsService", str);
    }

    public static void e(String str, String str2, Throwable th) {
        Log.e("MmsService", "[" + str + "] " + str2, th);
    }

    public static void e(String str, Throwable th) {
        Log.e("MmsService", str, th);
    }

    public static void e(String str, String str2) {
        Log.e("MmsService", "[" + str + "] " + str2);
    }

    public static void e(String str) {
        Log.e("MmsService", str);
    }

    public static void w(String str, String str2, Throwable th) {
        Log.w("MmsService", "[" + str + "] " + str2, th);
    }

    public static void w(String str, Throwable th) {
        Log.w("MmsService", str, th);
    }

    public static void w(String str, String str2) {
        Log.w("MmsService", "[" + str + "] " + str2);
    }

    public static void w(String str) {
        Log.w("MmsService", str);
    }

    public static boolean isLoggable(int i) {
        return Log.isLoggable("MmsService", i);
    }
}
