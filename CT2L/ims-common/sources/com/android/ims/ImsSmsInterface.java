package com.android.ims;

import android.os.Message;

public interface ImsSmsInterface {
    void acknowledgeIncomingGsmSmsWithPdu(boolean z, String str, Message message);

    void acknowledgeLastIncomingCdmaSms(boolean z, int i, Message message);

    void acknowledgeLastIncomingGsmSms(boolean z, int i, Message message);

    void sendImsCdmaSms(byte[] bArr, int i, int i2, Message message);

    void sendImsGsmSms(String str, String str2, int i, int i2, Message message);
}
