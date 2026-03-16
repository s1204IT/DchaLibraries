package java.util;

public final class Objects {
    private Objects() {
    }

    public static <T> int compare(T a, T b, Comparator<? super T> c) {
        if (a == b) {
            return 0;
        }
        return c.compare(a, b);
    }

    public static boolean deepEquals(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if ((a instanceof Object[]) && (b instanceof Object[])) {
            return Arrays.deepEquals((Object[]) a, (Object[]) b);
        }
        if ((a instanceof boolean[]) && (b instanceof boolean[])) {
            return Arrays.equals((boolean[]) a, (boolean[]) b);
        }
        if ((a instanceof byte[]) && (b instanceof byte[])) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        }
        if ((a instanceof char[]) && (b instanceof char[])) {
            return Arrays.equals((char[]) a, (char[]) b);
        }
        if ((a instanceof double[]) && (b instanceof double[])) {
            return Arrays.equals((double[]) a, (double[]) b);
        }
        if ((a instanceof float[]) && (b instanceof float[])) {
            return Arrays.equals((float[]) a, (float[]) b);
        }
        if ((a instanceof int[]) && (b instanceof int[])) {
            return Arrays.equals((int[]) a, (int[]) b);
        }
        if ((a instanceof long[]) && (b instanceof long[])) {
            return Arrays.equals((long[]) a, (long[]) b);
        }
        if ((a instanceof short[]) && (b instanceof short[])) {
            return Arrays.equals((short[]) a, (short[]) b);
        }
        return a.equals(b);
    }

    public static boolean equals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    public static int hashCode(Object o) {
        if (o == null) {
            return 0;
        }
        return o.hashCode();
    }

    public static <T> T requireNonNull(T o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return o;
    }

    public static <T> T requireNonNull(T o, String message) {
        if (o == null) {
            throw new NullPointerException(message);
        }
        return o;
    }

    public static String toString(Object o) {
        return o == null ? "null" : o.toString();
    }

    public static String toString(Object o, String nullString) {
        if (o == null) {
            return nullString;
        }
        String nullString2 = o.toString();
        return nullString2;
    }
}
