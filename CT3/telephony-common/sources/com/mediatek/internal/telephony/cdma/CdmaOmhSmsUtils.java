package com.mediatek.internal.telephony.cdma;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import com.mediatek.internal.telephony.ITelephonyEx;

public class CdmaOmhSmsUtils {
    public static final int INVALID_BROADCAST_CONFIG = -1;
    public static final int INVALID_MESSAGE_ID = -1;
    private static final String TAG = "CdmaOmhSmsUtils";

    public static boolean isOmhCard(int subId) {
        boolean isOmh = false;
        try {
            ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            isOmh = iTel.isOmhCard(subId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Rlog.d(TAG, "isOmhCard " + isOmh);
        return isOmh;
    }

    public static int getNextMessageId(int subId) {
        int id = -1;
        try {
            ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            id = iTel.getNextMessageId(subId);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Rlog.d(TAG, "getNextMessageId " + id);
        return id;
    }

    public static int getBcsmsCfgFromRuim(int subId, int userCategory, int userPriority) {
        int ret = -1;
        try {
            ITelephonyEx iTel = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            ret = iTel.getBcsmsCfgFromRuim(subId, userCategory, userPriority);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        Rlog.d(TAG, "getBcsmsCfgFromRuim " + ret);
        return ret;
    }
}
