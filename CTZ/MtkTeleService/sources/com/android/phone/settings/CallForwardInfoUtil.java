package com.android.phone.settings;

import android.os.Message;
import android.util.Log;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.Phone;

public class CallForwardInfoUtil {
    private static final String LOG_TAG = CallForwardInfoUtil.class.getSimpleName();

    public static CallForwardInfo infoForReason(CallForwardInfo[] callForwardInfoArr, int i) {
        if (callForwardInfoArr == null) {
            return null;
        }
        for (int i2 = 0; i2 < callForwardInfoArr.length; i2++) {
            if (callForwardInfoArr[i2].reason == i) {
                return callForwardInfoArr[i2];
            }
        }
        return null;
    }

    public static boolean isUpdateRequired(CallForwardInfo callForwardInfo, CallForwardInfo callForwardInfo2) {
        if (callForwardInfo == null || callForwardInfo2.status != 0 || callForwardInfo.status != 0) {
            return true;
        }
        return false;
    }

    public static void setCallForwardingOption(Phone phone, CallForwardInfo callForwardInfo, Message message) {
        int i;
        if (callForwardInfo.status == 1) {
            i = 3;
        } else {
            i = 0;
        }
        phone.setCallForwardingOption(i, callForwardInfo.reason, callForwardInfo.number, callForwardInfo.timeSeconds, message);
    }

    public static CallForwardInfo getCallForwardInfo(CallForwardInfo[] callForwardInfoArr, int i) {
        CallForwardInfo callForwardInfo;
        int i2 = 0;
        while (true) {
            if (i2 < callForwardInfoArr.length) {
                if (!isServiceClassVoice(callForwardInfoArr[i2])) {
                    i2++;
                } else {
                    callForwardInfo = callForwardInfoArr[i2];
                    break;
                }
            } else {
                callForwardInfo = null;
                break;
            }
        }
        if (callForwardInfo == null) {
            CallForwardInfo callForwardInfo2 = new CallForwardInfo();
            callForwardInfo2.status = 0;
            callForwardInfo2.reason = i;
            callForwardInfo2.serviceClass = 1;
            Log.d(LOG_TAG, "Created default info for reason: " + i);
            return callForwardInfo2;
        }
        if (!hasForwardingNumber(callForwardInfo)) {
            callForwardInfo.status = 0;
        }
        Log.d(LOG_TAG, "Retrieved  " + callForwardInfo.toString() + " for " + i);
        return callForwardInfo;
    }

    private static boolean isServiceClassVoice(CallForwardInfo callForwardInfo) {
        return (callForwardInfo.serviceClass & 1) != 0;
    }

    private static boolean hasForwardingNumber(CallForwardInfo callForwardInfo) {
        return callForwardInfo.number != null && callForwardInfo.number.length() > 0;
    }
}
