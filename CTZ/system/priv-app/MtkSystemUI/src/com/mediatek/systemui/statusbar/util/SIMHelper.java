package com.mediatek.systemui.statusbar.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.MtkIccCardConstants;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.List;

/* loaded from: classes.dex */
public class SIMHelper {
    private static final String[] CT_NUMERIC = {"45502", "45507", "46003", "46011", "46012", "46013"};
    public static Context sContext;
    private static List<SubscriptionInfo> sSimInfos;

    public static void updateSIMInfos(Context context) {
        sSimInfos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
    }

    public static int getFirstSubInSlot(int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId != null && subId.length > 0) {
            return subId[0];
        }
        Log.d("SIMHelper", "Cannot get first sub in slot: " + i);
        return -1;
    }

    public static int getSlotCount() {
        return TelephonyManager.getDefault().getPhoneCount();
    }

    public static void setContext(Context context) {
        sContext = context;
    }

    public static boolean isWifiOnlyDevice() {
        Context context = sContext;
        Context context2 = sContext;
        return !((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
    }

    public static boolean isRadioOn(int i) {
        ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        if (iTelephonyAsInterface != null) {
            try {
                return iTelephonyAsInterface.isRadioOnForSubscriber(i, sContext.getPackageName());
            } catch (RemoteException e) {
                Log.e("SIMHelper", "mTelephony exception");
                return false;
            }
        }
        return false;
    }

    public static int getMainPhoneId() {
        int i;
        String str = SystemProperties.get("persist.vendor.radio.simswitch", "1");
        if (!TextUtils.isEmpty(str)) {
            i = Integer.parseInt(str) - 1;
        } else {
            i = -1;
        }
        Log.d("@M_SIMHelper", "getMainPhoneId, mainPhoneId = " + i);
        return i;
    }

    public static boolean isSecondaryCSIMForMixedVolte(int i) {
        boolean z;
        int mainPhoneId = getMainPhoneId();
        int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(mainPhoneId);
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        if (mainPhoneId != slotIndex && ((isCTCardType(mainPhoneId) || isCtSim(subIdUsingPhoneId)) && (isCTCardType(slotIndex) || isCtSim(i)))) {
            z = true;
        } else {
            z = false;
        }
        if (FeatureOptions.LOG_ENABLE) {
            Log.d("@M_SIMHelper", "isSecondaryCSIMForMixedVolte, ret = " + z + ", subId = " + i);
        }
        return z;
    }

    public static boolean isCtSim(int i) {
        String simOperator = TelephonyManager.getDefault().getSimOperator(i);
        String[] strArr = CT_NUMERIC;
        int length = strArr.length;
        boolean z = false;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            if (!strArr[i2].equals(simOperator)) {
                i2++;
            } else {
                z = true;
                break;
            }
        }
        if (FeatureOptions.LOG_ENABLE) {
            Log.d("@M_SIMHelper", "isCtSim, ctSim = " + z + ", subId = " + i);
        }
        return z;
    }

    public static boolean isCTCardType(int i) {
        boolean z;
        MtkIccCardConstants.CardType cdmaCardType = MtkTelephonyManagerEx.getDefault().getCdmaCardType(i);
        if (MtkIccCardConstants.CardType.CT_4G_UICC_CARD.equals(cdmaCardType) || MtkIccCardConstants.CardType.CT_3G_UIM_CARD.equals(cdmaCardType) || MtkIccCardConstants.CardType.CT_UIM_SIM_CARD.equals(cdmaCardType)) {
            z = true;
        } else {
            z = false;
        }
        if (FeatureOptions.LOG_ENABLE) {
            Log.d("@M_SIMHelper", "isCTCardType, ctCard = " + z + ", soltId = " + i);
        }
        return z;
    }
}
