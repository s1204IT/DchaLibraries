package com.android.settingslib.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.PasspointConfiguration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WifiSavedConfigUtils {
    public static List<AccessPoint> getAllConfigs(Context context, WifiManager wifiManager) {
        ArrayList arrayList = new ArrayList();
        for (WifiConfiguration wifiConfiguration : wifiManager.getConfiguredNetworks()) {
            if (!wifiConfiguration.isPasspoint() && !wifiConfiguration.isEphemeral()) {
                arrayList.add(new AccessPoint(context, wifiConfiguration));
            }
        }
        try {
            Iterator<PasspointConfiguration> it = wifiManager.getPasspointConfigurations().iterator();
            while (it.hasNext()) {
                arrayList.add(new AccessPoint(context, it.next()));
            }
        } catch (UnsupportedOperationException e) {
        }
        return arrayList;
    }
}
