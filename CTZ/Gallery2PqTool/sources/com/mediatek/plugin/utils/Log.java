package com.mediatek.plugin.utils;

public class Log {
    private static final int CUST_LOG_LEVEL;
    private static final String BUILD_TYPE = SystemPropertyUtils.get("ro.build.type");
    private static final boolean IS_ENG = "eng".equalsIgnoreCase(BUILD_TYPE);
    private static final int LOG_LEVEL_IN_PROPERTY = SystemPropertyUtils.getInt("debug.gallery.loglevel", 2);

    static {
        int i = 2;
        if (LOG_LEVEL_IN_PROPERTY >= 0 && LOG_LEVEL_IN_PROPERTY <= 4) {
            i = LOG_LEVEL_IN_PROPERTY;
        }
        CUST_LOG_LEVEL = i;
        android.util.Log.d("PluginManager/Log", "BUILD_TYPE: " + BUILD_TYPE + ", IS_ENG: " + IS_ENG + ", CUST_LOG_LEVEL: " + CUST_LOG_LEVEL);
    }

    public static void d(String str, String str2) {
        if (str == null) {
            return;
        }
        if (IS_ENG || enableCustLog(1)) {
            android.util.Log.d(str, str2);
        }
    }

    private static boolean enableCustLog(int i) {
        return CUST_LOG_LEVEL >= 0 && CUST_LOG_LEVEL <= 4 && i >= CUST_LOG_LEVEL;
    }
}
