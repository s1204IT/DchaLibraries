package sun.misc;

import java.util.Comparator;

public class ASCIICaseInsensitiveComparator implements Comparator<String> {
    static final boolean $assertionsDisabled = false;
    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new ASCIICaseInsensitiveComparator();

    @Override
    public int compare(String str, String str2) {
        char lower;
        char lower2;
        int length = str.length();
        int length2 = str2.length();
        int i = length < length2 ? length : length2;
        for (int i2 = 0; i2 < i; i2++) {
            char cCharAt = str.charAt(i2);
            char cCharAt2 = str2.charAt(i2);
            if (cCharAt != cCharAt2 && (lower = (char) toLower(cCharAt)) != (lower2 = (char) toLower(cCharAt2))) {
                return lower - lower2;
            }
        }
        return length - length2;
    }

    public static int lowerCaseHashCode(String str) {
        int length = str.length();
        int lower = 0;
        for (int i = 0; i < length; i++) {
            lower = toLower(str.charAt(i)) + (31 * lower);
        }
        return lower;
    }

    static boolean isLower(int i) {
        return ((122 - i) | (i + (-97))) >= 0;
    }

    static boolean isUpper(int i) {
        return ((90 - i) | (i + (-65))) >= 0;
    }

    static int toLower(int i) {
        return isUpper(i) ? i + 32 : i;
    }

    static int toUpper(int i) {
        return isLower(i) ? i - 32 : i;
    }
}
