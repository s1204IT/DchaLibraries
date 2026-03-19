package com.android.bluetooth.util;

public class NumberUtils {
    public static int unsignedByteToInt(byte b) {
        return b & 255;
    }

    public static int littleEndianByteArrayToInt(byte[] bArr) {
        int length = bArr.length;
        int iUnsignedByteToInt = 0;
        if (length == 0) {
            return 0;
        }
        for (int i = length - 1; i >= 0; i--) {
            iUnsignedByteToInt += unsignedByteToInt(bArr[i]) << (i * 8);
        }
        return iUnsignedByteToInt;
    }
}
