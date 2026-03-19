package com.android.phone.vvm;

import android.telecom.PhoneAccountHandle;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneUtils;

public class PhoneAccountHandleConverter {
    public static PhoneAccountHandle fromSubId(int i) {
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            VvmLog.e("PhoneAccountHndCvtr", "invalid subId " + i);
            return null;
        }
        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i));
        if (phone == null) {
            VvmLog.e("PhoneAccountHndCvtr", "Unable to find Phone for subId " + i);
            return null;
        }
        return PhoneUtils.makePstnPhoneAccountHandle(phone);
    }

    public static int toSubId(PhoneAccountHandle phoneAccountHandle) {
        return PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccountHandle);
    }
}
