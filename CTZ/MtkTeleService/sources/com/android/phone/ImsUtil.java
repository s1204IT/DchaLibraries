package com.android.phone;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import com.android.ims.ImsManager;

public class ImsUtil {
    private static final boolean DBG = false;
    private static final String LOG_TAG = ImsUtil.class.getSimpleName();
    private static boolean sImsPhoneSupported;

    static {
        sImsPhoneSupported = false;
        PhoneGlobals.getInstance();
        sImsPhoneSupported = true;
    }

    private ImsUtil() {
    }

    static boolean isImsPhoneSupported() {
        return sImsPhoneSupported;
    }

    public static boolean isWfcEnabled(Context context) {
        ImsManager defaultImsManagerInstance = getDefaultImsManagerInstance(context);
        return defaultImsManagerInstance.isWfcEnabledByPlatform() && defaultImsManagerInstance.isWfcEnabledByUser();
    }

    public static boolean isWfcModeWifiOnly(Context context) {
        return isWfcEnabled(context) && (getDefaultImsManagerInstance(context).getWfcMode() == 0);
    }

    public static boolean shouldPromoteWfc(Context context) {
        ConnectivityManager connectivityManager;
        NetworkInfo activeNetworkInfo;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        return carrierConfigManager != null && carrierConfigManager.getConfig().getBoolean("carrier_promote_wfc_on_call_fail_bool") && getDefaultImsManagerInstance(context).isWfcProvisionedOnDevice() && (connectivityManager = (ConnectivityManager) context.getSystemService("connectivity")) != null && (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == 1 && !isWfcEnabled(context);
    }

    private static ImsManager getDefaultImsManagerInstance(Context context) {
        return ImsManager.getInstance(context, SubscriptionManager.getDefaultVoicePhoneId());
    }
}
