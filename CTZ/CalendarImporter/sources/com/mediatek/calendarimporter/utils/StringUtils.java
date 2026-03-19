package com.mediatek.calendarimporter.utils;

public final class StringUtils {
    private StringUtils() {
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }
}
