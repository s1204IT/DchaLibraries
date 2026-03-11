package com.mediatek.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class RestoreRotationReceiver extends BroadcastReceiver {
    public static boolean sRestoreRetore = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v("RestoreRotationReceiver_IPO", action);
        if (!action.equals("android.intent.action.BOOT_COMPLETED") && !action.equals("android.intent.action.ACTION_BOOT_IPO") && !action.equals("android.intent.action.USER_SWITCHED_FOR_MULTIUSER_APP")) {
            return;
        }
        sRestoreRetore = Settings.System.getIntForUser(context.getContentResolver(), "accelerometer_rotation_restore", 0, -2) != 0;
        if (!sRestoreRetore) {
            return;
        }
        Settings.System.putIntForUser(context.getContentResolver(), "accelerometer_rotation", 1, -2);
        Settings.System.putIntForUser(context.getContentResolver(), "accelerometer_rotation_restore", 0, -2);
    }
}
