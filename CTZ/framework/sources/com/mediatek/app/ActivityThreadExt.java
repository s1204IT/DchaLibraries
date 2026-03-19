package com.mediatek.app;

import android.app.ActivityThread;
import android.hardware.Camera;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.SystemProperties;

public class ActivityThreadExt {
    public static void enableActivityThreadLog(ActivityThread activityThread) {
        String str = SystemProperties.get("persist.vendor.sys.activitylog", null);
        if (str != null && !str.equals("")) {
            if (str.indexOf(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER) != -1 && str.indexOf(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER) + 1 <= str.length()) {
                String strSubstring = str.substring(0, str.indexOf(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER));
                boolean zEquals = Camera.Parameters.FLASH_MODE_ON.equals(str.substring(str.indexOf(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER) + 1, str.length()));
                if (strSubstring.equals("x")) {
                    enableActivityThreadLog(zEquals, activityThread);
                    return;
                }
                return;
            }
            SystemProperties.set("persist.vendor.sys.activitylog", "");
        }
    }

    public static void enableActivityThreadLog(boolean z, ActivityThread activityThread) {
        ActivityThread.localLOGV = z;
        ActivityThread.DEBUG_MESSAGES = z;
        ActivityThread.DEBUG_BROADCAST = z;
        ActivityThread.DEBUG_RESULTS = z;
        ActivityThread.DEBUG_BACKUP = z;
        ActivityThread.DEBUG_CONFIGURATION = z;
        ActivityThread.DEBUG_SERVICE = z;
        ActivityThread.DEBUG_MEMORY_TRIM = z;
        ActivityThread.DEBUG_PROVIDER = z;
        ActivityThread.DEBUG_ORDER = z;
    }
}
