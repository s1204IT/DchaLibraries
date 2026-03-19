package com.mediatek.internal.telephony;

import com.mediatek.internal.telephony.ppl.PplMessageManager;

public class MtkEtwsUtils {
    public static final int ETWS_PDU_LENGTH = 56;

    public static byte[] intToBytes(int i) {
        byte[] bArr = new byte[4];
        for (int i2 = 0; i2 < 4; i2++) {
            bArr[3 - i2] = (byte) (i & 255);
            i >>= 8;
        }
        return bArr;
    }

    public static int bytesToInt(byte[] bArr) {
        if (bArr == null || bArr.length == 0 || bArr.length > 4) {
            throw new RuntimeException("valid byte array");
        }
        int length = bArr.length - 1;
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            i = (i | (bArr[i2] & PplMessageManager.Type.INVALID)) << 8;
        }
        return (bArr[length] & PplMessageManager.Type.INVALID) | i;
    }
}
