package com.mediatek.gallery3d.util;

import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class Log {
    private static final int CUST_LOG_LEVEL;
    private static final boolean FORCE_ENABLE;
    private static final String BUILD_TYPE = SystemPropertyUtils.get("ro.build.type");
    private static final boolean IS_ENG = "eng".equalsIgnoreCase(BUILD_TYPE);
    private static final int LOG_LEVEL_IN_PROPERTY = SystemPropertyUtils.getInt("debug.gallery.loglevel", 2);

    static {
        int i = 2;
        if (LOG_LEVEL_IN_PROPERTY >= 0 && LOG_LEVEL_IN_PROPERTY <= 4) {
            i = LOG_LEVEL_IN_PROPERTY;
        }
        CUST_LOG_LEVEL = i;
        FORCE_ENABLE = SchemaSymbols.ATTVAL_TRUE_1.equals(SystemPropertyUtils.get("vendor.gallery.log.enable"));
        android.util.Log.d("MtkGallery2/Log", "BUILD_TYPE: " + BUILD_TYPE + ", IS_ENG: " + IS_ENG + ", CUST_LOG_LEVEL: " + CUST_LOG_LEVEL + ", FORCE_ENABLE: " + FORCE_ENABLE);
    }

    public static void v(String str, String str2) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(0)) {
            android.util.Log.v(str, str2);
        }
    }

    public static void v(String str, String str2, Throwable th) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(0)) {
            android.util.Log.v(str, str2, th);
        }
    }

    public static void d(String str, String str2) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(1)) {
            android.util.Log.d(str, str2);
        }
    }

    public static void d(String str, String str2, Throwable th) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(1)) {
            android.util.Log.d(str, str2, th);
        }
    }

    public static void i(String str, String str2) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(2)) {
            android.util.Log.i(str, str2);
        }
    }

    public static void i(String str, String str2, Throwable th) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(2)) {
            android.util.Log.i(str, str2, th);
        }
    }

    public static void w(String str, String str2) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(3)) {
            android.util.Log.w(str, str2);
        }
    }

    public static void w(String str, Throwable th) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(3)) {
            android.util.Log.w(str, th);
        }
    }

    public static void w(String str, String str2, Throwable th) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(3)) {
            android.util.Log.w(str, str2, th);
        }
    }

    public static void e(String str, String str2) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(4)) {
            android.util.Log.e(str, str2);
        }
    }

    public static void e(String str, String str2, Throwable th) {
        if (str == null) {
            return;
        }
        if (IS_ENG || FORCE_ENABLE || enableCustLog(4)) {
            android.util.Log.e(str, str2, th);
        }
    }

    private static boolean enableCustLog(int i) {
        return CUST_LOG_LEVEL >= 0 && CUST_LOG_LEVEL <= 4 && i >= CUST_LOG_LEVEL;
    }
}
