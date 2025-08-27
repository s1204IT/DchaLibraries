package com.android.settings.fuelgauge.batterytip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* loaded from: classes.dex */
public class AnomalyDetectionReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Log.i("SettingsAnomalyReceiver", "Anomaly intent received.  configUid = " + intent.getLongExtra("android.app.extra.STATS_CONFIG_UID", -1L) + " configKey = " + intent.getLongExtra("android.app.extra.STATS_CONFIG_KEY", -1L) + " subscriptionId = " + intent.getLongExtra("android.app.extra.STATS_SUBSCRIPTION_ID", -1L));
        intent.getExtras().putLong("key_anomaly_timestamp", System.currentTimeMillis());
        AnomalyDetectionJobService.scheduleAnomalyDetection(context, intent);
    }
}
