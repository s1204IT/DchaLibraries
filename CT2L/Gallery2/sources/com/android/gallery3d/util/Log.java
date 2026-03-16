package com.android.gallery3d.util;

public class Log {
    public static int w(String tag, String msg, Throwable tr) {
        return android.util.Log.w(tag, msg, tr);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return android.util.Log.e(tag, msg, tr);
    }
}
