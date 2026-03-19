package com.mediatek.contacts.util;

import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;

public final class Log {
    public static final boolean ENG_DEBUG = TextUtils.equals(Build.TYPE, "eng");
    public static final boolean FORCE_DEBUG;

    static {
        FORCE_DEBUG = SystemProperties.getInt("persist.vendor.log.tel_dbg", 0) == 1;
    }

    public static boolean isLoggable(String str, int i) {
        return android.util.Log.isLoggable(str, i);
    }

    public static void v(String str, String str2) {
        if (ENG_DEBUG || FORCE_DEBUG) {
            android.util.Log.v("ContactsApp/" + str, str2);
        }
    }

    public static void v(String str, String str2, Throwable th) {
        android.util.Log.v("ContactsApp/" + str, str2, th);
    }

    public static void d(String str, String str2) {
        if (ENG_DEBUG || FORCE_DEBUG) {
            android.util.Log.d("ContactsApp/" + str, str2);
        }
    }

    public static void i(String str, String str2) {
        if (ENG_DEBUG || FORCE_DEBUG) {
            android.util.Log.i("ContactsApp/" + str, str2);
        }
    }

    public static void i(String str, String str2, Throwable th) {
        android.util.Log.i("ContactsApp/" + str, str2, th);
    }

    public static void w(String str, String str2) {
        android.util.Log.w("ContactsApp/" + str, str2);
    }

    public static void w(String str, String str2, Throwable th) {
        android.util.Log.w("ContactsApp/" + str, str2, th);
    }

    public static void e(String str, String str2) {
        android.util.Log.e("ContactsApp/" + str, str2);
    }

    public static void e(String str, String str2, Throwable th) {
        android.util.Log.e("ContactsApp/" + str, str2, th);
    }

    public static void wtf(String str, String str2) {
        android.util.Log.wtf("ContactsApp/" + str, str2);
    }

    public static void sensitive(String str, String str2) {
    }

    public static String anonymize(Object obj) {
        String strValueOf = String.valueOf(obj);
        if (strValueOf.isEmpty() || strValueOf.equals("null")) {
            return strValueOf;
        }
        if (strValueOf.length() == 1) {
            return "*";
        }
        int length = strValueOf.length() / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append('*');
        }
        sb.append(strValueOf.substring(length, strValueOf.length()));
        return sb.toString();
    }
}
