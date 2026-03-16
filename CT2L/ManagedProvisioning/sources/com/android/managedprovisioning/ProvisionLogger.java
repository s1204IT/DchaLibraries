package com.android.managedprovisioning;

import android.util.Log;

public class ProvisionLogger {
    public static void logd(String message) {
        Log.d(getTag(), message);
    }

    public static void logi(String message) {
        Log.i(getTag(), message);
    }

    public static void logw(String message) {
        Log.w(getTag(), message);
    }

    public static void logw(String message, Throwable t) {
        Log.w(getTag(), message, t);
    }

    public static void loge(String message) {
        Log.e(getTag(), message);
    }

    public static void loge(String message, Throwable t) {
        Log.e(getTag(), message, t);
    }

    public static void loge(Throwable t) {
        Log.e(getTag(), "", t);
    }

    static String getTag() {
        return "ManagedProvisioning";
    }
}
