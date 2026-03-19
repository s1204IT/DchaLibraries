package org.gsma.joyn;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public final class Logger {
    public static final boolean IS_DEBUG = true;
    public static final String RCS_PREFS_IS_INTEGRATION_MODE = "IS_INTEGRATION_MODE";
    public static final String TAG = "[RCSe]";
    private static final boolean XLOG_ENABLED = true;
    private static boolean sIsIntegrationMode;
    private static boolean sIsLogEnabled = true;

    private Logger() {
    }

    public static boolean getIsIntegrationMode() {
        return true;
    }

    public static void setIsIntegrationMode(boolean isIntegrationMode) {
        sIsIntegrationMode = isIntegrationMode;
    }

    public static void initialize(Context context) {
        if (context != null) {
            v(TAG, "initialize(), context is not null");
            SharedPreferences preferences = context.getSharedPreferences(RCS_PREFS_IS_INTEGRATION_MODE, 0);
            if (preferences.contains(RCS_PREFS_IS_INTEGRATION_MODE)) {
                sIsIntegrationMode = preferences.getBoolean(RCS_PREFS_IS_INTEGRATION_MODE, false);
            } else {
                sIsIntegrationMode = false;
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(RCS_PREFS_IS_INTEGRATION_MODE, false);
                editor.commit();
            }
        } else {
            sIsIntegrationMode = false;
        }
        v(TAG, "initialize(), sIsIntegrationMode = " + sIsIntegrationMode);
    }

    public static void setLogEnabled(boolean isLogEnable) {
        sIsLogEnabled = isLogEnable;
        sIsLogEnabled = true;
    }

    public static void v(String tag, String message) {
        Log.v(TAG, getCombinedMessage(tag, message));
    }

    public static void d(String tag, String message) {
        Log.d(TAG, getCombinedMessage(tag, message));
    }

    public static void i(String tag, String message) {
        Log.i(TAG, getCombinedMessage(tag, message));
    }

    public static void w(String tag, String message) {
        Log.w(TAG, getCombinedMessage(tag, message));
    }

    public static void e(String tag, String message) {
        Log.e(TAG, getCombinedMessage(tag, message));
    }

    private static String getCombinedMessage(String tag, String message) {
        if (tag != null) {
            return "[" + tag + "]: " + message;
        }
        return message;
    }
}
