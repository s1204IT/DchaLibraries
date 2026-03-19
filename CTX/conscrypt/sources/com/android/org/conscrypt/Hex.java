package com.android.org.conscrypt;

public final class Hex {
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private Hex() {
    }

    public static String bytesToHexString(byte[] bArr) {
        char[] cArr = new char[bArr.length * 2];
        int i = 0;
        for (byte b : bArr) {
            int i2 = i + 1;
            cArr[i] = DIGITS[(b >> 4) & 15];
            i = i2 + 1;
            cArr[i2] = DIGITS[b & 15];
        }
        return new String(cArr);
    }

    public static String intToHexString(int i, int i2) {
        int i3;
        char[] cArr = new char[8];
        int i4 = i;
        int i5 = 8;
        while (true) {
            i5--;
            cArr[i5] = DIGITS[i4 & 15];
            i4 >>>= 4;
            if (i4 == 0 && (i3 = 8 - i5) >= i2) {
                return new String(cArr, i5, i3);
            }
        }
    }
}
