package com.google.common.primitives;

import android.support.v7.preference.Preference;
import com.android.settingslib.wifi.AccessPoint;
import java.util.Arrays;

public final class Ints {
    private static final byte[] asciiDigits = new byte[128];

    public static int saturatedCast(long j) {
        if (j > 2147483647L) {
            return Preference.DEFAULT_ORDER;
        }
        if (j < -2147483648L) {
            return AccessPoint.UNREACHABLE_RSSI;
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
