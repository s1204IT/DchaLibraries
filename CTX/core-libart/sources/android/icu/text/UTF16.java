package android.icu.text;

import android.icu.impl.Utility;
import java.util.Comparator;

public final class UTF16 {
    public static final int CODEPOINT_MAX_VALUE = 1114111;
    public static final int CODEPOINT_MIN_VALUE = 0;
    private static final int LEAD_SURROGATE_BITMASK = -1024;
    private static final int LEAD_SURROGATE_BITS = 55296;
    public static final int LEAD_SURROGATE_BOUNDARY = 2;
    public static final int LEAD_SURROGATE_MAX_VALUE = 56319;
    public static final int LEAD_SURROGATE_MIN_VALUE = 55296;
    private static final int LEAD_SURROGATE_OFFSET_ = 55232;
    private static final int LEAD_SURROGATE_SHIFT_ = 10;
    public static final int SINGLE_CHAR_BOUNDARY = 1;
    public static final int SUPPLEMENTARY_MIN_VALUE = 65536;
    private static final int SURROGATE_BITMASK = -2048;
    private static final int SURROGATE_BITS = 55296;
    public static final int SURROGATE_MAX_VALUE = 57343;
    public static final int SURROGATE_MIN_VALUE = 55296;
    private static final int TRAIL_SURROGATE_BITMASK = -1024;
    private static final int TRAIL_SURROGATE_BITS = 56320;
    public static final int TRAIL_SURROGATE_BOUNDARY = 5;
    private static final int TRAIL_SURROGATE_MASK_ = 1023;
    public static final int TRAIL_SURROGATE_MAX_VALUE = 57343;
    public static final int TRAIL_SURROGATE_MIN_VALUE = 56320;

    private UTF16() {
    }

    public static int charAt(String str, int i) {
        char cCharAt = str.charAt(i);
        if (cCharAt < 55296) {
            return cCharAt;
        }
        return _charAt(str, i, cCharAt);
    }

    private static int _charAt(String str, int i, char c) {
        char cCharAt;
        char cCharAt2;
        if (c > 57343) {
            return c;
        }
        if (c <= 56319) {
            int i2 = i + 1;
            if (str.length() != i2 && (cCharAt2 = str.charAt(i2)) >= 56320 && cCharAt2 <= 57343) {
                return Character.toCodePoint(c, cCharAt2);
            }
        } else {
            int i3 = i - 1;
            if (i3 >= 0 && (cCharAt = str.charAt(i3)) >= 55296 && cCharAt <= 56319) {
                return Character.toCodePoint(cCharAt, c);
            }
        }
        return c;
    }

    public static int charAt(CharSequence charSequence, int i) {
        char cCharAt = charSequence.charAt(i);
        if (cCharAt < 55296) {
            return cCharAt;
        }
        return _charAt(charSequence, i, cCharAt);
    }

    private static int _charAt(CharSequence charSequence, int i, char c) {
        char cCharAt;
        char cCharAt2;
        if (c > 57343) {
            return c;
        }
        if (c <= 56319) {
            int i2 = i + 1;
            if (charSequence.length() != i2 && (cCharAt2 = charSequence.charAt(i2)) >= 56320 && cCharAt2 <= 57343) {
                return Character.toCodePoint(c, cCharAt2);
            }
        } else {
            int i3 = i - 1;
            if (i3 >= 0 && (cCharAt = charSequence.charAt(i3)) >= 55296 && cCharAt <= 56319) {
                return Character.toCodePoint(cCharAt, c);
            }
        }
        return c;
    }

    public static int charAt(StringBuffer stringBuffer, int i) {
        if (i < 0 || i >= stringBuffer.length()) {
            throw new StringIndexOutOfBoundsException(i);
        }
        char cCharAt = stringBuffer.charAt(i);
        if (!isSurrogate(cCharAt)) {
            return cCharAt;
        }
        if (cCharAt <= 56319) {
            int i2 = i + 1;
            if (stringBuffer.length() != i2) {
                char cCharAt2 = stringBuffer.charAt(i2);
                if (isTrailSurrogate(cCharAt2)) {
                    return Character.toCodePoint(cCharAt, cCharAt2);
                }
            }
        } else {
            int i3 = i - 1;
            if (i3 >= 0) {
                char cCharAt3 = stringBuffer.charAt(i3);
                if (isLeadSurrogate(cCharAt3)) {
                    return Character.toCodePoint(cCharAt3, cCharAt);
                }
            }
        }
        return cCharAt;
    }

    public static int charAt(char[] cArr, int i, int i2, int i3) {
        int i4 = i3 + i;
        if (i4 < i || i4 >= i2) {
            throw new ArrayIndexOutOfBoundsException(i4);
        }
        char c = cArr[i4];
        if (!isSurrogate(c)) {
            return c;
        }
        if (c <= 56319) {
            int i5 = i4 + 1;
            if (i5 >= i2) {
                return c;
            }
            char c2 = cArr[i5];
            if (isTrailSurrogate(c2)) {
                return Character.toCodePoint(c, c2);
            }
        } else {
            if (i4 == i) {
                return c;
            }
            char c3 = cArr[i4 - 1];
            if (isLeadSurrogate(c3)) {
                return Character.toCodePoint(c3, c);
            }
        }
        return c;
    }

