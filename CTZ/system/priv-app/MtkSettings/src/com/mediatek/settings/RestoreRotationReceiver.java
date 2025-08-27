package com.mediatek.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/* loaded from: classes.dex */
public class RestoreRotationReceiver extends BroadcastReceiver {
    public static boolean sRestoreRetore = false;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v("RestoreRotationReceiver_IPO", action);
        if (action.equals("android.intent.action.LOCKED_BOOT_COMPLETED")) {
            sRestoreRetore = Settings.System.getIntForUser(context.getContentResolver(), "accelerometer_rotation_restore", 0, -2) != 0;
            if (sRestoreRetore) {
                Settings.System.putIntForUser(context.getContentResolver(), "accelerometer_rotation", 1, -2);
                Settings.System.putIntForUser(context.getContentResolver(), "accelerometer_rotation_restore", 0, -2);
            }
        }
    }
}
