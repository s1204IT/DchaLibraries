package com.mediatek.settings.cdma;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.mediatek.telephony.MtkTelephonyManagerEx;

/* loaded from: classes.dex */
public class CdmaUtils {
    public static boolean isSupportCdma(int i) {
        boolean z;
        if (TelephonyManager.getDefault().getCurrentPhoneType(i) == 2) {
            z = true;
        } else {
            z = false;
        }
        Log.d("CdmaUtils", "isSupportCdma=" + z + ", subId=" + i);
        return z;
    }

    public static boolean isCdmaCard(int i) {
        int simType = getSimType(i);
        boolean z = simType == 2 || simType == 3;
        Log.d("CdmaUtils", "isCdmaCard, simType=" + simType + ", isCdma=" + z);
        return z;
    }

    public static int getSimType(int i) {
        int iccAppFamily;
        MtkTelephonyManagerEx mtkTelephonyManagerEx = MtkTelephonyManagerEx.getDefault();
        if (mtkTelephonyManagerEx != null) {
            iccAppFamily = mtkTelephonyManagerEx.getIccAppFamily(i);
        } else {
            iccAppFamily = 0;
        }
        Log.d("CdmaUtils", "simType=" + iccAppFamily + ", slotId=" + i);
        return iccAppFamily;
    }

    public static boolean isCdmaCardCompetion(Context context) {
        int simCount;
        boolean z;
        boolean z2;
        if (context != null) {
            simCount = TelephonyManager.from(context).getSimCount();
        } else {
            simCount = 0;
        }
        if (simCount == 2) {
            int i = 0;
            z = true;
            z2 = true;
            while (true) {
                if (i < simCount) {
                    z = z && isCdmaCard(i);
                    SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(i);
                    if (activeSubscriptionInfoForSimSlotIndex != null) {
                        z2 = z2 && MtkTelephonyManagerEx.getDefault().isInHomeNetwork(activeSubscriptionInfoForSimSlotIndex.getSubscriptionId());
                        i++;
                    } else {
                        z2 = false;
                        break;
                    }
                } else {
                    break;
                }
            }
        } else {
            z = false;
            z2 = false;
        }
        Log.d("CdmaUtils", "isCdma=" + z + ", isCompletition=" + z2);
        return z && z2;
    }

    public static boolean isCdmaCardCompetionForData(Context context) {
        return isCdmaCardCompetion(context);
    }
}
