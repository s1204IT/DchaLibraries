package com.mediatek.calendar;

import android.os.Build;
import android.util.Log;

public final class LogUtil {
    private static final boolean IS_ENG_BUILD = "eng".equals(Build.TYPE);

    public static void v(String str, String str2) {
        Log.v("@M_Calendar", "<<" + str + ">>: " + str2);
    }

    public static void d(String str, String str2) {
        Log.d("@M_Calendar", "<<" + str + ">>: " + str2);
    }

    public static void i(String str, String str2) {
        Log.i("@M_Calendar", "<<" + str + ">>: " + str2);
    }

    public static void w(String str, String str2) {
        Log.w("@M_Calendar", "<<" + str + ">>: " + str2);
    }

    public static void e(String str, String str2) {
        Log.e("@M_Calendar", "<<" + str + ">>: " + str2);
    }

    public static void oi(String str, String str2) {
        if (IS_ENG_BUILD) {
            Log.i("@M_Calendar", "<<" + str + ">>: " + str2);
        }
    }
}
