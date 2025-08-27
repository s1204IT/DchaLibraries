package com.android.settingslib;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;

/* loaded from: classes.dex */
public class TetherUtil {
    static boolean isEntitlementCheckRequired(Context context) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        if (carrierConfigManager == null || carrierConfigManager.getConfig() == null) {
            return true;
        }
        return carrierConfigManager.getConfig().getBoolean("require_entitlement_checks_bool");
    }

    public static boolean isProvisioningNeeded(Context context) throws Resources.NotFoundException {
        String[] stringArray = context.getResources().getStringArray(android.R.array.config_companionPermSyncEnabledCerts);
        return !SystemProperties.getBoolean("net.tethering.noprovisioning", false) && stringArray != null && isEntitlementCheckRequired(context) && stringArray.length == 2;
    }
}
