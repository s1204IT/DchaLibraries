package com.android.internal.util;

public final class ParseUtils {
    private ParseUtils() {
    }

    public static int parseInt(String str, int i) {
        return parseIntWithBase(str, 10, i);
    }

    public static int parseIntWithBase(String str, int i, int i2) {
        if (str == null) {
            return i2;
        }
        try {
            return Integer.parseInt(str, i);
        } catch (NumberFormatException e) {
            return i2;
        }
    }

    public static long parseLong(String str, long j) {
        return parseLongWithBase(str, 10, j);
    }

    public static long parseLongWithBase(String str, int i, long j) {
        if (str == null) {
            return j;
        }
        try {
            return Long.parseLong(str, i);
        } catch (NumberFormatException e) {
            return j;
        }
    }

    public static float parseFloat(String str, float f) {
        if (str == null) {
            return f;
        }
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return f;
        }
    }

    public static double parseDouble(String str, double d) {
        if (str == null) {
            return d;
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return d;
        }
    }

    public static boolean parseBoolean(String str, boolean z) {
        if ("true".equals(str)) {
            return true;
        }
        return ("false".equals(str) || parseInt(str, z ? 1 : 0) == 0) ? false : true;
    }
}
