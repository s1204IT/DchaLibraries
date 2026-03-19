package com.google.common.primitives;

import java.util.Arrays;

public final class Ints {
    private static final byte[] asciiDigits = new byte[128];

    public static int saturatedCast(long j) {
        if (j > 2147483647L) {
            return Integer.MAX_VALUE;
        }
        if (j < -2147483648L) {
            return Integer.MIN_VALUE;
        }
        return (int) j;
    }

    static {
        Arrays.fill(asciiDigits, (byte) -1);
        for (int i = 0; i <= 9; i++) {
            asciiDigits[48 + i] = (byte) i;
        }
        for (int i2 = 0; i2 <= 26; i2++) {
            byte b = (byte) (10 + i2);
            asciiDigits[65 + i2] = b;
            asciiDigits[97 + i2] = b;
        }
    }
}
