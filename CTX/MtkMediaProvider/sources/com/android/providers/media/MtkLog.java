package com.android.providers.media;

import android.util.Log;

public final class MtkLog {
    public static int v(String str, String str2) {
        return Log.v("@M_" + str, str2);
    }

    public static int d(String str, String str2) {
        return Log.d("@M_" + str, str2);
    }

    public static int w(String str, String str2) {
        return Log.w("@M_" + str, str2);
    }

    public static int e(String str, String str2) {
        return Log.e("@M_" + str, str2);
    }

    public static int e(String str, String str2, Throwable th) {
        return Log.e("@M_" + str, str2, th);
    }
}
