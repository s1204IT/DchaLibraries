package com.android.internal.util;

public class ObjectUtils {
    private ObjectUtils() {
    }

    public static <T> T firstNotNull(T t, T t2) {
        return t != null ? t : (T) Preconditions.checkNotNull(t2);
    }

    public static <T extends Comparable> int compare(T t, T t2) {
        if (t == null) {
            return t2 != null ? -1 : 0;
        }
        if (t2 != null) {
            return t.compareTo(t2);
        }
        return 1;
    }
}
