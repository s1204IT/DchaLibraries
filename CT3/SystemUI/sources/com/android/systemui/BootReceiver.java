package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            ContentResolver res = context.getContentResolver();
            if (Settings.Global.getInt(res, "show_processes", 0) == 0) {
                return;
            }
            Intent loadavg = new Intent(context, (Class<?>) LoadAverageService.class);
            context.startService(loadavg);
        } catch (Exception e) {
            Log.e("SystemUIBootReceiver", "Can't start load average service", e);
        }
    }
}
