package com.android.documentsui;

import android.content.Context;
import android.preference.PreferenceManager;

public class LocalPreferences {
    public static boolean getDisplayAdvancedDevices(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("advancedDevices", false);
    }

    public static boolean getDisplayFileSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("fileSize", false);
    }

    public static void setDisplayAdvancedDevices(Context context, boolean display) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("advancedDevices", display).apply();
    }

    public static void setDisplayFileSize(Context context, boolean display) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("fileSize", display).apply();
    }
}
