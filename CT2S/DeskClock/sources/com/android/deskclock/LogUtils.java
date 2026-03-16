package com.android.deskclock;

import android.os.Build;

public class LogUtils {
    public static final boolean DEBUG;

    static {
        DEBUG = "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }

    public static void v(String message, Object... args) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 2)) {
            if (args != null) {
                message = String.format(message, args);
            }
            android.util.Log.v("AlarmClock", message);
        }
    }

    public static void v(String tag, String message, Object... args) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 2)) {
            String str = "AlarmClock/" + tag;
            if (args != null) {
                message = String.format(message, args);
            }
            android.util.Log.v(str, message);
        }
    }

    public static void i(String message, Object... args) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 4)) {
            if (args != null) {
                message = String.format(message, args);
            }
            android.util.Log.i("AlarmClock", message);
        }
    }

    public static void i(String tag, String message, Object... args) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 4)) {
            String str = "AlarmClock/" + tag;
            if (args != null) {
                message = String.format(message, args);
            }
            android.util.Log.i(str, message);
        }
    }

    public static void w(String message, Object... args) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 5)) {
            if (args != null) {
                message = String.format(message, args);
            }
            android.util.Log.w("AlarmClock", message);
        }
    }

    public static void e(String message, Object... args) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 6)) {
            if (args != null) {
                message = String.format(message, args);
            }
            android.util.Log.e("AlarmClock", message);
        }
    }

    public static void e(String tag, String message, Object... args) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 6)) {
            String str = "AlarmClock/" + tag;
            if (args != null) {
                message = String.format(message, args);
            }
            android.util.Log.e(str, message);
        }
    }

    public static void e(String message, Exception e) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 6)) {
            android.util.Log.e("AlarmClock", message, e);
        }
    }

    public static void wtf(String message, Object... args) {
        if (DEBUG || android.util.Log.isLoggable("AlarmClock", 7)) {
            if (args != null) {
                message = String.format(message, args);
            }
            android.util.Log.wtf("AlarmClock", message);
        }
    }
}
