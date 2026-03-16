package com.android.bluetooth.util;

public class NumberUtils {
    public static int unsignedByteToInt(byte b) {
        return b & 255;
    }

    public static int littleEndianByteArrayToInt(byte[] bytes) {
        int length = bytes.length;
        if (length == 0) {
            return 0;
        }
        int result = 0;
        for (int i = length - 1; i >= 0; i--) {
            int value = unsignedByteToInt(bytes[i]);
            result += value << (i * 8);
        }
        return result;
    }
}
