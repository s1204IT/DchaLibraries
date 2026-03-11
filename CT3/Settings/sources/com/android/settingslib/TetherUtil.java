package com.android.settingslib;

import android.R;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;

public class TetherUtil {
    public static boolean setWifiTethering(boolean enable, Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        return wifiManager.setWifiApEnabled(null, enable);
    }

    private static boolean isEntitlementCheckRequired(Context context) {
        CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        return configManager.getConfig().getBoolean("require_entitlement_checks_bool");
    }

    public static boolean isProvisioningNeeded(Context context) {
        String[] provisionApp = context.getResources().getStringArray(R.array.config_autoBrightnessLevelsIdle);
        return !SystemProperties.getBoolean("net.tethering.noprovisioning", false) && provisionApp != null && isEntitlementCheckRequired(context) && provisionApp.length == 2;
    }
}
