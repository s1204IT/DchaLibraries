package android.app.timezone;

final class Utils {
    private Utils() {
    }

    static int validateVersion(String str, int i) {
        if (i < 0 || i > 999) {
            throw new IllegalArgumentException("Invalid " + str + " version=" + i);
        }
        return i;
    }

    static String validateRulesVersion(String str, String str2) {
        validateNotNull(str, str2);
        if (str2.isEmpty()) {
            throw new IllegalArgumentException(str + " must not be empty");
        }
        return str2;
    }

    static <T> T validateNotNull(String str, T t) {
        if (t == null) {
            throw new NullPointerException(str + " == null");
        }
        return t;
    }

    static <T> T validateConditionalNull(boolean z, String str, T t) {
        if (z) {
            return (T) validateNotNull(str, t);
        }
        return (T) validateNull(str, t);
    }

    static <T> T validateNull(String str, T t) {
        if (t != null) {
            throw new IllegalArgumentException(str + " != null");
        }
        return null;
    }
}
