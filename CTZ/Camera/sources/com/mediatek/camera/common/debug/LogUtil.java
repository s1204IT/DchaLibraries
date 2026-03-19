package com.mediatek.camera.common.debug;

import android.os.Build;
import android.util.Log;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.portability.SystemProperties;

public class LogUtil {
    private static boolean sUserDebugLogAll = false;
    private static int sPersistLogLevel = -1;

    static {
        initCameraLogLevel();
    }

    public static final class Tag {
        private static final int MAX_TAG_LEN = 23 - "CamAp_".length();
        private String mValue;

        public Tag(String str) {
            this.mValue = "CamAp_" + str;
        }

        public String toString() {
            return this.mValue;
        }

        public void truncateTag() {
            this.mValue = this.mValue.length() - MAX_TAG_LEN > 0 ? this.mValue.substring(0, 22) : this.mValue;
        }
    }

    public static void initCameraLogLevel() {
        sPersistLogLevel = getPersistLevelFromProperty();
    }

    public static int getAndroidSDKVersion() {
        try {
            return Integer.valueOf(Build.VERSION.SDK_INT).intValue();
        } catch (NumberFormatException e) {
            Log.e("CamAp_LogUtil", e.getMessage());
            return 0;
        }
    }

    public static boolean isLoggable(Tag tag, int i) {
        boolean z;
        int overrideLevelFromProperty = getOverrideLevelFromProperty();
        if (overrideLevelFromProperty > -1 || sPersistLogLevel > -1) {
            z = (getLogLevelFromSystemLevel(i) <= overrideLevelFromProperty) || getLogLevelFromSystemLevel(i) <= sPersistLogLevel;
        }
        shouldLog(tag, i);
        return z || shouldLog(tag, i) || isDebugOsBuild();
    }

    private static int getOverrideLevelFromProperty() {
        try {
            return SystemProperties.getInt("vendor.debug.mtkcam.loglevel", -1);
        } catch (IllegalArgumentException e) {
            Log.e("CamAp_LogUtil", e.getMessage());
            return -1;
        }
    }

    private static int getPersistLevelFromProperty() {
        int i = -1;
        try {
            i = SystemProperties.getInt("persist.vendor.mtkcamapp.loglevel", -1);
        } catch (IllegalArgumentException e) {
            Log.e("CamAp_LogUtil", e.getMessage());
        }
        Log.i("CamAp_LogUtil", "getPersistLevelFromProperty: " + i);
        return i;
    }

    private static int getLogLevelFromSystemLevel(int i) {
        switch (i) {
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return 4;
            case Camera2Proxy.TEMPLATE_RECORD:
                return 3;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                return 2;
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                return 1;
            case Camera2Proxy.TEMPLATE_MANUAL:
                return 0;
            default:
                return -1;
        }
    }

    private static boolean shouldLog(Tag tag, int i) {
        try {
            return Log.isLoggable(tag.toString(), i);
        } catch (IllegalArgumentException e) {
            tag.truncateTag();
            return false;
        }
    }

    private static boolean isDebugOsBuild() {
        boolean zEquals;
        if (sUserDebugLogAll) {
            zEquals = "userdebug".equals(Build.TYPE);
        } else {
            zEquals = false;
        }
        return zEquals || "eng".equals(Build.TYPE);
    }
}
