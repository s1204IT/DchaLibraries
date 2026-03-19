package com.android.server.wifi;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import java.util.ArrayList;
import java.util.List;

public class SIMAccessor {
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    public SIMAccessor(Context context) {
        this.mTelephonyManager = TelephonyManager.from(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
    }

    public List<String> getMatchingImsis(IMSIParameter iMSIParameter) {
        if (iMSIParameter == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (int i : this.mSubscriptionManager.getActiveSubscriptionIdList()) {
            String subscriberId = this.mTelephonyManager.getSubscriberId(i);
            if (subscriberId != null && iMSIParameter.matchesImsi(subscriberId)) {
                arrayList.add(subscriberId);
            }
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        return arrayList;
    }
}
