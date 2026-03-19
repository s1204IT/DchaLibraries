package android.icu.impl;

import android.icu.impl.locale.LanguageTag;
import android.icu.lang.UCharacter;
import android.icu.lang.UCharacterEnums;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.Replaceable;
import android.icu.text.UTF16;
import android.icu.text.UnicodeMatcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Utility {
    private static final char APOSTROPHE = '\'';
    private static final char BACKSLASH = '\\';
    private static final char ESCAPE = 42405;
    static final byte ESCAPE_BYTE = -91;
    private static final int MAGIC_UNSIGNED = Integer.MIN_VALUE;
    public static String LINE_SEPARATOR = System.getProperty("line.separator");
    static final char[] HEX_DIGIT = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] UNESCAPE_MAP = {'a', 7, 'b', '\b', 'e', 27, 'f', '\f', 'n', '\n', 'r', '\r', 't', '\t', 'v', 11};
    static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    public static final boolean arrayEquals(Object[] objArr, Object obj) {
        if (objArr == null) {
            return obj == null;
        }
        if (!(obj instanceof Object[])) {
            return false;
        }
        Object[] objArr2 = (Object[]) obj;
        return objArr.length == objArr2.length && arrayRegionMatches(objArr, 0, objArr2, 0, objArr.length);
    }

    public static final boolean arrayEquals(int[] iArr, Object obj) {
        if (iArr == null) {
            return obj == null;
        }
        if (!(obj instanceof int[])) {
            return false;
        }
        int[] iArr2 = (int[]) obj;
        return iArr.length == iArr2.length && arrayRegionMatches(iArr, 0, iArr2, 0, iArr.length);
    }

    public static final boolean arrayEquals(double[] dArr, Object obj) {
        if (dArr == null) {
            return obj == null;
        }
        if (!(obj instanceof double[])) {
            return false;
        }
        double[] dArr2 = (double[]) obj;
        return dArr.length == dArr2.length && arrayRegionMatches(dArr, 0, dArr2, 0, dArr.length);
    }

    public static final boolean arrayEquals(byte[] bArr, Object obj) {
        if (bArr == null) {
            return obj == null;
        }
        if (!(obj instanceof byte[])) {
            return false;
        }
        byte[] bArr2 = (byte[]) obj;
        return bArr.length == bArr2.length && arrayRegionMatches(bArr, 0, bArr2, 0, bArr.length);
    }

    public static final boolean arrayEquals(Object obj, Object obj2) {
        if (obj == null) {
            return obj2 == null;
        }
        if (obj instanceof Object[]) {
            return arrayEquals((Object[]) obj, obj2);
        }
        if (obj instanceof int[]) {
            return arrayEquals((int[]) obj, obj2);
        }
        if (obj instanceof double[]) {
            return arrayEquals((double[]) obj, obj2);
        }
        if (obj instanceof byte[]) {
            return arrayEquals((byte[]) obj, obj2);
        }
        return obj.equals(obj2);
    }

    public static final boolean arrayRegionMatches(Object[] objArr, int i, Object[] objArr2, int i2, int i3) {
        int i4 = i3 + i;
        int i5 = i2 - i;
        while (i < i4) {
            if (arrayEquals(objArr[i], objArr2[i + i5])) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    public static final boolean arrayRegionMatches(char[] cArr, int i, char[] cArr2, int i2, int i3) {
        int i4 = i3 + i;
        int i5 = i2 - i;
        while (i < i4) {
            if (cArr[i] == cArr2[i + i5]) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    public static final boolean arrayRegionMatches(int[] iArr, int i, int[] iArr2, int i2, int i3) {
        int i4 = i3 + i;
        int i5 = i2 - i;
        while (i < i4) {
            if (iArr[i] == iArr2[i + i5]) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    public static final boolean arrayRegionMatches(double[] dArr, int i, double[] dArr2, int i2, int i3) {
        int i4 = i3 + i;
        int i5 = i2 - i;
        while (i < i4) {
            if (dArr[i] == dArr2[i + i5]) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    public static final boolean arrayRegionMatches(byte[] bArr, int i, byte[] bArr2, int i2, int i3) {
        int i4 = i3 + i;
        int i5 = i2 - i;
        while (i < i4) {
            if (bArr[i] == bArr2[i + i5]) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    public static final boolean sameObjects(Object obj, Object obj2) {
        return obj == obj2;
    }

    public static final boolean objectEquals(Object obj, Object obj2) {
        if (obj == null) {
            return obj2 == null;
        }
        if (obj2 == null) {
            return false;
        }
        return obj.equals(obj2);
    }

    public static <T extends Comparable<T>> int checkCompare(T t, T t2) {
        if (t == null) {
            return t2 == null ? 0 : -1;
        }
        if (t2 == null) {
            return 1;
        }
        return t.compareTo(t2);
    }

    public static int checkHash(Object obj) {
        if (obj == null) {
            return 0;
        }
        return obj.hashCode();
    }

    public static final String arrayToRLEString(int[] iArr) {
        StringBuilder sb = new StringBuilder();
        appendInt(sb, iArr.length);
        int i = iArr[0];
        int i2 = 1;
        for (int i3 = 1; i3 < iArr.length; i3++) {
            int i4 = iArr[i3];
            if (i4 == i && i2 < 65535) {
                i2++;
            } else {
                encodeRun(sb, i, i2);
                i2 = 1;
                i = i4;
            }
        }
        encodeRun(sb, i, i2);
        return sb.toString();
    }

    public static final String arrayToRLEString(short[] sArr) {
        StringBuilder sb = new StringBuilder();
        sb.append((char) (sArr.length >> 16));
        sb.append((char) sArr.length);
        short s = sArr[0];
        int i = 1;
        for (int i2 = 1; i2 < sArr.length; i2++) {
            short s2 = sArr[i2];
            if (s2 != s || i >= 65535) {
                encodeRun(sb, s, i);
                i = 1;
                s = s2;
            } else {
                i++;
            }
        }
        encodeRun(sb, s, i);
        return sb.toString();
    }

    public static final String arrayToRLEString(char[] cArr) {
        StringBuilder sb = new StringBuilder();
        sb.append((char) (cArr.length >> 16));
        sb.append((char) cArr.length);
        char c = cArr[0];
        int i = 1;
        for (int i2 = 1; i2 < cArr.length; i2++) {
            char c2 = cArr[i2];
            if (c2 != c || i >= 65535) {
                encodeRun(sb, (short) c, i);
                i = 1;
                c = c2;
            } else {
                i++;
            }
        }
        encodeRun(sb, (short) c, i);
        return sb.toString();
    }

    public static final String arrayToRLEString(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        sb.append((char) (bArr.length >> 16));
        sb.append((char) bArr.length);
        byte[] bArr2 = new byte[2];
        byte b = bArr[0];
        int i = 1;
        for (int i2 = 1; i2 < bArr.length; i2++) {
            byte b2 = bArr[i2];
            if (b2 != b || i >= 255) {
                encodeRun(sb, b, i, bArr2);
                i = 1;
                b = b2;
            } else {
                i++;
            }
        }
        encodeRun(sb, b, i, bArr2);
        if (bArr2[0] != 0) {
            appendEncodedByte(sb, (byte) 0, bArr2);
        }
        return sb.toString();
    }

    private static final <T extends Appendable> void encodeRun(T t, int i, int i2) {
        if (i2 < 4) {
            for (int i3 = 0; i3 < i2; i3++) {
                if (i == 42405) {
                    appendInt(t, i);
                }
                appendInt(t, i);
            }
            return;
        }
        if (i2 == 42405) {
            if (i == 42405) {
                appendInt(t, 42405);
            }
            appendInt(t, i);
            i2--;
        }
        appendInt(t, 42405);
        appendInt(t, i2);
        appendInt(t, i);
    }

    private static final <T extends Appendable> void appendInt(T t, int i) {
        try {
            t.append((char) (i >>> 16));
            t.append((char) (i & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH));
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    private static final <T extends Appendable> void encodeRun(T t, short s, int i) {
        char c = (char) s;
        try {
            if (i < 4) {
                for (int i2 = 0; i2 < i; i2++) {
                    if (c == 42405) {
                        t.append(ESCAPE);
                    }
                    t.append(c);
                }
                return;
            }
            if (i == 42405) {
                if (c == 42405) {
                    t.append(ESCAPE);
                }
                t.append(c);
                i--;
            }
            t.append(ESCAPE);
            t.append((char) i);
            t.append(c);
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    private static final <T extends Appendable> void encodeRun(T t, byte b, int i, byte[] bArr) {
        if (i >= 4) {
            if (((byte) i) == -91) {
                if (b == -91) {
                    appendEncodedByte(t, ESCAPE_BYTE, bArr);
                }
                appendEncodedByte(t, b, bArr);
                i--;
            }
            appendEncodedByte(t, ESCAPE_BYTE, bArr);
            appendEncodedByte(t, (byte) i, bArr);
            appendEncodedByte(t, b, bArr);
            return;
        }
        for (int i2 = 0; i2 < i; i2++) {
            if (b == -91) {
                appendEncodedByte(t, ESCAPE_BYTE, bArr);
            }
            appendEncodedByte(t, b, bArr);
        }
    }

    private static final <T extends Appendable> void appendEncodedByte(T t, byte b, byte[] bArr) {
        try {
            if (bArr[0] != 0) {
                t.append((char) ((b & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | (bArr[1] << 8)));
                bArr[0] = 0;
            } else {
                bArr[0] = 1;
                bArr[1] = b;
            }
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    public static final int[] RLEStringToIntArray(String str) {
        int i;
        int i2 = getInt(str, 0);
        int[] iArr = new int[i2];
        int length = str.length() / 2;
        int i3 = 1;
        int i4 = 0;
        while (i4 < i2 && i3 < length) {
            int i5 = i3 + 1;
            int i6 = getInt(str, i3);
            if (i6 == 42405) {
                i3 = i5 + 1;
                int i7 = getInt(str, i5);
                if (i7 == 42405) {
                    i = i4 + 1;
                    iArr[i4] = i7;
                } else {
                    int i8 = i3 + 1;
                    int i9 = getInt(str, i3);
                    int i10 = i4;
                    int i11 = 0;
                    while (i11 < i7) {
                        iArr[i10] = i9;
                        i11++;
                        i10++;
                    }
                    i3 = i8;
                    i4 = i10;
                }
            } else {
                i = i4 + 1;
                iArr[i4] = i6;
                i3 = i5;
            }
            i4 = i;
        }
        if (i4 != i2 || i3 != length) {
            throw new IllegalStateException("Bad run-length encoded int array");
        }
        return iArr;
    }

    static final int getInt(String str, int i) {
        int i2 = 2 * i;
        return str.charAt(i2 + 1) | (str.charAt(i2) << 16);
    }

    public static final short[] RLEStringToShortArray(String str) {
        int i;
        int iCharAt = (str.charAt(0) << 16) | str.charAt(1);
        short[] sArr = new short[iCharAt];
        int i2 = 2;
        int i3 = 0;
        while (i2 < str.length()) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == 42405) {
                i2++;
                char cCharAt2 = str.charAt(i2);
                if (cCharAt2 == 42405) {
                    i = i3 + 1;
                    sArr[i3] = (short) cCharAt2;
                } else {
                    i2++;
                    short sCharAt = (short) str.charAt(i2);
                    int i4 = i3;
                    int i5 = 0;
                    while (i5 < cCharAt2) {
                        sArr[i4] = sCharAt;
                        i5++;
                        i4++;
                    }
                    i3 = i4;
                    i2++;
                }
            } else {
                i = i3 + 1;
                sArr[i3] = (short) cCharAt;
            }
            i3 = i;
            i2++;
        }
        if (i3 != iCharAt) {
            throw new IllegalStateException("Bad run-length encoded short array");
        }
        return sArr;
    }

    public static final char[] RLEStringToCharArray(String str) {
        int i;
        int iCharAt = (str.charAt(0) << 16) | str.charAt(1);
        char[] cArr = new char[iCharAt];
        int i2 = 2;
        int i3 = 0;
        while (i2 < str.length()) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == 42405) {
                i2++;
                char cCharAt2 = str.charAt(i2);
                if (cCharAt2 == 42405) {
                    i = i3 + 1;
                    cArr[i3] = cCharAt2;
                } else {
                    i2++;
                    char cCharAt3 = str.charAt(i2);
                    int i4 = i3;
                    int i5 = 0;
                    while (i5 < cCharAt2) {
                        cArr[i4] = cCharAt3;
                        i5++;
                        i4++;
                    }
                    i3 = i4;
                    i2++;
                }
            } else {
                i = i3 + 1;
                cArr[i3] = cCharAt;
            }
            i3 = i;
            i2++;
        }
        if (i3 != iCharAt) {
            throw new IllegalStateException("Bad run-length encoded short array");
        }
        return cArr;
    }

    public static final byte[] RLEStringToByteArray(String str) {
        char c;
        boolean z;
        byte b;
        int iCharAt = (str.charAt(0) << 16) | str.charAt(1);
        byte[] bArr = new byte[iCharAt];
        int i = 0;
        char c2 = 0;
        char c3 = 0;
        int i2 = 0;
        boolean z2 = true;
        int i3 = 2;
        while (i < iCharAt) {
            if (z2) {
                int i4 = i3 + 1;
                char cCharAt = str.charAt(i3);
                byte b2 = (byte) (cCharAt >> '\b');
                c = cCharAt;
                i3 = i4;
                b = b2;
                z = false;
            } else {
                byte b3 = (byte) (c3 & 255);
                c = c3;
                z = true;
                b = b3;
            }
            int i5 = b;
            switch (c2) {
                case 0:
                    if (b != -91) {
                        bArr[i] = b;
                        i++;
                    } else {
                        c2 = 1;
                    }
                    break;
                case 1:
                    if (b != -91) {
                        if (b < 0) {
                            i5 = b + 256;
                        }
                        c2 = 2;
                        i2 = i5;
                    } else {
                        bArr[i] = ESCAPE_BYTE;
                        c2 = 0;
                        i++;
                    }
                    break;
                case 2:
                    int i6 = i;
                    int i7 = 0;
                    while (i7 < i2) {
                        bArr[i6] = b;
                        i7++;
                        i6++;
                    }
                    i = i6;
                    c2 = 0;
                    break;
            }
            z2 = z;
            c3 = c;
            i2 = i2;
        }
        if (c2 != 0) {
            throw new IllegalStateException("Bad run-length encoded byte array");
        }
        if (i3 == str.length()) {
            return bArr;
        }
        throw new IllegalStateException("Excess data in RLE byte array string");
    }

    public static final String formatForSource(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            if (i > 0) {
                sb.append('+');
                sb.append(LINE_SEPARATOR);
            }
            sb.append("        \"");
            int i2 = 11;
            while (i < str.length() && i2 < 80) {
                int i3 = i + 1;
                char cCharAt = str.charAt(i);
                if (cCharAt < ' ' || cCharAt == '\"' || cCharAt == '\\') {
                    if (cCharAt == '\n') {
                        sb.append("\\n");
                        i2 += 2;
                    } else if (cCharAt == '\t') {
                        sb.append("\\t");
                        i2 += 2;
                    } else if (cCharAt == '\r') {
                        sb.append("\\r");
                        i2 += 2;
                    } else {
                        sb.append('\\');
                        sb.append(HEX_DIGIT[(cCharAt & 448) >> 6]);
                        sb.append(HEX_DIGIT[(cCharAt & '8') >> 3]);
                        sb.append(HEX_DIGIT[cCharAt & 7]);
                        i2 += 4;
                    }
                } else if (cCharAt <= '~') {
                    sb.append(cCharAt);
                    i2++;
                } else {
                    sb.append("\\u");
                    sb.append(HEX_DIGIT[(61440 & cCharAt) >> 12]);
                    sb.append(HEX_DIGIT[(cCharAt & 3840) >> 8]);
                    sb.append(HEX_DIGIT[(cCharAt & 240) >> 4]);
                    sb.append(HEX_DIGIT[cCharAt & 15]);
                    i2 += 6;
                }
                i = i3;
            }
            sb.append('\"');
        }
        return sb.toString();
    }

    public static final String format1ForSource(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        int i = 0;
        while (i < str.length()) {
            int i2 = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt < ' ' || cCharAt == '\"' || cCharAt == '\\') {
                if (cCharAt == '\n') {
                    sb.append("\\n");
                } else if (cCharAt == '\t') {
                    sb.append("\\t");
                } else if (cCharAt == '\r') {
                    sb.append("\\r");
                } else {
                    sb.append('\\');
                    sb.append(HEX_DIGIT[(cCharAt & 448) >> 6]);
                    sb.append(HEX_DIGIT[(cCharAt & '8') >> 3]);
                    sb.append(HEX_DIGIT[cCharAt & 7]);
                }
            } else if (cCharAt <= '~') {
                sb.append(cCharAt);
            } else {
                sb.append("\\u");
                sb.append(HEX_DIGIT[(61440 & cCharAt) >> 12]);
                sb.append(HEX_DIGIT[(cCharAt & 3840) >> 8]);
                sb.append(HEX_DIGIT[(cCharAt & 240) >> 4]);
                sb.append(HEX_DIGIT[cCharAt & 15]);
            }
            i = i2;
        }
        sb.append('\"');
        return sb.toString();
    }

    public static final String escape(String str) {
        StringBuilder sb = new StringBuilder();
        int charCount = 0;
        while (charCount < str.length()) {
            int iCodePointAt = Character.codePointAt(str, charCount);
            charCount += UTF16.getCharCount(iCodePointAt);
            if (iCodePointAt >= 32 && iCodePointAt <= 127) {
                if (iCodePointAt == 92) {
                    sb.append("\\\\");
                } else {
                    sb.append((char) iCodePointAt);
                }
            } else {
                boolean z = iCodePointAt <= 65535;
                sb.append(z ? "\\u" : "\\U");
                sb.append(hex(iCodePointAt, z ? 4 : 8));
            }
        }
        return sb.toString();
    }

    public static int unescapeAt(String str, int[] iArr) {
        int codePoint;
        int i;
        boolean z;
        int i2;
        int i3;
        int i4;
        int i5 = iArr[0];
        int length = str.length();
        if (i5 < 0 || i5 >= length) {
            return -1;
        }
        int iCodePointAt = Character.codePointAt(str, i5);
        int charCount = i5 + UTF16.getCharCount(iCodePointAt);
        int i6 = 4;
        int i7 = 3;
        if (iCodePointAt == 85) {
            codePoint = 0;
            i = 0;
            z = false;
            i2 = 4;
            i6 = 8;
            i3 = 8;
        } else if (iCodePointAt == 117) {
            codePoint = 0;
            i = 0;
            z = false;
            i3 = 4;
            i2 = 4;
        } else if (iCodePointAt != 120) {
            codePoint = UCharacter.digit(iCodePointAt, 8);
            if (codePoint >= 0) {
                z = false;
                i3 = 3;
                i2 = 3;
                i6 = 1;
                i = 1;
            } else {
                codePoint = 0;
                i = 0;
                i3 = 0;
                z = false;
                i2 = 4;
                i6 = 0;
            }
        } else if (charCount >= length || UTF16.charAt(str, charCount) != 123) {
            i = 0;
            z = false;
            i3 = 2;
            i2 = 4;
            i6 = 1;
            codePoint = 0;
        } else {
            charCount++;
            codePoint = 0;
            i = 0;
            i2 = 4;
            i3 = 8;
            i6 = 1;
            z = true;
        }
        if (i6 == 0) {
            for (int i8 = 0; i8 < UNESCAPE_MAP.length; i8 += 2) {
                if (iCodePointAt == UNESCAPE_MAP[i8]) {
                    iArr[0] = charCount;
                    return UNESCAPE_MAP[i8 + 1];
                }
                if (iCodePointAt < UNESCAPE_MAP[i8]) {
                    break;
                }
            }
            if (iCodePointAt != 99 || charCount >= length) {
                iArr[0] = charCount;
                return iCodePointAt;
            }
            int iCharAt = UTF16.charAt(str, charCount);
            iArr[0] = charCount + UTF16.getCharCount(iCharAt);
            return iCharAt & 31;
        }
        while (charCount < length && i < i3) {
            iCodePointAt = UTF16.charAt(str, charCount);
            int iDigit = UCharacter.digit(iCodePointAt, i2 == i7 ? 8 : 16);
            if (iDigit < 0) {
                break;
            }
            codePoint = (codePoint << i2) | iDigit;
            charCount += UTF16.getCharCount(iCodePointAt);
            i++;
            i7 = 3;
        }
        if (i < i6) {
            return -1;
        }
        if (z) {
            if (iCodePointAt != 125) {
                return -1;
            }
            charCount++;
        }
        if (codePoint < 0 || codePoint >= 1114112) {
            return -1;
        }
        if (charCount < length) {
            char c = (char) codePoint;
            if (UTF16.isLeadSurrogate(c)) {
                int i9 = charCount + 1;
                int iCharAt2 = str.charAt(charCount);
                if (iCharAt2 != 92 || i9 >= length) {
                    i4 = i9;
                } else {
                    int[] iArr2 = {i9};
                    iCharAt2 = unescapeAt(str, iArr2);
                    i4 = iArr2[0];
                }
                char c2 = (char) iCharAt2;
                if (UTF16.isTrailSurrogate(c2)) {
                    codePoint = Character.toCodePoint(c, c2);
                } else {
                    i4 = charCount;
                }
            }
        }
        iArr[0] = i4;
        return codePoint;
    }

    public static String unescape(String str) {
        StringBuilder sb = new StringBuilder();
        int[] iArr = new int[1];
        int i = 0;
        while (i < str.length()) {
            int i2 = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt == '\\') {
                iArr[0] = i2;
                int iUnescapeAt = unescapeAt(str, iArr);
                if (iUnescapeAt < 0) {
                    throw new IllegalArgumentException("Invalid escape sequence " + str.substring(i2 - 1, Math.min(i2 + 8, str.length())));
                }
                sb.appendCodePoint(iUnescapeAt);
                i = iArr[0];
            } else {
                sb.append(cCharAt);
                i = i2;
            }
        }
        return sb.toString();
    }

    public static String unescapeLeniently(String str) {
        StringBuilder sb = new StringBuilder();
        int[] iArr = new int[1];
        int i = 0;
        while (i < str.length()) {
            int i2 = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt == '\\') {
                iArr[0] = i2;
                int iUnescapeAt = unescapeAt(str, iArr);
                if (iUnescapeAt < 0) {
                    sb.append(cCharAt);
                } else {
                    sb.appendCodePoint(iUnescapeAt);
                    i2 = iArr[0];
                }
            } else {
                sb.append(cCharAt);
            }
            i = i2;
        }
        return sb.toString();
    }

    public static String hex(long j) {
        return hex(j, 4);
    }

    public static String hex(long j, int i) {
        if (j == Long.MIN_VALUE) {
            return "-8000000000000000";
        }
        boolean z = j < 0;
        if (z) {
            j = -j;
        }
        String upperCase = Long.toString(j, 16).toUpperCase(Locale.ENGLISH);
        if (upperCase.length() < i) {
            upperCase = "0000000000000000".substring(upperCase.length(), i) + upperCase;
        }
        if (z) {
            return '-' + upperCase;
        }
        return upperCase;
    }

    public static String hex(CharSequence charSequence) {
        return ((StringBuilder) hex(charSequence, 4, ",", true, new StringBuilder())).toString();
    }

    public static <S extends CharSequence, U extends CharSequence, T extends Appendable> T hex(S s, int i, U u, boolean z, T t) {
        int charCount = 0;
        try {
            if (z) {
                while (charCount < s.length()) {
                    int iCodePointAt = Character.codePointAt(s, charCount);
                    if (charCount != 0) {
                        t.append(u);
                    }
                    t.append(hex(iCodePointAt, i));
                    charCount += UTF16.getCharCount(iCodePointAt);
                }
            } else {
                while (charCount < s.length()) {
                    if (charCount != 0) {
                        t.append(u);
                    }
                    t.append(hex(s.charAt(charCount), i));
                    charCount++;
                }
            }
            return t;
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    public static String hex(byte[] bArr, int i, int i2, String str) {
        StringBuilder sb = new StringBuilder();
        while (i < i2) {
            if (i != 0) {
                sb.append(str);
            }
            sb.append(hex(bArr[i]));
            i++;
        }
        return sb.toString();
    }

    public static <S extends CharSequence> String hex(S s, int i, S s2) {
        return ((StringBuilder) hex(s, i, s2, true, new StringBuilder())).toString();
    }

    public static void split(String str, char c, String[] strArr) {
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        while (i < str.length()) {
            if (str.charAt(i) == c) {
                strArr[i2] = str.substring(i3, i);
                i3 = i + 1;
                i2++;
            }
            i++;
        }
        strArr[i2] = str.substring(i3, i);
        for (int i4 = i2 + 1; i4 < strArr.length; i4++) {
            strArr[i4] = "";
        }
    }

    public static String[] split(String str, char c) {
        ArrayList arrayList = new ArrayList();
        int i = 0;
        int i2 = 0;
        while (i < str.length()) {
            if (str.charAt(i) == c) {
                arrayList.add(str.substring(i2, i));
                i2 = i + 1;
            }
            i++;
        }
        arrayList.add(str.substring(i2, i));
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public static int lookup(String str, String[] strArr) {
        for (int i = 0; i < strArr.length; i++) {
            if (str.equals(strArr[i])) {
                return i;
            }
        }
        return -1;
    }

    public static boolean parseChar(String str, int[] iArr, char c) {
        int i = iArr[0];
        iArr[0] = PatternProps.skipWhiteSpace(str, iArr[0]);
        if (iArr[0] != str.length() && str.charAt(iArr[0]) == c) {
            iArr[0] = iArr[0] + 1;
            return true;
        }
        iArr[0] = i;
        return false;
    }

    public static int parsePattern(String str, int i, int i2, String str2, int[] iArr) {
        int[] iArr2 = new int[1];
        int iSkipWhiteSpace = i;
        int i3 = 0;
        for (int i4 = 0; i4 < str2.length(); i4++) {
            char cCharAt = str2.charAt(i4);
            if (cCharAt != ' ') {
                if (cCharAt == '#') {
                    iArr2[0] = iSkipWhiteSpace;
                    int i5 = i3 + 1;
                    iArr[i3] = parseInteger(str, iArr2, i2);
                    if (iArr2[0] == iSkipWhiteSpace) {
                        return -1;
                    }
                    iSkipWhiteSpace = iArr2[0];
                    i3 = i5;
                } else if (cCharAt != '~') {
                    if (iSkipWhiteSpace >= i2) {
                        return -1;
                    }
                    int i6 = iSkipWhiteSpace + 1;
                    if (((char) UCharacter.toLowerCase(str.charAt(iSkipWhiteSpace))) != cCharAt) {
                        return -1;
                    }
                    iSkipWhiteSpace = i6;
                }
            } else {
                if (iSkipWhiteSpace >= i2) {
                    return -1;
                }
                int i7 = iSkipWhiteSpace + 1;
                if (!PatternProps.isWhiteSpace(str.charAt(iSkipWhiteSpace))) {
                    return -1;
                }
                iSkipWhiteSpace = i7;
            }
            iSkipWhiteSpace = PatternProps.skipWhiteSpace(str, iSkipWhiteSpace);
        }
        return iSkipWhiteSpace;
    }

    public static int parsePattern(String str, Replaceable replaceable, int i, int i2) {
        if (str.length() == 0) {
            return i;
        }
        int i3 = 0;
        int iCodePointAt = Character.codePointAt(str, 0);
        while (i < i2) {
            int iChar32At = replaceable.char32At(i);
            if (iCodePointAt == 126) {
                if (PatternProps.isWhiteSpace(iChar32At)) {
                    i += UTF16.getCharCount(iChar32At);
                } else {
                    i3++;
                    if (i3 == str.length()) {
                        return i;
                    }
                }
            } else {
                if (iChar32At != iCodePointAt) {
                    return -1;
                }
                int charCount = UTF16.getCharCount(iChar32At);
                i += charCount;
                i3 += charCount;
                if (i3 == str.length()) {
                    return i;
                }
            }
            iCodePointAt = UTF16.charAt(str, i3);
        }
        return -1;
    }

    public static int parseInteger(String str, int[] iArr, int i) {
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7 = iArr[0];
        if (str.regionMatches(true, i7, "0x", 0, 2)) {
            i7 += 2;
            i2 = 16;
        } else {
            if (i7 < i && str.charAt(i7) == '0') {
                i7++;
                i2 = 8;
                i3 = 1;
                i4 = 0;
                while (true) {
                    if (i7 < i) {
                        break;
                    }
                    i5 = i7 + 1;
                    int iDigit = UCharacter.digit(str.charAt(i7), i2);
                    if (iDigit < 0) {
                        i7 = i5 - 1;
                        break;
                    }
                    i3++;
                    i6 = iDigit + (i4 * i2);
                    if (i6 <= i4) {
                        return 0;
                    }
                    i7 = i5;
                    i4 = i6;
                }
                if (i3 > 0) {
                    iArr[0] = i7;
                }
                return i4;
            }
            i2 = 10;
        }
        i3 = 0;
        i4 = 0;
        while (true) {
            if (i7 < i) {
            }
            i7 = i5;
            i4 = i6;
        }
        if (i3 > 0) {
        }
        return i4;
    }

    public static String parseUnicodeIdentifier(String str, int[] iArr) {
        StringBuilder sb = new StringBuilder();
        int charCount = iArr[0];
        while (charCount < str.length()) {
            int iCodePointAt = Character.codePointAt(str, charCount);
            if (sb.length() == 0) {
                if (UCharacter.isUnicodeIdentifierStart(iCodePointAt)) {
                    sb.appendCodePoint(iCodePointAt);
                } else {
                    return null;
                }
            } else {
                if (!UCharacter.isUnicodeIdentifierPart(iCodePointAt)) {
                    break;
                }
                sb.appendCodePoint(iCodePointAt);
            }
            charCount += UTF16.getCharCount(iCodePointAt);
        }
        iArr[0] = charCount;
        return sb.toString();
    }

    private static <T extends Appendable> void recursiveAppendNumber(T t, int i, int i2, int i3) {
        try {
            int i4 = i % i2;
            if (i >= i2 || i3 > 1) {
                recursiveAppendNumber(t, i / i2, i2, i3 - 1);
            }
            t.append(DIGITS[i4]);
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    public static <T extends Appendable> T appendNumber(T t, int i, int i2, int i3) {
        try {
            if (i2 < 2 || i2 > 36) {
                throw new IllegalArgumentException("Illegal radix " + i2);
            }
            if (i < 0) {
                i = -i;
                t.append(LanguageTag.SEP);
            }
            recursiveAppendNumber(t, i, i2, i3);
            return t;
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    public static int parseNumber(String str, int[] iArr, int i) {
        int iDigit;
        int i2 = iArr[0];
        int i3 = 0;
        while (i2 < str.length() && (iDigit = UCharacter.digit(Character.codePointAt(str, i2), i)) >= 0) {
            i3 = (i3 * i) + iDigit;
            if (i3 < 0) {
                return -1;
            }
            i2++;
        }
        if (i2 == iArr[0]) {
            return -1;
        }
        iArr[0] = i2;
        return i3;
    }

    public static boolean isUnprintable(int i) {
        return i < 32 || i > 126;
    }

    public static <T extends Appendable> boolean escapeUnprintable(T t, int i) {
        try {
            if (isUnprintable(i)) {
                t.append('\\');
                if (((-65536) & i) != 0) {
                    t.append('U');
                    t.append(DIGITS[(i >> 28) & 15]);
                    t.append(DIGITS[(i >> 24) & 15]);
                    t.append(DIGITS[(i >> 20) & 15]);
                    t.append(DIGITS[(i >> 16) & 15]);
                } else {
                    t.append('u');
                }
                t.append(DIGITS[(i >> 12) & 15]);
                t.append(DIGITS[(i >> 8) & 15]);
                t.append(DIGITS[(i >> 4) & 15]);
                t.append(DIGITS[i & 15]);
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new IllegalIcuArgumentException(e);
        }
    }

    public static int quotedIndexOf(String str, int i, int i2, String str2) {
        while (i < i2) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\\') {
                i++;
            } else if (cCharAt == '\'') {
                do {
                    i++;
                    if (i < i2) {
                    }
                } while (str.charAt(i) != '\'');
            } else if (str2.indexOf(cCharAt) >= 0) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static void appendToRule(StringBuffer stringBuffer, int i, boolean z, boolean z2, StringBuffer stringBuffer2) {
        if (z || (z2 && isUnprintable(i))) {
            if (stringBuffer2.length() > 0) {
                while (stringBuffer2.length() >= 2 && stringBuffer2.charAt(0) == '\'' && stringBuffer2.charAt(1) == '\'') {
                    stringBuffer.append('\\');
                    stringBuffer.append('\'');
                    stringBuffer2.delete(0, 2);
                }
                int i2 = 0;
                while (stringBuffer2.length() >= 2 && stringBuffer2.charAt(stringBuffer2.length() - 2) == '\'' && stringBuffer2.charAt(stringBuffer2.length() - 1) == '\'') {
                    stringBuffer2.setLength(stringBuffer2.length() - 2);
                    i2++;
                }
                if (stringBuffer2.length() > 0) {
                    stringBuffer.append('\'');
                    stringBuffer.append(stringBuffer2);
                    stringBuffer.append('\'');
                    stringBuffer2.setLength(0);
                }
                while (true) {
                    int i3 = i2 - 1;
                    if (i2 <= 0) {
                        break;
                    }
                    stringBuffer.append('\\');
                    stringBuffer.append('\'');
                    i2 = i3;
                }
            }
            if (i != -1) {
                if (i != 32) {
                    if (!z2 || !escapeUnprintable(stringBuffer, i)) {
                        stringBuffer.appendCodePoint(i);
                        return;
                    }
                    return;
                }
                int length = stringBuffer.length();
                if (length > 0 && stringBuffer.charAt(length - 1) != ' ') {
                    stringBuffer.append(' ');
                    return;
                }
                return;
            }
            return;
        }
        if (stringBuffer2.length() == 0 && (i == 39 || i == 92)) {
            stringBuffer.append('\\');
            stringBuffer.append((char) i);
            return;
        }
        if (stringBuffer2.length() > 0 || ((i >= 33 && i <= 126 && ((i < 48 || i > 57) && ((i < 65 || i > 90) && (i < 97 || i > 122)))) || PatternProps.isWhiteSpace(i))) {
            stringBuffer2.appendCodePoint(i);
            if (i == 39) {
                stringBuffer2.append((char) i);
                return;
            }
            return;
        }
        stringBuffer.appendCodePoint(i);
    }

    public static void appendToRule(StringBuffer stringBuffer, String str, boolean z, boolean z2, StringBuffer stringBuffer2) {
        for (int i = 0; i < str.length(); i++) {
            appendToRule(stringBuffer, str.charAt(i), z, z2, stringBuffer2);
        }
    }

    public static void appendToRule(StringBuffer stringBuffer, UnicodeMatcher unicodeMatcher, boolean z, StringBuffer stringBuffer2) {
        if (unicodeMatcher != null) {
            appendToRule(stringBuffer, unicodeMatcher.toPattern(z), true, z, stringBuffer2);
        }
    }

    public static final int compareUnsigned(int i, int i2) {
        int i3 = i - Integer.MIN_VALUE;
        int i4 = i2 - Integer.MIN_VALUE;
        if (i3 < i4) {
            return -1;
        }
        if (i3 > i4) {
            return 1;
        }
        return 0;
    }

    public static final byte highBit(int i) {
        if (i <= 0) {
            return (byte) -1;
        }
        byte b = 0;
        if (i >= 65536) {
            i >>= 16;
            b = (byte) 16;
        }
        if (i >= 256) {
            i >>= 8;
            b = (byte) (b + 8);
        }
        if (i >= 16) {
            i >>= 4;
            b = (byte) (b + 4);
        }
        if (i >= 4) {
            i >>= 2;
            b = (byte) (b + 2);
        }
        if (i >= 2) {
            return (byte) (b + 1);
        }
        return b;
    }

    public static String valueOf(int[] iArr) {
        StringBuilder sb = new StringBuilder(iArr.length);
        for (int i : iArr) {
            sb.appendCodePoint(i);
        }
        return sb.toString();
    }

    public static String repeat(String str, int i) {
        if (i <= 0) {
            return "";
        }
        if (i == 1) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < i; i2++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static String[] splitString(String str, String str2) {
        return str.split("\\Q" + str2 + "\\E");
    }

    public static String[] splitWhitespace(String str) {
        return str.split("\\s+");
    }

    public static String fromHex(String str, int i, String str2) {
        if (str2 == null) {
            str2 = "\\s+";
        }
        return fromHex(str, i, Pattern.compile(str2));
    }

    public static String fromHex(String str, int i, Pattern pattern) {
        StringBuilder sb = new StringBuilder();
        for (String str2 : pattern.split(str)) {
            if (str2.length() < i) {
                throw new IllegalArgumentException("code point too short: " + str2);
            }
            sb.appendCodePoint(Integer.parseInt(str2, 16));
        }
        return sb.toString();
    }

    public static boolean equals(Object obj, Object obj2) {
        return obj == obj2 || !(obj == null || obj2 == null || !obj.equals(obj2));
    }

    public static int hash(Object... objArr) {
        return Arrays.hashCode(objArr);
    }

    public static int hashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        return obj.hashCode();
    }

    public static String toString(Object obj) {
        return obj == null ? "null" : obj.toString();
    }
}
