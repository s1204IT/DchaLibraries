package com.android.managedprovisioning.common;

import android.util.Log;

public class ProvisionLogger {
    public static void logd(String str) {
        Log.d(getTag(), str);
    }

    public static void logi(String str) {
        Log.i(getTag(), str);
    }

    public static void logi(String str, Throwable th) {
        Log.i(getTag(), str, th);
    }

    public static void logw(String str) {
        Log.w(getTag(), str);
    }

    public static void logw(String str, Throwable th) {
        Log.w(getTag(), str, th);
    }

    public static void loge(String str) {
        Log.e(getTag(), str);
    }

    public static void loge(String str, Throwable th) {
        Log.e(getTag(), str, th);
    }

    static String getTag() {
        return "ManagedProvisioning";
    }
}
