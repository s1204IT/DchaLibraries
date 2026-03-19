package android.icu.lang;

import android.icu.text.UTF16;
import dalvik.bytecode.Opcodes;

@Deprecated
public class CharSequences {
    @Deprecated
    public static int matchAfter(CharSequence a, CharSequence b, int aIndex, int bIndex) {
        int i = aIndex;
        int j = bIndex;
        int alen = a.length();
        int blen = b.length();
        while (i < alen && j < blen) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (ca != cb) {
                break;
            }
            i++;
            j++;
        }
        int result = i - aIndex;
        if (result != 0 && !onCharacterBoundary(a, i) && !onCharacterBoundary(b, j)) {
            return result - 1;
        }
        return result;
    }

    @Deprecated
    public int codePointLength(CharSequence s) {
        return Character.codePointCount(s, 0, s.length());
    }

    @Deprecated
    public static final boolean equals(int codepoint, CharSequence other) {
        if (other == null) {
            return false;
        }
        switch (other.length()) {
            case 1:
                if (codepoint != other.charAt(0)) {
                    break;
                }
                break;
            case 2:
                if (codepoint > 65535 && codepoint == Character.codePointAt(other, 0)) {
                    break;
                }
                break;
        }
        return false;
    }

    @Deprecated
    public static final boolean equals(CharSequence other, int codepoint) {
        return equals(codepoint, other);
    }

    @Deprecated
    public static int compare(CharSequence string, int codePoint) {
        if (codePoint < 0 || codePoint > 1114111) {
            throw new IllegalArgumentException();
        }
        int stringLength = string.length();
        if (stringLength == 0) {
            return -1;
        }
        char firstChar = string.charAt(0);
        int offset = codePoint - 65536;
        if (offset < 0) {
            int result = firstChar - codePoint;
            if (result != 0) {
                return result;
            }
            return stringLength - 1;
        }
        char lead = (char) ((offset >>> 10) + 55296);
        int result2 = firstChar - lead;
        if (result2 != 0) {
            return result2;
        }
        if (stringLength > 1) {
            char trail = (char) ((offset & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE);
            int result3 = string.charAt(1) - trail;
            if (result3 != 0) {
                return result3;
            }
        }
        return stringLength - 2;
    }

    @Deprecated
    public static int compare(int codepoint, CharSequence a) {
        return -compare(a, codepoint);
    }

    @Deprecated
    public static int getSingleCodePoint(CharSequence s) {
        int length = s.length();
        if (length < 1 || length > 2) {
            return Integer.MAX_VALUE;
        }
        int result = Character.codePointAt(s, 0);
        if ((result < 65536) == (length == 1)) {
            return result;
        }
        return Integer.MAX_VALUE;
    }

    @Deprecated
    public static final <T> boolean equals(T a, T b) {
        if (a == null) {
            return b == null;
        }
        if (b != null) {
            return a.equals(b);
        }
        return false;
    }

    @Deprecated
    public static int compare(CharSequence a, CharSequence b) {
        int alength = a.length();
        int blength = b.length();
        int min = alength <= blength ? alength : blength;
        for (int i = 0; i < min; i++) {
            int diff = a.charAt(i) - b.charAt(i);
            if (diff != 0) {
                return diff;
            }
        }
        return alength - blength;
    }

    @Deprecated
    public static boolean equalsChars(CharSequence a, CharSequence b) {
        return a.length() == b.length() && compare(a, b) == 0;
    }

    @Deprecated
    public static boolean onCharacterBoundary(CharSequence s, int i) {
        if (i <= 0 || i >= s.length() || !Character.isHighSurrogate(s.charAt(i - 1))) {
            return true;
        }
        return !Character.isLowSurrogate(s.charAt(i));
    }

    @Deprecated
    public static int indexOf(CharSequence s, int codePoint) {
        int i = 0;
        while (i < s.length()) {
            int cp = Character.codePointAt(s, i);
            if (cp != codePoint) {
                i += Character.charCount(cp);
            } else {
                return i;
            }
        }
        return -1;
    }

    @Deprecated
    public static int[] codePoints(CharSequence s) {
        char last;
        int[] result = new int[s.length()];
        int j = 0;
        for (int i = 0; i < s.length(); i++) {
            char cp = s.charAt(i);
            if (cp >= 56320 && cp <= 57343 && i != 0 && (last = (char) result[j - 1]) >= 55296 && last <= 56319) {
                result[j - 1] = Character.toCodePoint(last, cp);
            } else {
                result[j] = cp;
                j++;
            }
        }
        if (j == result.length) {
            return result;
        }
        int[] shortResult = new int[j];
        System.arraycopy(result, 0, shortResult, 0, j);
        return shortResult;
    }

    private CharSequences() {
    }
}
