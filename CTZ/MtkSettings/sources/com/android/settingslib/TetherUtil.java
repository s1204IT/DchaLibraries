package com.android.settingslib;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;

public class TetherUtil {
    static boolean isEntitlementCheckRequired(Context context) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        if (carrierConfigManager == null || carrierConfigManager.getConfig() == null) {
            return true;
        }
        return carrierConfigManager.getConfig().getBoolean("require_entitlement_checks_bool");
    }

    public static boolean isProvisioningNeeded(Context context) {
        String[] stringArray = context.getResources().getStringArray(android.R.array.config_cell_retries_per_error_code);
        return !SystemProperties.getBoolean("net.tethering.noprovisioning", false) && stringArray != null && isEntitlementCheckRequired(context) && stringArray.length == 2;
    }
}
