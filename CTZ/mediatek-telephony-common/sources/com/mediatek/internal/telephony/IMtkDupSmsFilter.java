package com.mediatek.internal.telephony;

public interface IMtkDupSmsFilter {
    boolean containDupSms(byte[] bArr);

    void setPhoneId(int i);
}
