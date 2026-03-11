package com.mediatek.systemui.statusbar.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import java.util.List;

public class SIMHelper {
    private static boolean bMtkHotKnotSupport = SystemProperties.get("ro.mtk_hotknot_support").equals("1");
    public static Context sContext;
    private static List<SubscriptionInfo> sSimInfos;

    private SIMHelper() {
    }

    public static void updateSIMInfos(Context context) {
        sSimInfos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
    }

    public static int getFirstSubInSlot(int slotId) {
        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds != null && subIds.length > 0) {
            return subIds[0];
        }
        Log.d("SIMHelper", "Cannot get first sub in slot: " + slotId);
        return -1;
    }

    public static int getSlotCount() {
        return TelephonyManager.getDefault().getPhoneCount();
    }

    public static final boolean isMtkHotKnotSupport() {
        Log.d("@M_SIMHelper", "isMtkHotKnotSupport, bMtkHotKnotSupport = " + bMtkHotKnotSupport);
        return bMtkHotKnotSupport;
    }

    public static void setContext(Context context) {
        sContext = context;
    }

    public static boolean isWifiOnlyDevice() {
        ConnectivityManager cm = (ConnectivityManager) sContext.getSystemService("connectivity");
        return !cm.isNetworkSupported(0);
    }

    public static boolean isRadioOn(int subId) {
        ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        if (telephony != null) {
            try {
                return telephony.isRadioOnForSubscriber(subId, sContext.getPackageName());
            } catch (RemoteException e) {
                Log.e("SIMHelper", "mTelephony exception");
                return false;
            }
        }
        return false;
    }
}
