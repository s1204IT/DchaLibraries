package com.mediatek.phone.ext;

import android.content.Context;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.Connection;

public class DefaultDigitsUtilExt implements IDigitsUtilExt {
    @Override
    public PhoneAccountHandle convertToPstnPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle, Context context) {
        return phoneAccountHandle;
    }

    @Override
    public PhoneAccountHandle convertToPstnPhoneAccount(PhoneAccount phoneAccount) {
        return phoneAccount.getAccountHandle();
    }

    @Override
    public String getVirtualLineNumber(PhoneAccountHandle phoneAccountHandle) {
        return "";
    }

    @Override
    public PhoneAccountHandle makeVirtualPhoneAccountHandle(String str, String str2) {
        return null;
    }

    @Override
    public boolean isVirtualPhoneAccount(PhoneAccountHandle phoneAccountHandle, Context context) {
        return false;
    }

    @Override
    public boolean isPotentialVirtualPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        return false;
    }

    @Override
    public Object replaceTelecomAccountRegistry(Object obj, Context context) {
        return obj;
    }

    @Override
    public Bundle putLineNumberToExtras(Bundle bundle, Context context) {
        return bundle;
    }

    @Override
    public boolean isConnectionMatched(Connection connection, PhoneAccountHandle phoneAccountHandle, Context context) {
        return true;
    }

    @Override
    public PhoneAccountHandle getCorrectPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle, PhoneAccountHandle phoneAccountHandle2, Context context) {
        return phoneAccountHandle;
    }

    @Override
    public void setPhoneAccountHandle(Object obj, PhoneAccountHandle phoneAccountHandle) {
    }

    @Override
    public String getIccidFromPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        return phoneAccountHandle.getId();
    }

    @Override
    public PhoneAccountHandle getHandleByConnectionIfRequired(PhoneAccountHandle phoneAccountHandle, Object obj) {
        return phoneAccountHandle;
    }

    @Override
    public boolean areConnectionsInSameLine(Object obj, Object obj2) {
        return true;
    }
}
