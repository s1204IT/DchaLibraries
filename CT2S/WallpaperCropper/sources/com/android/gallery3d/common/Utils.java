package com.android.gallery3d.common;

import android.os.Build;
import android.util.Log;
import java.io.Closeable;
import java.io.IOException;

public class Utils {
    private static final boolean IS_DEBUG_BUILD;
    private static long[] sCrcTable = new long[256];

    static {
        IS_DEBUG_BUILD = Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");
        for (int i = 0; i < 256; i++) {
            long part = i;
            for (int j = 0; j < 8; j++) {
                long x = (((int) part) & 1) != 0 ? -7661587058870466123L : 0L;
                part = (part >> 1) ^ x;
            }
            sCrcTable[i] = part;
        }
    }

    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > 1073741824) {
            throw new IllegalArgumentException("n is invalid: " + n);
        }
        int n2 = n - 1;
        int n3 = n2 | (n2 >> 16);
        int n4 = n3 | (n3 >> 8);
        int n5 = n4 | (n4 >> 4);
        int n6 = n5 | (n5 >> 2);
        return (n6 | (n6 >> 1)) + 1;
    }

    public static int prevPowerOf2(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.highestOneBit(n);
    }

    public static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        return x < min ? min : x;
    }

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException t) {
                Log.w("Utils", "close fail ", t);
            }
        }
    }

    public static int ceilLog2(float value) {
        int i = 0;
        while (i < 31 && (1 << i) < value) {
            i++;
        }
        return i;
    }

    public static int floorLog2(float value) {
        int i = 0;
        while (i < 31 && (1 << i) <= value) {
            i++;
        }
        return i - 1;
    }
}
