package com.mediatek.internal.telephony.devreg;

public interface IDeviceRegisterExt {
    void handleAutoRegMessage(byte[] bArr);

    void setCdmaCardEsnOrMeid(String str);
}
