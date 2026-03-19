package com.mediatek.phone.ext;

import android.content.Context;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.Connection;

public interface IDigitsUtilExt {
    boolean areConnectionsInSameLine(Object obj, Object obj2);

    PhoneAccountHandle convertToPstnPhoneAccount(PhoneAccount phoneAccount);

    PhoneAccountHandle convertToPstnPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle, Context context);

    PhoneAccountHandle getCorrectPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle, PhoneAccountHandle phoneAccountHandle2, Context context);

    PhoneAccountHandle getHandleByConnectionIfRequired(PhoneAccountHandle phoneAccountHandle, Object obj);

    String getIccidFromPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle);

    String getVirtualLineNumber(PhoneAccountHandle phoneAccountHandle);

    boolean isConnectionMatched(Connection connection, PhoneAccountHandle phoneAccountHandle, Context context);

    boolean isPotentialVirtualPhoneAccount(PhoneAccountHandle phoneAccountHandle);

    boolean isVirtualPhoneAccount(PhoneAccountHandle phoneAccountHandle, Context context);

    PhoneAccountHandle makeVirtualPhoneAccountHandle(String str, String str2);

    Bundle putLineNumberToExtras(Bundle bundle, Context context);

    Object replaceTelecomAccountRegistry(Object obj, Context context);

    void setPhoneAccountHandle(Object obj, PhoneAccountHandle phoneAccountHandle);
}
