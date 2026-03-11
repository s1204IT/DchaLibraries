package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

public class HotspotOffReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = Log.isLoggable("HotspotOffReceiver", 3);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
            return;
        }
        WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        if (wifiManager.getWifiApState() != 11) {
            return;
        }
        if (DEBUG) {
            Log.d("HotspotOffReceiver", "TetherService.cancelRecheckAlarmIfNecessary called");
        }
        TetherService.cancelRecheckAlarmIfNecessary(context, 0);
    }
}
