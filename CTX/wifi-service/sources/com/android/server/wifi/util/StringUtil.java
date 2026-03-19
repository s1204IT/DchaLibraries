package com.android.server.wifi.util;

public class StringUtil {
    static final byte ASCII_PRINTABLE_MAX = 126;
    static final byte ASCII_PRINTABLE_MIN = 32;

    public static boolean isAsciiPrintable(byte[] bArr) {
        if (bArr == null) {
            return true;
        }
        for (byte b : bArr) {
            if (b != 7) {
                switch (b) {
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                        continue;
                    default:
                        if (b < 32 || b > 126) {
                            return false;
                        }
                        break;
                        break;
                }
            }
        }
        return true;
    }
}
