package com.android.music;

import android.os.SystemProperties;
import android.util.Log;

public class MusicLogUtils {
    private static final boolean LOG_ENABLE;

    static {
        LOG_ENABLE = SystemProperties.get("ro.build.type").equals("eng") || Log.isLoggable("MusicLog", 3);
    }

    public static final int v(String str, String str2) {
        if (LOG_ENABLE) {
            return Log.v(str, str2);
        }
        return 0;
    }

    public static final int v(String str, String str2, Throwable th) {
        if (LOG_ENABLE) {
            return Log.v(str, str2, th);
        }
        return 0;
    }

    public static final int d(String str, String str2) {
        if (LOG_ENABLE) {
            return Log.d(str, str2);
        }
        return 0;
    }

    public static final int e(String str, String str2) {
        return Log.e(str, str2);
    }
}
