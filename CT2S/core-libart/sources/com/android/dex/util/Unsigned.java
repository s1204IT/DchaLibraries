package com.android.dex.util;

public final class Unsigned {
    private Unsigned() {
    }

    public static int compare(short ushortA, short ushortB) {
        if (ushortA == ushortB) {
            return 0;
        }
        int a = ushortA & 65535;
        int b = ushortB & 65535;
        return a < b ? -1 : 1;
    }

    public static int compare(int uintA, int uintB) {
        if (uintA == uintB) {
            return 0;
        }
        long a = ((long) uintA) & 4294967295L;
        long b = ((long) uintB) & 4294967295L;
        return a < b ? -1 : 1;
    }
}
