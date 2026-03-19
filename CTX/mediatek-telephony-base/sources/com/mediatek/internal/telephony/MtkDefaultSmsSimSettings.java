package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import java.util.Iterator;
import java.util.List;

public class MtkDefaultSmsSimSettings {
    public static final int ASK_USER_SUB_ID = -2;
    private static final String TAG = "MTKDefaultSmsSimSettings";

    public static void setSmsTalkDefaultSim(List<SubscriptionInfo> list, Context context) {
        int defaultSmsSubscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
        Log.i(TAG, "oldSmsDefaultSIM" + defaultSmsSubscriptionId);
        if (list == null) {
            Log.i(TAG, "subInfos == null, return");
            return;
        }
        Log.i(TAG, "subInfos size = " + list.size());
        if (list.size() > 1) {
            if (isoldDefaultSMSSubIdActive(list)) {
                Log.i(TAG, "subInfos size > 1 & old available, set to :" + defaultSmsSubscriptionId);
                return;
            }
            if ("OP01".equals(SystemProperties.get("persist.vendor.operator.optr"))) {
                Log.i(TAG, "subInfos size > 1, set to : AUTO");
                SubscriptionManager.from(context).setDefaultSmsSubId(-3);
                return;
            }
            if ("OP09".equals(SystemProperties.get("persist.vendor.operator.optr")) && "SEGDEFAULT".equals(SystemProperties.get("persist.vendor.operator.seg"))) {
                SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(0);
                int subscriptionId = -1;
                if (activeSubscriptionInfoForSimSlotIndex != null) {
                    subscriptionId = activeSubscriptionInfoForSimSlotIndex.getSubscriptionId();
                }
                SubscriptionManager.from(context).setDefaultSmsSubId(subscriptionId);
                Log.i(TAG, "subInfos size > 1, set to " + subscriptionId);
                return;
            }
            Log.i(TAG, "subInfos size > 1, set to : ASK_USER_SUB_ID");
            return;
        }
        if (list.size() == 1) {
            Log.i(TAG, "sub size = 1,segment = " + SystemProperties.get("persist.vendor.operator.seg"));
            if ("OP09".equals(SystemProperties.get("persist.vendor.operator.optr")) && "SEGDEFAULT".equals(SystemProperties.get("persist.vendor.operator.seg"))) {
                int subscriptionId2 = list.get(0).getSubscriptionId();
                SubscriptionManager.from(context).setDefaultSmsSubId(subscriptionId2);
                Log.i(TAG, "subInfos size = 1, set to " + subscriptionId2);
                return;
            }
            if ("OP01".equals(SystemProperties.get("persist.vendor.operator.optr"))) {
                int subscriptionId3 = list.get(0).getSubscriptionId();
                SubscriptionManager.from(context).setDefaultSmsSubId(subscriptionId3);
                Log.i(TAG, "subInfos size = 1, set to " + subscriptionId3);
                return;
            }
            return;
        }
        Log.i(TAG, "setSmsTalkDefaultSim SIM not insert");
    }

    private static boolean isoldDefaultSMSSubIdActive(List<SubscriptionInfo> list) {
        int defaultSmsSubscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
        Iterator<SubscriptionInfo> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().getSubscriptionId() == defaultSmsSubscriptionId) {
                return true;
            }
        }
        if ("OP01".equals(SystemProperties.get("persist.vendor.operator.optr"))) {
            return defaultSmsSubscriptionId == -2 || defaultSmsSubscriptionId == -3;
        }
        return false;
    }
}
