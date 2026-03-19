package com.android.providers.contacts.util;

public class Hex {
    private static final byte[] DIGITS;
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] FIRST_CHAR = new char[256];
    private static final char[] SECOND_CHAR = new char[256];

    static {
        for (int i = 0; i < 256; i++) {
            FIRST_CHAR[i] = HEX_DIGITS[(i >> 4) & 15];
            SECOND_CHAR[i] = HEX_DIGITS[i & 15];
        }
        DIGITS = new byte[103];
        for (int i2 = 0; i2 <= 70; i2++) {
            DIGITS[i2] = -1;
        }
        for (byte b = 0; b < 10; b = (byte) (b + 1)) {
            DIGITS[48 + b] = b;
        }
        for (byte b2 = 0; b2 < 6; b2 = (byte) (b2 + 1)) {
            byte b3 = (byte) (10 + b2);
            DIGITS[65 + b2] = b3;
            DIGITS[97 + b2] = b3;
        }
    }

    public static String encodeHex(byte[] bArr, boolean z) {
        char[] cArr = new char[bArr.length * 2];
        int i = 0;
        for (int i2 = 0; i2 < bArr.length; i2++) {
            int i3 = bArr[i2] & 255;
            if (z && i3 == 0 && i2 == bArr.length - 1) {
                break;
            }
            int i4 = i + 1;
            cArr[i] = FIRST_CHAR[i3];
            i = i4 + 1;
            cArr[i4] = SECOND_CHAR[i3];
        }
        return new String(cArr, 0, i);
    }

    public static byte[] decodeHex(String str) {
        boolean z;
        byte b;
        byte b2;
        int length = str.length();
        if ((length & 1) != 0) {
            throw new IllegalArgumentException("Odd number of characters: " + str);
        }
        byte[] bArr = new byte[length >> 1];
        int i = 0;
        int i2 = 0;
        while (true) {
            z = true;
            if (i < length) {
                int i3 = i + 1;
                char cCharAt = str.charAt(i);
                if (cCharAt > 'f' || (b = DIGITS[cCharAt]) == -1) {
                    break;
                }
                int i4 = i3 + 1;
                char cCharAt2 = str.charAt(i3);
                if (cCharAt2 > 'f' || (b2 = DIGITS[cCharAt2]) == -1) {
                    break;
                }
                bArr[i2] = (byte) ((b << 4) | b2);
                i2++;
                i = i4;
            } else {
                z = false;
                break;
            }
        }
        if (z) {
            throw new IllegalArgumentException("Invalid hexadecimal digit: " + str);
        }
        return bArr;
    }
}
