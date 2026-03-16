package com.android.phone.settings;

import android.os.Message;
import android.util.Log;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.Phone;

public class CallForwardInfoUtil {
    private static final String LOG_TAG = CallForwardInfoUtil.class.getSimpleName();

    public static CallForwardInfo infoForReason(CallForwardInfo[] infos, int reason) {
        if (infos == null) {
            return null;
        }
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].reason == reason) {
                return infos[i];
            }
        }
        return null;
    }

    public static boolean isUpdateRequired(CallForwardInfo oldInfo, CallForwardInfo newInfo) {
        return (oldInfo != null && newInfo.status == 0 && oldInfo.status == 0) ? false : true;
    }

    public static void setCallForwardingOption(Phone phone, CallForwardInfo info, Message message) {
        int commandInterfaceCfAction = info.status == 1 ? 3 : 0;
        phone.setCallForwardingOption(commandInterfaceCfAction, info.reason, info.number, info.timeSeconds, message);
    }

    public static CallForwardInfo getCallForwardInfo(CallForwardInfo[] infos, int reason) {
        CallForwardInfo info = null;
        int i = 0;
        while (true) {
            if (i >= infos.length) {
                break;
            }
            if (!isServiceClassVoice(infos[i])) {
                i++;
            } else {
                info = infos[i];
                break;
            }
        }
        if (info == null) {
            CallForwardInfo info2 = new CallForwardInfo();
            info2.status = 0;
            info2.reason = reason;
            info2.serviceClass = 1;
            Log.d(LOG_TAG, "Created default info for reason: " + reason);
            return info2;
        }
        if (!hasForwardingNumber(info)) {
            info.status = 0;
        }
        Log.d(LOG_TAG, "Retrieved  " + info.toString() + " for " + reason);
        return info;
    }

    private static boolean isServiceClassVoice(CallForwardInfo info) {
        return (info.serviceClass & 1) != 0;
    }

    private static boolean hasForwardingNumber(CallForwardInfo info) {
        return info.number != null && info.number.length() > 0;
    }
}
