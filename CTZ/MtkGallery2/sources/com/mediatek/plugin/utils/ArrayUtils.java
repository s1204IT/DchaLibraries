package com.mediatek.plugin.utils;

public class ArrayUtils {
    public static <T> boolean areExactMatch(T[] tArr, T[] tArr2) {
        return tArr.length == tArr2.length && containsAll(tArr, tArr2) && containsAll(tArr2, tArr);
    }

    public static <T> boolean isEmpty(T[] tArr) {
        return tArr == null || tArr.length == 0;
    }

    public static <T> boolean containsAll(T[] tArr, T[] tArr2) {
        if (tArr2 == null) {
            return true;
        }
        for (T t : tArr2) {
            if (!contains(tArr, t)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean contains(T[] tArr, T t) {
        return indexOf(tArr, t) != -1;
    }

    public static <T> int indexOf(T[] tArr, T t) {
        if (tArr == null) {
            return -1;
        }
        for (int i = 0; i < tArr.length; i++) {
            if (tArr[i] == null && t == null) {
                return i;
            }
            if ((tArr[i] != null || t == null) && tArr[i].equals(t)) {
                return i;
            }
        }
        return -1;
    }
}
