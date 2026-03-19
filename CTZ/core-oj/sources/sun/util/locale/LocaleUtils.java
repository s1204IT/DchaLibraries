package sun.util.locale;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LocaleUtils {
    private LocaleUtils() {
    }

    public static boolean caseIgnoreMatch(String str, String str2) {
        if (str == str2) {
            return true;
        }
        int length = str.length();
        if (length != str2.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            char cCharAt2 = str2.charAt(i);
            if (cCharAt != cCharAt2 && toLower(cCharAt) != toLower(cCharAt2)) {
                return false;
            }
        }
        return true;
    }

    static int caseIgnoreCompare(String str, String str2) {
        if (str == str2) {
            return 0;
        }
        return toLowerString(str).compareTo(toLowerString(str2));
    }

    static char toUpper(char c) {
        return isLower(c) ? (char) (c - ' ') : c;
    }

    static char toLower(char c) {
        return isUpper(c) ? (char) (c + ' ') : c;
    }

    public static String toLowerString(String str) {
        int length = str.length();
        int i = 0;
        while (i < length && !isUpper(str.charAt(i))) {
            i++;
        }
        if (i == length) {
            return str;
        }
        char[] cArr = new char[length];
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (i2 >= i) {
                cCharAt = toLower(cCharAt);
            }
            cArr[i2] = cCharAt;
        }
        return new String(cArr);
    }

    static String toUpperString(String str) {
        int length = str.length();
        int i = 0;
        while (i < length && !isLower(str.charAt(i))) {
            i++;
        }
        if (i == length) {
            return str;
        }
        char[] cArr = new char[length];
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (i2 >= i) {
                cCharAt = toUpper(cCharAt);
            }
            cArr[i2] = cCharAt;
        }
        return new String(cArr);
    }

    static String toTitleString(String str) {
        int i;
        int length = str.length();
        if (length == 0) {
            return str;
        }
        if (!isLower(str.charAt(0))) {
            i = 1;
            while (i < length && !isUpper(str.charAt(i))) {
                i++;
            }
        } else {
            i = 0;
        }
        if (i == length) {
            return str;
        }
        char[] cArr = new char[length];
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (i2 == 0 && i == 0) {
                cArr[i2] = toUpper(cCharAt);
            } else if (i2 < i) {
                cArr[i2] = cCharAt;
            } else {
                cArr[i2] = toLower(cCharAt);
            }
        }
        return new String(cArr);
    }

    private static boolean isUpper(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean isLower(char c) {
        return c >= 'a' && c <= 'z';
    }

    static boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    static boolean isAlphaString(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!isAlpha(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean isNumeric(char c) {
        return c >= '0' && c <= '9';
    }

    static boolean isNumericString(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!isNumeric(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isNumeric(c);
    }

    public static boolean isAlphaNumericString(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!isAlphaNumeric(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    static boolean isEmpty(Set<?> set) {
        return set == null || set.isEmpty();
    }

    static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
