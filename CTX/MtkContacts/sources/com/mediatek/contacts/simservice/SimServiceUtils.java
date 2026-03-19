package com.mediatek.contacts.simservice;

import android.content.Context;
import android.provider.Settings;

public class SimServiceUtils {
    public static boolean isServiceRunning(Context context, int i) {
        return "true".equals(Settings.System.getString(context.getContentResolver(), "import_remove_running"));
    }
}
