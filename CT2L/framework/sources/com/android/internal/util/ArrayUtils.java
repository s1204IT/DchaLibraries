package com.android.internal.util;

import android.util.ArraySet;
import dalvik.system.VMRuntime;
import java.lang.reflect.Array;
import java.util.ArrayList;
import libcore.util.EmptyArray;

public class ArrayUtils {
    private static final int CACHE_SIZE = 73;
    private static Object[] sCache = new Object[73];

    private ArrayUtils() {
    }

    public static byte[] newUnpaddedByteArray(int minLen) {
        return (byte[]) VMRuntime.getRuntime().newUnpaddedArray(Byte.TYPE, minLen);
    }

    public static char[] newUnpaddedCharArray(int minLen) {
        return (char[]) VMRuntime.getRuntime().newUnpaddedArray(Character.TYPE, minLen);
    }

    public static int[] newUnpaddedIntArray(int minLen) {
        return (int[]) VMRuntime.getRuntime().newUnpaddedArray(Integer.TYPE, minLen);
    }

    public static boolean[] newUnpaddedBooleanArray(int minLen) {
        return (boolean[]) VMRuntime.getRuntime().newUnpaddedArray(Boolean.TYPE, minLen);
    }

    public static long[] newUnpaddedLongArray(int minLen) {
        return (long[]) VMRuntime.getRuntime().newUnpaddedArray(Long.TYPE, minLen);
    }

    public static float[] newUnpaddedFloatArray(int minLen) {
        return (float[]) VMRuntime.getRuntime().newUnpaddedArray(Float.TYPE, minLen);
    }

    public static Object[] newUnpaddedObjectArray(int minLen) {
        return (Object[]) VMRuntime.getRuntime().newUnpaddedArray(Object.class, minLen);
    }

    public static <T> T[] newUnpaddedArray(Class<T> cls, int i) {
        return (T[]) ((Object[]) VMRuntime.getRuntime().newUnpaddedArray(cls, i));
    }

    public static boolean equals(byte[] array1, byte[] array2, int length) {
        if (length < 0) {
            throw new IllegalArgumentException();
        }
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length < length || array2.length < length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static <T> T[] emptyArray(Class<T> cls) {
        if (cls == Object.class) {
            return (T[]) EmptyArray.OBJECT;
        }
        int iHashCode = (cls.hashCode() & Integer.MAX_VALUE) % 73;
        Object objNewInstance = sCache[iHashCode];
        if (objNewInstance == null || objNewInstance.getClass().getComponentType() != cls) {
            objNewInstance = Array.newInstance((Class<?>) cls, 0);
            sCache[iHashCode] = objNewInstance;
        }
        return (T[]) ((Object[]) objNewInstance);
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(int[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(long[] array) {
        return array == null || array.length == 0;
    }

    public static <T> boolean contains(T[] array, T value) {
        return indexOf(array, value) != -1;
    }

    public static <T> int indexOf(T[] array, T value) {
        if (array == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                if (value == null) {
                    return i;
                }
            } else if (value != null && array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public static <T> boolean containsAll(T[] array, T[] check) {
        if (check == null) {
            return true;
        }
        for (T checkItem : check) {
            if (!contains(array, checkItem)) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(int[] array, int value) {
        if (array == null) {
            return false;
        }
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(long[] array, long value) {
        if (array == null) {
            return false;
        }
        for (long element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }

    public static long total(long[] array) {
        long total = 0;
        for (long value : array) {
            total += value;
        }
        return total;
    }

    public static <T> T[] appendElement(Class<T> cls, T[] tArr, T t) {
        int length;
        T[] tArr2;
        if (tArr != null) {
            length = tArr.length;
            tArr2 = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, length + 1));
            System.arraycopy(tArr, 0, tArr2, 0, length);
        } else {
            length = 0;
            tArr2 = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, 1));
        }
        tArr2[length] = t;
        return tArr2;
    }

    public static <T> T[] removeElement(Class<T> cls, T[] tArr, T t) {
        if (tArr != null) {
            int length = tArr.length;
            for (int i = 0; i < length; i++) {
                if (tArr[i] == t) {
                    if (length == 1) {
                        return null;
                    }
                    T[] tArr2 = (T[]) ((Object[]) Array.newInstance((Class<?>) cls, length - 1));
                    System.arraycopy(tArr, 0, tArr2, 0, i);
                    System.arraycopy(tArr, i + 1, tArr2, i, (length - i) - 1);
                    return tArr2;
                }
            }
        }
        return tArr;
    }

    public static int[] appendInt(int[] cur, int val) {
        if (cur == null) {
            return new int[]{val};
        }
        int N = cur.length;
        for (int i : cur) {
            if (i == val) {
                return cur;
            }
        }
        int[] ret = new int[N + 1];
        System.arraycopy(cur, 0, ret, 0, N);
        ret[N] = val;
        return ret;
    }

    public static int[] removeInt(int[] cur, int val) {
        if (cur == null) {
            return null;
        }
        int N = cur.length;
        for (int i = 0; i < N; i++) {
            if (cur[i] == val) {
                int[] ret = new int[N - 1];
                if (i > 0) {
                    System.arraycopy(cur, 0, ret, 0, i);
                }
                if (i < N - 1) {
                    System.arraycopy(cur, i + 1, ret, i, (N - i) - 1);
                    return ret;
                }
                return ret;
            }
        }
        return cur;
    }

    public static long[] appendLong(long[] cur, long val) {
        if (cur == null) {
            return new long[]{val};
        }
        int N = cur.length;
        for (long j : cur) {
            if (j == val) {
                return cur;
            }
        }
        long[] ret = new long[N + 1];
        System.arraycopy(cur, 0, ret, 0, N);
        ret[N] = val;
        return ret;
    }

    public static long[] removeLong(long[] cur, long val) {
        if (cur == null) {
            return null;
        }
        int N = cur.length;
        for (int i = 0; i < N; i++) {
            if (cur[i] == val) {
                long[] ret = new long[N - 1];
                if (i > 0) {
                    System.arraycopy(cur, 0, ret, 0, i);
                }
                if (i < N - 1) {
                    System.arraycopy(cur, i + 1, ret, i, (N - i) - 1);
                    return ret;
                }
                return ret;
            }
        }
        return cur;
    }

    public static long[] cloneOrNull(long[] array) {
        if (array != null) {
            return (long[]) array.clone();
        }
        return null;
    }

    public static <T> ArraySet<T> add(ArraySet<T> cur, T val) {
        if (cur == null) {
            cur = new ArraySet<>();
        }
        cur.add(val);
        return cur;
    }

    public static <T> ArraySet<T> remove(ArraySet<T> cur, T val) {
        if (cur == null) {
            return null;
        }
        cur.remove(val);
        if (cur.isEmpty()) {
            return null;
        }
        return cur;
    }

    public static <T> boolean contains(ArraySet<T> cur, T val) {
        if (cur != null) {
            return cur.contains(val);
        }
        return false;
    }

    public static <T> ArrayList<T> add(ArrayList<T> cur, T val) {
        if (cur == null) {
            cur = new ArrayList<>();
        }
        cur.add(val);
        return cur;
    }

    public static <T> ArrayList<T> remove(ArrayList<T> cur, T val) {
        if (cur == null) {
            return null;
        }
        cur.remove(val);
        if (cur.isEmpty()) {
            return null;
        }
        return cur;
    }

    public static <T> boolean contains(ArrayList<T> cur, T val) {
        if (cur != null) {
            return cur.contains(val);
        }
        return false;
    }
}
