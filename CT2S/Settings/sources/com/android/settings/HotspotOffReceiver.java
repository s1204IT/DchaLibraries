package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

public class HotspotOffReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
            WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
            if (wifiManager.getWifiApState() == 11) {
                TetherService.cancelRecheckAlarmIfNecessary(context, 0);
            }
        }
    }
}