    public static int charAt(Replaceable replaceable, int i) {
        if (i < 0 || i >= replaceable.length()) {
            throw new StringIndexOutOfBoundsException(i);
        }
        char cCharAt = replaceable.charAt(i);
        if (!isSurrogate(cCharAt)) {
            return cCharAt;
        }
        if (cCharAt <= 56319) {
            int i2 = i + 1;
            if (replaceable.length() != i2) {
                char cCharAt2 = replaceable.charAt(i2);
                if (isTrailSurrogate(cCharAt2)) {
                    return Character.toCodePoint(cCharAt, cCharAt2);
                }
            }
        } else {
            int i3 = i - 1;
            if (i3 >= 0) {
                char cCharAt3 = replaceable.charAt(i3);
                if (isLeadSurrogate(cCharAt3)) {
                    return Character.toCodePoint(cCharAt3, cCharAt);
                }
            }
        }
        return cCharAt;
    }

    public static int getCharCount(int i) {
        if (i < 65536) {
            return 1;
        }
        return 2;
    }

    public static int bounds(String str, int i) {
        char cCharAt = str.charAt(i);
        if (isSurrogate(cCharAt)) {
            if (isLeadSurrogate(cCharAt)) {
                int i2 = i + 1;
                if (i2 < str.length() && isTrailSurrogate(str.charAt(i2))) {
                    return 2;
                }
            } else {
                int i3 = i - 1;
                if (i3 >= 0 && isLeadSurrogate(str.charAt(i3))) {
                    return 5;
                }
            }
        }
        return 1;
    }

    public static int bounds(StringBuffer stringBuffer, int i) {
        char cCharAt = stringBuffer.charAt(i);
        if (isSurrogate(cCharAt)) {
            if (isLeadSurrogate(cCharAt)) {
                int i2 = i + 1;
                if (i2 < stringBuffer.length() && isTrailSurrogate(stringBuffer.charAt(i2))) {
                    return 2;
                }
            } else {
                int i3 = i - 1;
                if (i3 >= 0 && isLeadSurrogate(stringBuffer.charAt(i3))) {
                    return 5;
                }
            }
        }
        return 1;
    }

    public static int bounds(char[] cArr, int i, int i2, int i3) {
        int i4 = i3 + i;
        if (i4 < i || i4 >= i2) {
            throw new ArrayIndexOutOfBoundsException(i4);
        }
        char c = cArr[i4];
        if (isSurrogate(c)) {
            if (isLeadSurrogate(c)) {
                int i5 = i4 + 1;
                if (i5 < i2 && isTrailSurrogate(cArr[i5])) {
                    return 2;
                }
            } else {
                int i6 = i4 - 1;
                if (i6 >= i && isLeadSurrogate(cArr[i6])) {
                    return 5;
                }
            }
        }
        return 1;
    }

    public static boolean isSurrogate(char c) {
        return (c & SURROGATE_BITMASK) == 55296;
    }

    public static boolean isTrailSurrogate(char c) {
        return (c & (-1024)) == 56320;
    }

    public static boolean isLeadSurrogate(char c) {
        return (c & (-1024)) == 55296;
    }

    public static char getLeadSurrogate(int i) {
        if (i >= 65536) {
            return (char) (LEAD_SURROGATE_OFFSET_ + (i >> 10));
        }
        return (char) 0;
    }

    public static char getTrailSurrogate(int i) {
        if (i >= 65536) {
            return (char) (56320 + (i & 1023));
        }
        return (char) i;
    }

