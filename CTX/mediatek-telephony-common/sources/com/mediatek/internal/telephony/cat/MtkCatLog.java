package com.mediatek.internal.telephony.cat;

import android.os.Build;
import android.telephony.Rlog;
import android.text.TextUtils;

public abstract class MtkCatLog {
    static final boolean DEBUG = true;
    static final boolean ENGDEBUG = TextUtils.equals(Build.TYPE, "eng");
    static final String TAG = "MTKCAT";

    public static void d(Object obj, String str) {
        String name = obj.getClass().getName();
        Rlog.d(TAG, name.substring(name.lastIndexOf(46) + 1) + ": " + str);
    }

    public static void d(String str, String str2) {
        Rlog.d(TAG, str + ": " + str2);
    }

    public static void e(Object obj, String str) {
        String name = obj.getClass().getName();
        Rlog.e(TAG, name.substring(name.lastIndexOf(46) + 1) + ": " + str);
    }

    public static void e(String str, String str2) {
        Rlog.e(TAG, str + ": " + str2);
    }

    public static void w(Object obj, String str) {
        if (!ENGDEBUG) {
            return;
        }
        String name = obj.getClass().getName();
        Rlog.w(TAG, name.substring(name.lastIndexOf(46) + 1) + ": " + str);
    }

    public static void w(String str, String str2) {
        if (!ENGDEBUG) {
            return;
        }
        Rlog.w(TAG, str + ": " + str2);
    }

    public static void v(Object obj, String str) {
        if (!ENGDEBUG) {
            return;
        }
        String name = obj.getClass().getName();
        Rlog.v(TAG, name.substring(name.lastIndexOf(46) + 1) + ": " + str);
    }

    public static void v(String str, String str2) {
        if (!ENGDEBUG) {
            return;
        }
        Rlog.v(TAG, str + ": " + str2);
    }
}
