package android.support.v4.util;

public class Preconditions {
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    public static int checkArgumentNonnegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException();
        }
        return value;
    }
}