    public static String valueOf(int i) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Illegal codepoint");
        }
        return toString(i);
    }

    public static String valueOf(String str, int i) {
        int iBounds = bounds(str, i);
        if (iBounds == 2) {
            return str.substring(i, i + 2);
        }
        if (iBounds == 5) {
            return str.substring(i - 1, i + 1);
        }
        return str.substring(i, i + 1);
    }

    public static String valueOf(StringBuffer stringBuffer, int i) {
        int iBounds = bounds(stringBuffer, i);
        if (iBounds == 2) {
            return stringBuffer.substring(i, i + 2);
        }
        if (iBounds == 5) {
            return stringBuffer.substring(i - 1, i + 1);
        }
        return stringBuffer.substring(i, i + 1);
    }

    public static String valueOf(char[] cArr, int i, int i2, int i3) {
        int iBounds = bounds(cArr, i, i2, i3);
        if (iBounds == 2) {
            return new String(cArr, i + i3, 2);
        }
        if (iBounds == 5) {
            return new String(cArr, (i + i3) - 1, 2);
        }
        return new String(cArr, i + i3, 1);
    }

    public static int findOffsetFromCodePoint(String str, int i) {
        int i2;
        int length = str.length();
        if (i < 0 || i > length) {
            throw new StringIndexOutOfBoundsException(i);
        }
        int i3 = 0;
        int i4 = i;
        while (i3 < length && i4 > 0) {
            if (isLeadSurrogate(str.charAt(i3)) && (i2 = i3 + 1) < length && isTrailSurrogate(str.charAt(i2))) {
                i3 = i2;
            }
            i4--;
            i3++;
        }
        if (i4 != 0) {
            throw new StringIndexOutOfBoundsException(i);
        }
        return i3;
    }

    public static int findOffsetFromCodePoint(StringBuffer stringBuffer, int i) {
        int i2;
        int length = stringBuffer.length();
        if (i < 0 || i > length) {
            throw new StringIndexOutOfBoundsException(i);
        }
        int i3 = 0;
        int i4 = i;
        while (i3 < length && i4 > 0) {
            if (isLeadSurrogate(stringBuffer.charAt(i3)) && (i2 = i3 + 1) < length && isTrailSurrogate(stringBuffer.charAt(i2))) {
                i3 = i2;
            }
            i4--;
            i3++;
        }
        if (i4 != 0) {
            throw new StringIndexOutOfBoundsException(i);
        }
        return i3;
    }

    public static int findOffsetFromCodePoint(char[] cArr, int i, int i2, int i3) {
        int i4;
        if (i3 > i2 - i) {
            throw new ArrayIndexOutOfBoundsException(i3);
        }
        int i5 = i;
        int i6 = i3;
        while (i5 < i2 && i6 > 0) {
            if (isLeadSurrogate(cArr[i5]) && (i4 = i5 + 1) < i2 && isTrailSurrogate(cArr[i4])) {
                i5 = i4;
            }
            i6--;
            i5++;
        }
        if (i6 != 0) {
            throw new ArrayIndexOutOfBoundsException(i3);
        }
        return i5 - i;
    }

    public static int findCodePointOffset(String str, int i) {
        if (i < 0 || i > str.length()) {
            throw new StringIndexOutOfBoundsException(i);
        }
        boolean zIsLeadSurrogate = false;
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            char cCharAt = str.charAt(i3);
            if (zIsLeadSurrogate && isTrailSurrogate(cCharAt)) {
                zIsLeadSurrogate = false;
            } else {
                zIsLeadSurrogate = isLeadSurrogate(cCharAt);
                i2++;
            }
        }
        if (i != str.length() && zIsLeadSurrogate && isTrailSurrogate(str.charAt(i))) {
            return i2 - 1;
        }
        return i2;
    }

    public static int findCodePointOffset(StringBuffer stringBuffer, int i) {
        if (i < 0 || i > stringBuffer.length()) {
            throw new StringIndexOutOfBoundsException(i);
        }
        boolean zIsLeadSurrogate = false;
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            char cCharAt = stringBuffer.charAt(i3);
            if (zIsLeadSurrogate && isTrailSurrogate(cCharAt)) {
                zIsLeadSurrogate = false;
            } else {
                zIsLeadSurrogate = isLeadSurrogate(cCharAt);
                i2++;
            }
        }
        if (i != stringBuffer.length() && zIsLeadSurrogate && isTrailSurrogate(stringBuffer.charAt(i))) {
            return i2 - 1;
        }
        return i2;
    }

    public static int findCodePointOffset(char[] cArr, int i, int i2, int i3) {
        int i4 = i3 + i;
        if (i4 > i2) {
            throw new StringIndexOutOfBoundsException(i4);
        }
        boolean zIsLeadSurrogate = false;
        int i5 = 0;
        while (i < i4) {
            char c = cArr[i];
            if (zIsLeadSurrogate && isTrailSurrogate(c)) {
                zIsLeadSurrogate = false;
            } else {
                zIsLeadSurrogate = isLeadSurrogate(c);
                i5++;
            }
            i++;
        }
        if (i4 != i2 && zIsLeadSurrogate && isTrailSurrogate(cArr[i4])) {
            return i5 - 1;
        }
        return i5;
    }

    public static StringBuffer append(StringBuffer stringBuffer, int i) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Illegal codepoint: " + Integer.toHexString(i));
        }
        if (i >= 65536) {
            stringBuffer.append(getLeadSurrogate(i));
            stringBuffer.append(getTrailSurrogate(i));
        } else {
            stringBuffer.append((char) i);
        }
        return stringBuffer;
    }

    public static StringBuffer appendCodePoint(StringBuffer stringBuffer, int i) {
        return append(stringBuffer, i);
    }

    public static int append(char[] cArr, int i, int i2) {
        if (i2 < 0 || i2 > 1114111) {
            throw new IllegalArgumentException("Illegal codepoint");
        }
        if (i2 >= 65536) {
            int i3 = i + 1;
            cArr[i] = getLeadSurrogate(i2);
            int i4 = i3 + 1;
            cArr[i3] = getTrailSurrogate(i2);
            return i4;
        }
        int i5 = i + 1;
        cArr[i] = (char) i2;
        return i5;
    }

    public static int countCodePoint(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        return findCodePointOffset(str, str.length());
    }

    public static int countCodePoint(StringBuffer stringBuffer) {
        if (stringBuffer == null || stringBuffer.length() == 0) {
            return 0;
        }
        return findCodePointOffset(stringBuffer, stringBuffer.length());
    }

    public static int countCodePoint(char[] cArr, int i, int i2) {
        if (cArr == null || cArr.length == 0) {
            return 0;
        }
        return findCodePointOffset(cArr, i, i2, i2 - i);
    }

    public static void setCharAt(StringBuffer stringBuffer, int i, int i2) {
        int i3;
        char cCharAt = stringBuffer.charAt(i);
        int i4 = 2;
        if (isSurrogate(cCharAt)) {
            if (!isLeadSurrogate(cCharAt) || stringBuffer.length() <= (i3 = i + 1) || !isTrailSurrogate(stringBuffer.charAt(i3))) {
                if (isTrailSurrogate(cCharAt) && i > 0 && isLeadSurrogate(stringBuffer.charAt(i - 1))) {
                    i--;
                }
            }
        } else {
            i4 = 1;
        }
        stringBuffer.replace(i, i4 + i, valueOf(i2));
    }

    public static int setCharAt(char[] cArr, int i, int i2, int i3) {
        int i4;
        int i5;
        if (i2 >= i) {
            throw new ArrayIndexOutOfBoundsException(i2);
        }
        char c = cArr[i2];
        if (isSurrogate(c)) {
            if (!isLeadSurrogate(c) || cArr.length <= (i5 = i2 + 1) || !isTrailSurrogate(cArr[i5])) {
                if (isTrailSurrogate(c) && i2 > 0 && isLeadSurrogate(cArr[i2 - 1])) {
                    i2--;
                }
                i4 = 1;
            }
            i4 = 2;
        } else {
            i4 = 1;
        }
        String strValueOf = valueOf(i3);
        int length = strValueOf.length();
        cArr[i2] = strValueOf.charAt(0);
        if (i4 == length) {
            if (i4 == 2) {
                cArr[i2 + 1] = strValueOf.charAt(1);
                return i;
            }
            return i;
        }
        int i6 = i2 + i4;
        System.arraycopy(cArr, i6, cArr, i2 + length, i - i6);
        if (i4 < length) {
            cArr[i2 + 1] = strValueOf.charAt(1);
            int i7 = i + 1;
            if (i7 < cArr.length) {
                cArr[i7] = 0;
                return i7;
            }
            return i7;
        }
        int i8 = i - 1;
        cArr[i8] = 0;
        return i8;
    }

    public static int moveCodePointOffset(String str, int i, int i2) {
        int i3;
        int i4;
        int length = str.length();
        if (i < 0 || i > length) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (i2 > 0) {
            if (i2 + i > length) {
                throw new StringIndexOutOfBoundsException(i);
            }
            i3 = i2;
            while (i < length && i3 > 0) {
                if (isLeadSurrogate(str.charAt(i)) && (i4 = i + 1) < length && isTrailSurrogate(str.charAt(i4))) {
                    i = i4;
                }
                i3--;
                i++;
            }
        } else {
            if (i + i2 < 0) {
                throw new StringIndexOutOfBoundsException(i);
            }
            i3 = -i2;
            while (i3 > 0) {
                i--;
                if (i < 0) {
                    break;
                }
                if (isTrailSurrogate(str.charAt(i)) && i > 0 && isLeadSurrogate(str.charAt(i - 1))) {
                    i--;
                }
                i3--;
            }
        }
        if (i3 != 0) {
            throw new StringIndexOutOfBoundsException(i2);
        }
        return i;
    }

    public static int moveCodePointOffset(StringBuffer stringBuffer, int i, int i2) {
        int i3;
        int i4;
        int length = stringBuffer.length();
        if (i < 0 || i > length) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (i2 > 0) {
            if (i2 + i > length) {
                throw new StringIndexOutOfBoundsException(i);
            }
            i3 = i2;
            while (i < length && i3 > 0) {
                if (isLeadSurrogate(stringBuffer.charAt(i)) && (i4 = i + 1) < length && isTrailSurrogate(stringBuffer.charAt(i4))) {
                    i = i4;
                }
                i3--;
                i++;
            }
        } else {
            if (i + i2 < 0) {
                throw new StringIndexOutOfBoundsException(i);
            }
            i3 = -i2;
            while (i3 > 0) {
                i--;
                if (i < 0) {
                    break;
                }
                if (isTrailSurrogate(stringBuffer.charAt(i)) && i > 0 && isLeadSurrogate(stringBuffer.charAt(i - 1))) {
                    i--;
                }
                i3--;
            }
        }
        if (i3 != 0) {
            throw new StringIndexOutOfBoundsException(i2);
        }
        return i;
    }

    public static int moveCodePointOffset(char[] cArr, int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        int length = cArr.length;
        int i7 = i3 + i;
        if (i < 0 || i2 < i) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (i2 > length) {
            throw new StringIndexOutOfBoundsException(i2);
        }
        if (i3 < 0 || i7 > i2) {
            throw new StringIndexOutOfBoundsException(i3);
        }
        if (i4 > 0) {
            if (i4 + i7 > length) {
                throw new StringIndexOutOfBoundsException(i7);
            }
            i5 = i4;
            while (i7 < i2 && i5 > 0) {
                if (!isLeadSurrogate(cArr[i7]) || (i6 = i7 + 1) >= i2 || !isTrailSurrogate(cArr[i6])) {
                    i6 = i7;
                }
                i5--;
                i7 = i6 + 1;
            }
        } else {
            if (i7 + i4 < i) {
                throw new StringIndexOutOfBoundsException(i7);
            }
            i5 = -i4;
            while (i5 > 0) {
                i7--;
                if (i7 < i) {
                    break;
                }
                if (isTrailSurrogate(cArr[i7]) && i7 > i && isLeadSurrogate(cArr[i7 - 1])) {
                    i7--;
                }
                i5--;
            }
        }
        if (i5 != 0) {
            throw new StringIndexOutOfBoundsException(i4);
        }
        return i7 - i;
    }

    public static StringBuffer insert(StringBuffer stringBuffer, int i, int i2) {
        String strValueOf = valueOf(i2);
        if (i != stringBuffer.length() && bounds(stringBuffer, i) == 5) {
            i++;
        }
        stringBuffer.insert(i, strValueOf);
        return stringBuffer;
    }

    public static int insert(char[] cArr, int i, int i2, int i3) {
        String strValueOf = valueOf(i3);
        if (i2 != i && bounds(cArr, 0, i, i2) == 5) {
            i2++;
        }
        int length = strValueOf.length();
        int i4 = i + length;
        if (i4 > cArr.length) {
            throw new ArrayIndexOutOfBoundsException(i2 + length);
        }
        System.arraycopy(cArr, i2, cArr, i2 + length, i - i2);
        cArr[i2] = strValueOf.charAt(0);
        if (length == 2) {
            cArr[i2 + 1] = strValueOf.charAt(1);
        }
        return i4;
    }

    public static StringBuffer delete(StringBuffer stringBuffer, int i) {
        int iBounds = bounds(stringBuffer, i);
        int i2 = 2;
        if (iBounds != 2) {
            if (iBounds == 5) {
                i--;
            } else {
                i2 = 1;
            }
        }
        stringBuffer.delete(i, i2 + i);
        return stringBuffer;
    }

    public static int delete(char[] cArr, int i, int i2) {
        int iBounds = bounds(cArr, 0, i, i2);
        int i3 = 2;
        if (iBounds != 2) {
            if (iBounds == 5) {
                i2--;
            } else {
                i3 = 1;
            }
        }
        int i4 = i2 + i3;
        System.arraycopy(cArr, i4, cArr, i2, i - i4);
        int i5 = i - i3;
        cArr[i5] = 0;
        return i5;
    }

    public static int indexOf(String str, int i) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Argument char32 is not a valid codepoint");
        }
        if (i < 55296 || (i > 57343 && i < 65536)) {
            return str.indexOf((char) i);
        }
        if (i < 65536) {
            char c = (char) i;
            int iIndexOf = str.indexOf(c);
            if (iIndexOf >= 0) {
                if (isLeadSurrogate(c) && iIndexOf < str.length() - 1) {
                    int i2 = iIndexOf + 1;
                    if (isTrailSurrogate(str.charAt(i2))) {
                        return indexOf(str, i, i2);
                    }
                }
                if (iIndexOf > 0 && isLeadSurrogate(str.charAt(iIndexOf - 1))) {
                    return indexOf(str, i, iIndexOf + 1);
                }
            }
            return iIndexOf;
        }
        return str.indexOf(toString(i));
    }

    public static int indexOf(String str, String str2) {
        int length = str2.length();
        if (!isTrailSurrogate(str2.charAt(0)) && !isLeadSurrogate(str2.charAt(length - 1))) {
            return str.indexOf(str2);
        }
        int iIndexOf = str.indexOf(str2);
        int i = iIndexOf + length;
        if (iIndexOf >= 0) {
            if (isLeadSurrogate(str2.charAt(length - 1)) && iIndexOf < str.length() - 1) {
                int i2 = i + 1;
                if (isTrailSurrogate(str.charAt(i2))) {
                    return indexOf(str, str2, i2);
                }
            }
            if (isTrailSurrogate(str2.charAt(0)) && iIndexOf > 0 && isLeadSurrogate(str.charAt(iIndexOf - 1))) {
                return indexOf(str, str2, i + 1);
            }
        }
        return iIndexOf;
    }

    public static int indexOf(String str, int i, int i2) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Argument char32 is not a valid codepoint");
        }
        if (i < 55296 || (i > 57343 && i < 65536)) {
            return str.indexOf((char) i, i2);
        }
        if (i < 65536) {
            char c = (char) i;
            int iIndexOf = str.indexOf(c, i2);
            if (iIndexOf >= 0) {
                if (isLeadSurrogate(c) && iIndexOf < str.length() - 1) {
                    int i3 = iIndexOf + 1;
                    if (isTrailSurrogate(str.charAt(i3))) {
                        return indexOf(str, i, i3);
                    }
                }
                if (iIndexOf > 0 && isLeadSurrogate(str.charAt(iIndexOf - 1))) {
                    return indexOf(str, i, iIndexOf + 1);
                }
            }
            return iIndexOf;
        }
        return str.indexOf(toString(i), i2);
    }

    public static int indexOf(String str, String str2, int i) {
        int length = str2.length();
        if (!isTrailSurrogate(str2.charAt(0)) && !isLeadSurrogate(str2.charAt(length - 1))) {
            return str.indexOf(str2, i);
        }
        int iIndexOf = str.indexOf(str2, i);
        int i2 = iIndexOf + length;
        if (iIndexOf >= 0) {
            if (isLeadSurrogate(str2.charAt(length - 1)) && iIndexOf < str.length() - 1 && isTrailSurrogate(str.charAt(i2))) {
                return indexOf(str, str2, i2 + 1);
            }
            if (isTrailSurrogate(str2.charAt(0)) && iIndexOf > 0 && isLeadSurrogate(str.charAt(iIndexOf - 1))) {
                return indexOf(str, str2, i2 + 1);
            }
        }
        return iIndexOf;
    }

    public static int lastIndexOf(String str, int i) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Argument char32 is not a valid codepoint");
        }
        if (i < 55296 || (i > 57343 && i < 65536)) {
            return str.lastIndexOf((char) i);
        }
        if (i < 65536) {
            char c = (char) i;
            int iLastIndexOf = str.lastIndexOf(c);
            if (iLastIndexOf >= 0) {
                if (isLeadSurrogate(c) && iLastIndexOf < str.length() - 1 && isTrailSurrogate(str.charAt(iLastIndexOf + 1))) {
                    return lastIndexOf(str, i, iLastIndexOf - 1);
                }
                if (iLastIndexOf > 0) {
                    int i2 = iLastIndexOf - 1;
                    if (isLeadSurrogate(str.charAt(i2))) {
                        return lastIndexOf(str, i, i2);
                    }
                }
            }
            return iLastIndexOf;
        }
        return str.lastIndexOf(toString(i));
    }

    public static int lastIndexOf(String str, String str2) {
        int length = str2.length();
        if (!isTrailSurrogate(str2.charAt(0)) && !isLeadSurrogate(str2.charAt(length - 1))) {
            return str.lastIndexOf(str2);
        }
        int iLastIndexOf = str.lastIndexOf(str2);
        if (iLastIndexOf >= 0) {
            if (isLeadSurrogate(str2.charAt(length - 1)) && iLastIndexOf < str.length() - 1 && isTrailSurrogate(str.charAt(length + iLastIndexOf + 1))) {
                return lastIndexOf(str, str2, iLastIndexOf - 1);
            }
            if (isTrailSurrogate(str2.charAt(0)) && iLastIndexOf > 0) {
                int i = iLastIndexOf - 1;
                if (isLeadSurrogate(str.charAt(i))) {
                    return lastIndexOf(str, str2, i);
                }
            }
        }
        return iLastIndexOf;
    }

    public static int lastIndexOf(String str, int i, int i2) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Argument char32 is not a valid codepoint");
        }
        if (i < 55296 || (i > 57343 && i < 65536)) {
            return str.lastIndexOf((char) i, i2);
        }
        if (i < 65536) {
            char c = (char) i;
            int iLastIndexOf = str.lastIndexOf(c, i2);
            if (iLastIndexOf >= 0) {
                if (isLeadSurrogate(c) && iLastIndexOf < str.length() - 1 && isTrailSurrogate(str.charAt(iLastIndexOf + 1))) {
                    return lastIndexOf(str, i, iLastIndexOf - 1);
                }
                if (iLastIndexOf > 0) {
                    int i3 = iLastIndexOf - 1;
                    if (isLeadSurrogate(str.charAt(i3))) {
                        return lastIndexOf(str, i, i3);
                    }
                }
            }
            return iLastIndexOf;
        }
        return str.lastIndexOf(toString(i), i2);
    }

    public static int lastIndexOf(String str, String str2, int i) {
        int length = str2.length();
        if (!isTrailSurrogate(str2.charAt(0)) && !isLeadSurrogate(str2.charAt(length - 1))) {
            return str.lastIndexOf(str2, i);
        }
        int iLastIndexOf = str.lastIndexOf(str2, i);
        if (iLastIndexOf >= 0) {
            if (isLeadSurrogate(str2.charAt(length - 1)) && iLastIndexOf < str.length() - 1 && isTrailSurrogate(str.charAt(length + iLastIndexOf))) {
                return lastIndexOf(str, str2, iLastIndexOf - 1);
            }
            if (isTrailSurrogate(str2.charAt(0)) && iLastIndexOf > 0) {
                int i2 = iLastIndexOf - 1;
                if (isLeadSurrogate(str.charAt(i2))) {
                    return lastIndexOf(str, str2, i2);
                }
            }
        }
        return iLastIndexOf;
    }

    public static String replace(String str, int i, int i2) {
        if (i <= 0 || i > 1114111) {
            throw new IllegalArgumentException("Argument oldChar32 is not a valid codepoint");
        }
        if (i2 <= 0 || i2 > 1114111) {
            throw new IllegalArgumentException("Argument newChar32 is not a valid codepoint");
        }
        int iIndexOf = indexOf(str, i);
        if (iIndexOf == -1) {
            return str;
        }
        String string = toString(i2);
        int i3 = 1;
        int length = string.length();
        StringBuffer stringBuffer = new StringBuffer(str);
        if (i >= 65536) {
            i3 = 2;
        }
        int i4 = i3;
        int i5 = iIndexOf;
        while (iIndexOf != -1) {
            stringBuffer.replace(i5, i5 + i4, string);
            int i6 = iIndexOf + i4;
            int iIndexOf2 = indexOf(str, i, i6);
            i5 += (length + iIndexOf2) - i6;
            iIndexOf = iIndexOf2;
        }
        return stringBuffer.toString();
    }

    public static String replace(String str, String str2, String str3) {
        int iIndexOf = indexOf(str, str2);
        if (iIndexOf == -1) {
            return str;
        }
        int length = str2.length();
        int length2 = str3.length();
        StringBuffer stringBuffer = new StringBuffer(str);
        int i = iIndexOf;
        while (iIndexOf != -1) {
            stringBuffer.replace(i, i + length, str3);
            int i2 = iIndexOf + length;
            int iIndexOf2 = indexOf(str, str2, i2);
            i += (length2 + iIndexOf2) - i2;
            iIndexOf = iIndexOf2;
        }
        return stringBuffer.toString();
    }

    public static StringBuffer reverse(StringBuffer stringBuffer) {
        int length = stringBuffer.length();
        StringBuffer stringBuffer2 = new StringBuffer(length);
        while (true) {
            int i = length - 1;
            if (length > 0) {
                char cCharAt = stringBuffer.charAt(i);
                if (isTrailSurrogate(cCharAt) && i > 0) {
                    char cCharAt2 = stringBuffer.charAt(i - 1);
                    if (isLeadSurrogate(cCharAt2)) {
                        stringBuffer2.append(cCharAt2);
                        stringBuffer2.append(cCharAt);
                        length = i - 1;
                    }
                }
                stringBuffer2.append(cCharAt);
                length = i;
            } else {
                return stringBuffer2;
            }
        }
    }

    public static boolean hasMoreCodePointsThan(String str, int i) {
        if (i < 0) {
            return true;
        }
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (((length + 1) >> 1) > i) {
            return true;
        }
        int i2 = length - i;
        if (i2 <= 0) {
            return false;
        }
        int i3 = i2;
        int i4 = 0;
        while (length != 0) {
            if (i == 0) {
                return true;
            }
            int i5 = i4 + 1;
            if (isLeadSurrogate(str.charAt(i4)) && i5 != length && isTrailSurrogate(str.charAt(i5))) {
                i5++;
                i3--;
                if (i3 <= 0) {
                    return false;
                }
            }
            i4 = i5;
            i--;
        }
        return false;
    }

    public static boolean hasMoreCodePointsThan(char[] cArr, int i, int i2, int i3) {
        int i4 = i2 - i;
        if (i4 < 0 || i < 0 || i2 < 0) {
            throw new IndexOutOfBoundsException("Start and limit indexes should be non-negative and start <= limit");
        }
        if (i3 < 0) {
            return true;
        }
        if (cArr != null) {
            if (((i4 + 1) >> 1) > i3) {
                return true;
            }
            int i5 = i4 - i3;
            if (i5 <= 0) {
                return false;
            }
            while (i4 != 0) {
                if (i3 == 0) {
                    return true;
                }
                int i6 = i + 1;
                if (isLeadSurrogate(cArr[i]) && i6 != i2 && isTrailSurrogate(cArr[i6])) {
                    i6++;
                    i5--;
                    if (i5 <= 0) {
                        return false;
                    }
                }
                i = i6;
                i3--;
            }
            return false;
        }
        return false;
    }

    public static boolean hasMoreCodePointsThan(StringBuffer stringBuffer, int i) {
        if (i < 0) {
            return true;
        }
        if (stringBuffer == null) {
            return false;
        }
        int length = stringBuffer.length();
        if (((length + 1) >> 1) > i) {
            return true;
        }
        int i2 = length - i;
        if (i2 <= 0) {
            return false;
        }
        int i3 = i2;
        int i4 = 0;
        while (length != 0) {
            if (i == 0) {
                return true;
            }
            int i5 = i4 + 1;
            if (isLeadSurrogate(stringBuffer.charAt(i4)) && i5 != length && isTrailSurrogate(stringBuffer.charAt(i5))) {
                i5++;
                i3--;
                if (i3 <= 0) {
                    return false;
                }
            }
            i4 = i5;
            i--;
        }
        return false;
    }

    public static String newString(int[] iArr, int i, int i2) {
        if (i2 < 0) {
            throw new IllegalArgumentException();
        }
        char[] cArr = new char[i2];
        int i3 = i2 + i;
        char[] cArr2 = cArr;
        int i4 = 0;
        for (int i5 = i; i5 < i3; i5++) {
            int i6 = iArr[i5];
            if (i6 >= 0 && i6 <= 1114111) {
                while (true) {
                    if (i6 < 65536) {
                        try {
                            cArr2[i4] = (char) i6;
                            i4++;
                            break;
                        } catch (IndexOutOfBoundsException e) {
                            char[] cArr3 = new char[(int) Math.ceil((((double) iArr.length) * ((double) (i4 + 2))) / ((double) ((i5 - i) + 1)))];
                            System.arraycopy(cArr2, 0, cArr3, 0, i4);
                            cArr2 = cArr3;
                        }
                    } else {
                        cArr2[i4] = (char) (LEAD_SURROGATE_OFFSET_ + (i6 >> 10));
                        cArr2[i4 + 1] = (char) (56320 + (i6 & 1023));
                        i4 += 2;
                        break;
                    }
                }
            } else {
                throw new IllegalArgumentException();
            }
        }
        return new String(cArr2, 0, i4);
    }

    public static final class StringComparator implements Comparator<String> {
        private static final int CODE_POINT_COMPARE_SURROGATE_OFFSET_ = 10240;
        public static final int FOLD_CASE_DEFAULT = 0;
        public static final int FOLD_CASE_EXCLUDE_SPECIAL_I = 1;
        private int m_codePointCompare_;
        private int m_foldCase_;
        private boolean m_ignoreCase_;

        public StringComparator() {
            this(false, false, 0);
        }

        public StringComparator(boolean z, boolean z2, int i) {
            setCodePointCompare(z);
            this.m_ignoreCase_ = z2;
            if (i < 0 || i > 1) {
                throw new IllegalArgumentException("Invalid fold case option");
            }
            this.m_foldCase_ = i;
        }

        public void setCodePointCompare(boolean z) {
            if (z) {
                this.m_codePointCompare_ = 32768;
            } else {
                this.m_codePointCompare_ = 0;
            }
        }

        public void setIgnoreCase(boolean z, int i) {
            this.m_ignoreCase_ = z;
            if (i < 0 || i > 1) {
                throw new IllegalArgumentException("Invalid fold case option");
            }
            this.m_foldCase_ = i;
        }

        public boolean getCodePointCompare() {
            return this.m_codePointCompare_ == 32768;
        }

        public boolean getIgnoreCase() {
            return this.m_ignoreCase_;
        }

        public int getIgnoreCaseOption() {
            return this.m_foldCase_;
        }

        @Override
        public int compare(String str, String str2) {
            if (Utility.sameObjects(str, str2)) {
                return 0;
            }
            if (str == null) {
                return -1;
            }
            if (str2 == null) {
                return 1;
            }
            if (this.m_ignoreCase_) {
                return compareCaseInsensitive(str, str2);
            }
            return compareCaseSensitive(str, str2);
        }

        private int compareCaseInsensitive(String str, String str2) {
            return Normalizer.cmpEquivFold(str, str2, this.m_foldCase_ | this.m_codePointCompare_ | 65536);
        }

        private int compareCaseSensitive(String str, String str2) {
            int i;
            int i2;
            int i3;
            int i4;
            int length = str.length();
            int length2 = str2.length();
            boolean z = false;
            if (length >= length2) {
                if (length <= length2) {
                    i = length;
                    i2 = 0;
                } else {
                    i = length2;
                    i2 = 1;
                }
            } else {
                i2 = -1;
                i = length;
            }
            int i5 = 0;
            char cCharAt = 0;
            char cCharAt2 = 0;
            while (i5 < i) {
                cCharAt = str.charAt(i5);
                cCharAt2 = str2.charAt(i5);
                if (cCharAt != cCharAt2) {
                    break;
                }
                i5++;
            }
            if (i5 == i) {
                return i2;
            }
            if (this.m_codePointCompare_ == 32768) {
                z = true;
            }
            if (cCharAt >= 55296 && cCharAt2 >= 55296 && z) {
                if ((cCharAt > 56319 || (i4 = i5 + 1) == length || !UTF16.isTrailSurrogate(str.charAt(i4))) && (!UTF16.isTrailSurrogate(cCharAt) || i5 == 0 || !UTF16.isLeadSurrogate(str.charAt(i5 - 1)))) {
                    cCharAt = (char) (cCharAt - 10240);
                }
                if ((cCharAt2 > 56319 || (i3 = i5 + 1) == length2 || !UTF16.isTrailSurrogate(str2.charAt(i3))) && (!UTF16.isTrailSurrogate(cCharAt2) || i5 == 0 || !UTF16.isLeadSurrogate(str2.charAt(i5 - 1)))) {
                    cCharAt2 = (char) (cCharAt2 - 10240);
                }
            }
            return cCharAt - cCharAt2;
        }
    }

    public static int getSingleCodePoint(CharSequence charSequence) {
        int iCodePointAt;
        if (charSequence == null || charSequence.length() == 0) {
            return -1;
        }
        if (charSequence.length() == 1) {
            return charSequence.charAt(0);
        }
        if (charSequence.length() > 2 || (iCodePointAt = Character.codePointAt(charSequence, 0)) <= 65535) {
            return -1;
        }
        return iCodePointAt;
    }

    public static int compareCodePoint(int i, CharSequence charSequence) {
        int length;
        if (charSequence == null || (length = charSequence.length()) == 0) {
            return 1;
        }
        int iCodePointAt = i - Character.codePointAt(charSequence, 0);
        if (iCodePointAt != 0) {
            return iCodePointAt;
        }
        return length == Character.charCount(i) ? 0 : -1;
    }

    private static String toString(int i) {
        if (i < 65536) {
            return String.valueOf((char) i);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getLeadSurrogate(i));
        sb.append(getTrailSurrogate(i));
        return sb.toString();
    }
}
