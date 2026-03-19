package com.mediatek.services.telephony;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.telecom.Log;

public class MtkLogUtils {
    public static void initLogging(Context context) {
        setMtkTag("Telephony");
        Log.setSessionContext(context);
    }

    private static void setMtkTag(String str) {
        Log.TAG = str;
        if (Build.IS_ENG || SystemProperties.getInt("persist.vendor.log.tel_dbg", 0) > 0) {
            Log.ERROR = true;
            Log.WARN = true;
            Log.INFO = true;
            Log.DEBUG = true;
            Log.VERBOSE = true;
            return;
        }
        Log.ERROR = android.util.Log.isLoggable(str, 6);
        Log.WARN = android.util.Log.isLoggable(str, 5);
        Log.INFO = android.util.Log.isLoggable(str, 4);
        Log.DEBUG = android.util.Log.isLoggable(str, 3);
        Log.VERBOSE = android.util.Log.isLoggable(str, 2);
    }
}
