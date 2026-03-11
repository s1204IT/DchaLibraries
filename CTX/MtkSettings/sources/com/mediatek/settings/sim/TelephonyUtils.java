package com.mediatek.settings.sim;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;

public class TelephonyUtils {
    private static boolean DBG = SystemProperties.get("ro.build.type").equals("eng");

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    public static boolean isRadioOn(int i, Context context) {
        ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        boolean zIsRadioOnForSubscriber = false;
        if (iTelephonyAsInterface != null) {
            if (i != -1) {
                try {
                    zIsRadioOnForSubscriber = iTelephonyAsInterface.isRadioOnForSubscriber(i, context.getPackageName());
                } catch (RemoteException e) {
                    Log.e("TelephonyUtils", "isRadioOn, RemoteException=" + e);
                }
            }
        } else {
            Log.e("TelephonyUtils", "isRadioOn, ITelephony is null.");
        }
        log("isRadioOn=" + zIsRadioOnForSubscriber + ", subId=" + i);
        return zIsRadioOnForSubscriber;
    }

    public static boolean isCapabilitySwitching() {
        boolean zIsCapabilitySwitching;
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (iMtkTelephonyExAsInterface != null) {
            try {
                zIsCapabilitySwitching = iMtkTelephonyExAsInterface.isCapabilitySwitching();
            } catch (RemoteException e) {
                Log.e("TelephonyUtils", "isCapabilitySwitching, RemoteException=" + e);
                zIsCapabilitySwitching = false;
            }
            log("isSwitching=" + zIsCapabilitySwitching);
            return zIsCapabilitySwitching;
        }
        log("isCapabilitySwitching, IMtkTelephonyEx service not ready.");
        zIsCapabilitySwitching = false;
        log("isSwitching=" + zIsCapabilitySwitching);
        return zIsCapabilitySwitching;
    }

    private static void log(String str) {
        if (DBG) {
            Log.d("TelephonyUtils", str);
        }
    }

    public static int getMainCapabilityPhoneId() {
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (iMtkTelephonyExAsInterface != null) {
            try {
                return iMtkTelephonyExAsInterface.getMainCapabilityPhoneId();
            } catch (RemoteException e) {
                log("getMainCapabilityPhoneId, RemoteException=" + e);
                return -1;
            }
        }
        log("IMtkTelephonyEx service not ready.");
        return RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
    }
}
