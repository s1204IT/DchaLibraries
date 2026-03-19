package com.mediatek.internal.telephony;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.mediatek.internal.telephony.IMtkSub;

public class MtkSubscriptionManager {
    private static final boolean DBG = false;
    public static final int EXTRA_VALUE_NEW_SIM = 1;
    public static final int EXTRA_VALUE_NOCHANGE = 4;
    public static final int EXTRA_VALUE_REMOVE_SIM = 2;
    public static final int EXTRA_VALUE_REPOSITION_SIM = 3;
    public static final String INTENT_KEY_DETECT_STATUS = "simDetectStatus";
    public static final String INTENT_KEY_NEW_SIM_SLOT = "newSIMSlot";
    public static final String INTENT_KEY_NEW_SIM_STATUS = "newSIMStatus";
    public static final String INTENT_KEY_PROP_KEY = "simPropKey";
    public static final String INTENT_KEY_SIM_COUNT = "simCount";
    private static final String LOG_TAG = "MtkSubscriptionManager";
    private static final boolean VDBG = false;

    public static MtkSubscriptionInfo getSubInfo(String str, int i) {
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            Rlog.d(LOG_TAG, "[getSubInfo]- invalid subId, subId = " + i);
            return null;
        }
        try {
            IMtkSub iMtkSubAsInterface = IMtkSub.Stub.asInterface(ServiceManager.getService("isubstub"));
            if (iMtkSubAsInterface != null) {
                return iMtkSubAsInterface.getSubInfo(str, i);
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public static MtkSubscriptionInfo getSubInfoForIccId(String str, String str2) {
        if (str2 == null) {
            Rlog.d(LOG_TAG, "[getSubInfoForIccId]- null iccid");
            return null;
        }
        try {
            IMtkSub iMtkSubAsInterface = IMtkSub.Stub.asInterface(ServiceManager.getService("isubstub"));
            if (iMtkSubAsInterface == null) {
                return null;
            }
            return iMtkSubAsInterface.getSubInfoForIccId(str, str2);
        } catch (RemoteException e) {
            return null;
        }
    }

    public static int getSubIdUsingPhoneId(int i) {
        try {
            IMtkSub iMtkSubAsInterface = IMtkSub.Stub.asInterface(ServiceManager.getService("isubstub"));
            if (iMtkSubAsInterface == null) {
                return -1;
            }
            return iMtkSubAsInterface.getSubIdUsingPhoneId(i);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static void setDefaultSubId(int i) {
        if (i <= 0) {
            printStackTrace("setDefaultSubId subId 0");
        }
        try {
            IMtkSub iMtkSubAsInterface = IMtkSub.Stub.asInterface(ServiceManager.getService("isubstub"));
            if (iMtkSubAsInterface != null) {
                iMtkSubAsInterface.setDefaultFallbackSubId(i);
            }
        } catch (RemoteException e) {
        }
    }

    public static void setDefaultDataSubIdWithoutCapabilitySwitch(int i) {
        if (i <= 0) {
            printStackTrace("setDefaultDataSubIdWithoutCapabilitySwitch subId 0");
        }
        try {
            IMtkSub iMtkSubAsInterface = IMtkSub.Stub.asInterface(ServiceManager.getService("isubstub"));
            if (iMtkSubAsInterface != null) {
                iMtkSubAsInterface.setDefaultDataSubIdWithoutCapabilitySwitch(i);
            }
        } catch (RemoteException e) {
        }
    }

    private static void printStackTrace(String str) {
        RuntimeException runtimeException = new RuntimeException();
        Rlog.d(LOG_TAG, "StackTrace - " + str);
        for (StackTraceElement stackTraceElement : runtimeException.getStackTrace()) {
            Rlog.d(LOG_TAG, stackTraceElement.toString());
        }
    }
}
