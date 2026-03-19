package com.mediatek.internal.telephony;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.internal.telephony.IMtkTelephonyRegistryEx;
import java.util.Arrays;
import mediatek.telephony.MtkServiceState;

public class MtkPhoneNotifier extends DefaultPhoneNotifier {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "MtkPhoneNotifr";
    private final int mFakeSub = 2147483646;
    protected IMtkTelephonyRegistryEx mMtkRegistry;

    public MtkPhoneNotifier() {
        Rlog.d(LOG_TAG, "constructor");
        MtkTelephonyRegistryEx.init();
        this.mMtkRegistry = IMtkTelephonyRegistryEx.Stub.asInterface(ServiceManager.getService("telephony.mtkregistry"));
    }

    private boolean checkSubIdPhoneId(Phone phone) {
        if (phone.getSubId() >= 0) {
            return true;
        }
        int defaultFallbackSubId = MtkSubscriptionController.getMtkInstance().getDefaultFallbackSubId();
        int phoneId = MtkSubscriptionController.getMtkInstance().getPhoneId(defaultFallbackSubId);
        int[] activeSubIdList = MtkSubscriptionController.getMtkInstance().getActiveSubIdList();
        Rlog.d(LOG_TAG, "checkSubIdPhoneId defaultFallbackSubId:" + defaultFallbackSubId + " defaultFallbackPhoneId:" + phoneId + " sender's SubId:" + phone.getSubId() + " activeSubIds: " + Arrays.toString(activeSubIdList));
        return activeSubIdList.length == 0;
    }

    public void notifyServiceState(Phone phone) {
        if (checkSubIdPhoneId(phone)) {
            super.notifyServiceState(phone);
            return;
        }
        MtkServiceState serviceState = phone.getServiceState();
        int phoneId = phone.getPhoneId();
        if (serviceState == null) {
            serviceState = new MtkServiceState();
            serviceState.setStateOutOfService();
        }
        Rlog.d(LOG_TAG, "MtkPhoneNotifier notifyServiceState phoneId:" + phoneId + " fake subId: 2147483646 ServiceState: " + serviceState);
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyServiceStateForPhoneId(phoneId, 2147483646, serviceState);
            }
        } catch (RemoteException e) {
        }
    }

    public void notifySignalStrength(Phone phone) {
        if (checkSubIdPhoneId(phone)) {
            super.notifySignalStrength(phone);
            return;
        }
        int phoneId = phone.getPhoneId();
        Rlog.d(LOG_TAG, "MtkPhoneNotifier notifySignalStrength phoneId:" + phoneId + " fake subId: 2147483646 signal: " + phone.getSignalStrength());
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifySignalStrengthForPhoneId(phoneId, 2147483646, phone.getSignalStrength());
            }
        } catch (RemoteException e) {
        }
    }

    public void notifyDataConnection(Phone phone, String str, String str2, PhoneConstants.DataState dataState) {
        if (phone.getActiveApnHost(str2) == null && !"default".equals(str2) && !"emergency".equals(str2) && !"preempt".equals(str2)) {
            return;
        }
        super.notifyDataConnection(phone, str, str2, dataState);
    }
}
