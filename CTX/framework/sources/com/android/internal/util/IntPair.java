package com.android.internal.util;

public class IntPair {
    private IntPair() {
    }

    public static long of(int i, int i2) {
        return (((long) i2) & 4294967295L) | (((long) i) << 32);
    }

    public static int first(long j) {
        return (int) (j >> 32);
    }

    public static int second(long j) {
        return (int) j;
    }
}
