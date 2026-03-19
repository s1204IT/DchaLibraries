package com.android.gallery3d.common;

import android.os.Build;

public class Utils {
    private static final boolean IS_DEBUG_BUILD;
    private static long[] sCrcTable = new long[256];

    static {
        IS_DEBUG_BUILD = Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");
        for (int i = 0; i < 256; i++) {
            long j = i;
            for (int i2 = 0; i2 < 8; i2++) {
                j = (j >> 1) ^ ((((int) j) & 1) != 0 ? -7661587058870466123L : 0L);
            }
            sCrcTable[i] = j;
        }
    }

    public static void assertTrue(boolean z) {
        if (!z) {
            throw new AssertionError();
        }
    }

    public static boolean isOpaque(int i) {
        return (i >>> 24) == 255;
    }
}
