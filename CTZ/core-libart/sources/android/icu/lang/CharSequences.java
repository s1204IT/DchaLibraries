package android.icu.lang;

import android.icu.text.UTF16;
import dalvik.bytecode.Opcodes;

@Deprecated
public class CharSequences {
    @Deprecated
    public static int matchAfter(CharSequence charSequence, CharSequence charSequence2, int i, int i2) {
        int length = charSequence.length();
        int length2 = charSequence2.length();
        int i3 = i2;
        int i4 = i;
        while (i4 < length && i3 < length2 && charSequence.charAt(i4) == charSequence2.charAt(i3)) {
            i4++;
            i3++;
        }
        int i5 = i4 - i;
        if (i5 != 0 && !onCharacterBoundary(charSequence, i4) && !onCharacterBoundary(charSequence2, i3)) {
            return i5 - 1;
        }
        return i5;
    }

    @Deprecated
    public int codePointLength(CharSequence charSequence) {
        return Character.codePointCount(charSequence, 0, charSequence.length());
    }

    @Deprecated
    public static final boolean equals(int i, CharSequence charSequence) {
        if (charSequence == null) {
            return false;
        }
        switch (charSequence.length()) {
            case 1:
                if (i == charSequence.charAt(0)) {
                }
                break;
            case 2:
                if (i > 65535 && i == Character.codePointAt(charSequence, 0)) {
                    break;
                }
                break;
        }
        return false;
    }

    @Deprecated
    public static final boolean equals(CharSequence charSequence, int i) {
        return equals(i, charSequence);
    }

    @Deprecated
    public static int compare(CharSequence charSequence, int i) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException();
        }
        int length = charSequence.length();
        if (length == 0) {
            return -1;
        }
        char cCharAt = charSequence.charAt(0);
        int i2 = i - 65536;
        if (i2 < 0) {
            int i3 = cCharAt - i;
            if (i3 != 0) {
                return i3;
            }
            return length - 1;
        }
        int i4 = cCharAt - ((char) ((i2 >>> 10) + 55296));
        if (i4 != 0) {
            return i4;
        }
        if (length > 1) {
            int iCharAt = charSequence.charAt(1) - ((char) ((i2 & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE));
            if (iCharAt != 0) {
                return iCharAt;
            }
        }
        return length - 2;
    }

    @Deprecated
    public static int compare(int i, CharSequence charSequence) {
        int iCompare = compare(charSequence, i);
        if (iCompare > 0) {
            return -1;
        }
        return iCompare < 0 ? 1 : 0;
    }

    @Deprecated
    public static int getSingleCodePoint(CharSequence charSequence) {
        int length = charSequence.length();
        if (length < 1 || length > 2) {
            return Integer.MAX_VALUE;
        }
        int iCodePointAt = Character.codePointAt(charSequence, 0);
        if ((iCodePointAt < 65536) == (length == 1)) {
            return iCodePointAt;
        }
        return Integer.MAX_VALUE;
    }

    @Deprecated
    public static final <T> boolean equals(T t, T t2) {
        if (t == null) {
            return t2 == null;
        }
        if (t2 == null) {
            return false;
        }
        return t.equals(t2);
    }

    @Deprecated
    public static int compare(CharSequence charSequence, CharSequence charSequence2) {
        int length = charSequence.length();
        int length2 = charSequence2.length();
        int i = length <= length2 ? length : length2;
        for (int i2 = 0; i2 < i; i2++) {
            int iCharAt = charSequence.charAt(i2) - charSequence2.charAt(i2);
            if (iCharAt != 0) {
                return iCharAt;
            }
        }
        return length - length2;
    }

    @Deprecated
    public static boolean equalsChars(CharSequence charSequence, CharSequence charSequence2) {
        return charSequence.length() == charSequence2.length() && compare(charSequence, charSequence2) == 0;
    }

    @Deprecated
    public static boolean onCharacterBoundary(CharSequence charSequence, int i) {
        return i <= 0 || i >= charSequence.length() || !Character.isHighSurrogate(charSequence.charAt(i + (-1))) || !Character.isLowSurrogate(charSequence.charAt(i));
    }

    @Deprecated
    public static int indexOf(CharSequence charSequence, int i) {
        int iCharCount = 0;
        while (iCharCount < charSequence.length()) {
            int iCodePointAt = Character.codePointAt(charSequence, iCharCount);
            if (iCodePointAt != i) {
                iCharCount += Character.charCount(iCodePointAt);
            } else {
                return iCharCount;
            }
        }
        return -1;
    }

    @Deprecated
    public static int[] codePoints(CharSequence charSequence) {
        int[] iArr = new int[charSequence.length()];
        int i = 0;
        for (int i2 = 0; i2 < charSequence.length(); i2++) {
            char cCharAt = charSequence.charAt(i2);
            if (cCharAt >= 56320 && cCharAt <= 57343 && i2 != 0) {
                int i3 = i - 1;
                char c = (char) iArr[i3];
                if (c >= 55296 && c <= 56319) {
                    iArr[i3] = Character.toCodePoint(c, cCharAt);
                }
            } else {
                iArr[i] = cCharAt;
                i++;
            }
        }
        if (i == iArr.length) {
            return iArr;
        }
        int[] iArr2 = new int[i];
        System.arraycopy(iArr, 0, iArr2, 0, i);
        return iArr2;
    }

    private CharSequences() {
    }
}
