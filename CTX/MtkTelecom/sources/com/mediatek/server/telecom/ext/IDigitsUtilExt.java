package com.mediatek.server.telecom.ext;

import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import java.util.Comparator;
import java.util.List;

public interface IDigitsUtilExt {
    void putExtrasForConnectionRequest(Bundle bundle, Object obj);

    void sortPhoneAccounts(List<PhoneAccount> list, Comparator<PhoneAccount> comparator, Comparator<PhoneAccount> comparator2, Comparator<PhoneAccount> comparator3);

    void updatePhoneAccounts(List<PhoneAccountHandle> list, PhoneAccountHandle phoneAccountHandle);
}
