package com.mediatek.galleryportable;

import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class Log {
    private static final boolean FORCE_ENABLE;
    private static final boolean IS_ENG;

    static {
        String buildType = SystemPropertyUtils.get("ro.build.type");
        IS_ENG = buildType != null ? buildType.equals("eng") : false;
        FORCE_ENABLE = SchemaSymbols.ATTVAL_TRUE_1.equals(SystemPropertyUtils.get("vendor.gallery.log.enable"));
    }

    private static boolean isLogEnable(String tag) {
        return FORCE_ENABLE || IS_ENG || android.util.Log.isLoggable(tag, 3);
    }

    public static int v(String tag, String msg) {
        if (isLogEnable(tag)) {
            return android.util.Log.v(tag, msg);
        }
        return -1;
    }

    public static int d(String tag, String msg) {
        if (isLogEnable(tag)) {
            return android.util.Log.d(tag, msg);
        }
        return -1;
    }

    public static int w(String tag, String msg) {
        if (isLogEnable(tag)) {
            return android.util.Log.w(tag, msg);
        }
        return -1;
    }

    public static int e(String tag, String msg) {
        if (isLogEnable(tag)) {
            return android.util.Log.e(tag, msg);
        }
        return -1;
    }
}
