package com.android.gallery3d.data;

public class Log {
    public static void d(String str, String str2) {
        com.mediatek.gallery3d.util.Log.d(str, str2);
    }

    public static void w(String str, String str2) {
        com.mediatek.gallery3d.util.Log.w(str, str2);
    }

    public static void w(String str, String str2, Throwable th) {
        com.mediatek.gallery3d.util.Log.w(str, str2, th);
    }

    public static void w(String str, Throwable th) {
        com.mediatek.gallery3d.util.Log.w(str, th);
    }

    public static void e(String str, String str2) {
        com.mediatek.gallery3d.util.Log.e(str, str2);
    }

    public static void e(String str, String str2, Throwable th) {
        com.mediatek.gallery3d.util.Log.e(str, str2, th);
    }
}
