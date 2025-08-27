package com.android.settings.fuelgauge.batterytip;

import android.app.StatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* loaded from: classes.dex */
public class AnomalyConfigReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if ("android.app.action.STATSD_STARTED".equals(intent.getAction()) || "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            StatsManager statsManager = (StatsManager) context.getSystemService(StatsManager.class);
            AnomalyConfigJobService.scheduleConfigUpdate(context);
            try {
                BatteryTipUtils.uploadAnomalyPendingIntent(context, statsManager);
            } catch (StatsManager.StatsUnavailableException e) {
                Log.w("AnomalyConfigReceiver", "Failed to uploadAnomalyPendingIntent.", e);
            }
            if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                AnomalyCleanupJobService.scheduleCleanUp(context);
            }
        }
    }
}
