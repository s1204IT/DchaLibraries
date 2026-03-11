package com.mediatek.settings.sim;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.ITelephonyEx;

public class TelephonyUtils {
    private static boolean DBG;

    static {
        DBG = SystemProperties.get("ro.build.type").equals("eng");
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    public static boolean isRadioOn(int subId, Context context) {
        ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        boolean isOn = false;
        try {
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (phone != null) {
            if (subId != -1) {
                isOn = phone.isRadioOnForSubscriber(subId, context.getPackageName());
            } else {
                isOn = false;
                log("isRadioOn = " + isOn + ", subId: " + subId);
                return isOn;
            }
        } else {
            Log.e("TelephonyUtils", "ITelephony is null !!!");
        }
        log("isRadioOn = " + isOn + ", subId: " + subId);
        return isOn;
    }

    public static boolean isCapabilitySwitching() {
        ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        boolean isSwitching = false;
        try {
            if (telephonyEx != null) {
                isSwitching = telephonyEx.isCapabilitySwitching();
            } else {
                Log.d("TelephonyUtils", "mTelephonyEx is null, returen false");
            }
        } catch (RemoteException e) {
            Log.e("TelephonyUtils", "RemoteException = " + e);
        }
        log("isSwitching = " + isSwitching);
        return isSwitching;
    }

    private static void log(String msg) {
        if (!DBG) {
            return;
        }
        Log.d("TelephonyUtils", msg);
    }
}
