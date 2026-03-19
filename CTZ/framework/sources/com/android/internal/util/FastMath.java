package com.android.internal.util;

public class FastMath {
    public static int round(float f) {
        return (int) ((((long) (f * 1.6777216E7f)) + 8388608) >> 24);
    }
}
