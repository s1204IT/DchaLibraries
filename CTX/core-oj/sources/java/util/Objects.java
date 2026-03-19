package java.util;

import java.util.function.Supplier;

public final class Objects {
    private Objects() {
        throw new AssertionError((Object) "No java.util.Objects instances for you!");
    }

    public static boolean equals(Object obj, Object obj2) {
        return obj == obj2 || (obj != null && obj.equals(obj2));
    }

    public static boolean deepEquals(Object obj, Object obj2) {
        if (obj == obj2) {
            return true;
        }
        if (obj == null || obj2 == null) {
            return false;
        }
        return Arrays.deepEquals0(obj, obj2);
    }

    public static int hashCode(Object obj) {
        if (obj != null) {
            return obj.hashCode();
        }
        return 0;
    }

    public static int hash(Object... objArr) {
        return Arrays.hashCode(objArr);
    }

    public static String toString(Object obj) {
        return String.valueOf(obj);
    }

    public static String toString(Object obj, String str) {
        return obj != null ? obj.toString() : str;
    }

    public static <T> int compare(T t, T t2, Comparator<? super T> comparator) {
        if (t == t2) {
            return 0;
        }
        return comparator.compare(t, t2);
    }

    public static <T> T requireNonNull(T t) {
        if (t == null) {
            throw new NullPointerException();
        }
        return t;
    }

    public static <T> T requireNonNull(T t, String str) {
        if (t == null) {
            throw new NullPointerException(str);
        }
        return t;
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    public static <T> T requireNonNull(T t, Supplier<String> supplier) {
        if (t == null) {
            throw new NullPointerException(supplier.get());
        }
        return t;
    }
}
