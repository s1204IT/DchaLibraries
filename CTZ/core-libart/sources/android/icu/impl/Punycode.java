package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.text.StringPrepParseException;
import android.icu.text.UTF16;

public final class Punycode {
    private static final int BASE = 36;
    private static final int CAPITAL_A = 65;
    private static final int CAPITAL_Z = 90;
    private static final int DAMP = 700;
    private static final char DELIMITER = '-';
    private static final char HYPHEN = '-';
    private static final int INITIAL_BIAS = 72;
    private static final int INITIAL_N = 128;
    private static final int SKEW = 38;
    private static final int SMALL_A = 97;
    private static final int SMALL_Z = 122;
    private static final int TMAX = 26;
    private static final int TMIN = 1;
    private static final int ZERO = 48;
    static final int[] basicToDigit = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    private static int adaptBias(int i, int i2, boolean z) {
        int i3;
        if (z) {
            i3 = i / DAMP;
        } else {
            i3 = i / 2;
        }
        int i4 = i3 + (i3 / i2);
        int i5 = 0;
        while (i4 > 455) {
            i4 /= 35;
            i5 += 36;
        }
        return i5 + ((36 * i4) / (i4 + 38));
    }

    private static char asciiCaseMap(char c, boolean z) {
        if (z) {
            if ('a' <= c && c <= 'z') {
                return (char) (c - ' ');
            }
            return c;
        }
        if ('A' <= c && c <= 'Z') {
            return (char) (c + ' ');
        }
        return c;
    }

    private static char digitToBasic(int i, boolean z) {
        if (i < 26) {
            if (z) {
                return (char) (65 + i);
            }
            return (char) (97 + i);
        }
        return (char) (22 + i);
    }

    public static StringBuilder encode(CharSequence charSequence, boolean[] zArr) throws StringPrepParseException {
        int codePoint;
        int i;
        int length = charSequence.length();
        int[] iArr = new int[length];
        StringBuilder sb = new StringBuilder(length);
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int i4 = 1;
            if (i2 < length) {
                char cCharAt = charSequence.charAt(i2);
                if (isBasic(cCharAt)) {
                    i = i3 + 1;
                    iArr[i3] = 0;
                    if (zArr != null) {
                        cCharAt = asciiCaseMap(cCharAt, zArr[i2]);
                    }
                    sb.append(cCharAt);
                } else {
                    int i5 = ((zArr == null || !zArr[i2]) ? 0 : 1) << 31;
                    if (!UTF16.isSurrogate(cCharAt)) {
                        codePoint = cCharAt | i5;
                    } else {
                        if (!UTF16.isLeadSurrogate(cCharAt) || (i2 = i2 + 1) >= length) {
                            break;
                        }
                        char cCharAt2 = charSequence.charAt(i2);
                        if (!UTF16.isTrailSurrogate(cCharAt2)) {
                            break;
                        }
                        codePoint = UCharacter.getCodePoint(cCharAt, cCharAt2) | i5;
                    }
                    i = i3 + 1;
                    iArr[i3] = codePoint;
                }
                i3 = i;
                i2++;
            } else {
                int length2 = sb.length();
                if (length2 > 0) {
                    sb.append('-');
                }
                int iAdaptBias = 72;
                int i6 = 0;
                int i7 = 128;
                int i8 = length2;
                while (i8 < i3) {
                    int i9 = Integer.MAX_VALUE;
                    for (int i10 = 0; i10 < i3; i10++) {
                        int i11 = iArr[i10] & Integer.MAX_VALUE;
                        if (i7 <= i11 && i11 < i9) {
                            i9 = i11;
                        }
                    }
                    int i12 = i9 - i7;
                    int i13 = i8 + 1;
                    if (i12 > (Integer.MAX_VALUE - i6) / i13) {
                        throw new IllegalStateException("Internal program error");
                    }
                    int i14 = i6 + (i12 * i13);
                    int i15 = i8;
                    int i16 = 0;
                    while (i16 < i3) {
                        int i17 = iArr[i16] & Integer.MAX_VALUE;
                        if (i17 < i9) {
                            i14++;
                        } else if (i17 == i9) {
                            int i18 = i14;
                            int i19 = 36;
                            while (true) {
                                int i20 = i19 - iAdaptBias;
                                if (i20 >= i4) {
                                    i4 = i19 >= iAdaptBias + 26 ? 26 : i20;
                                }
                                if (i18 < i4) {
                                    break;
                                }
                                int i21 = i18 - i4;
                                int i22 = 36 - i4;
                                sb.append(digitToBasic(i4 + (i21 % i22), false));
                                i18 = i21 / i22;
                                i19 += 36;
                                i4 = 1;
                            }
                            sb.append(digitToBasic(i18, iArr[i16] < 0));
                            int i23 = i15 + 1;
                            iAdaptBias = adaptBias(i14, i23, i15 == length2);
                            i14 = 0;
                            i15 = i23;
                        }
                        i16++;
                        i4 = 1;
                    }
                    i6 = i14 + 1;
                    i4 = 1;
                    int i24 = i15;
                    i7 = i9 + 1;
                    i8 = i24;
                }
                return sb;
            }
        }
        throw new StringPrepParseException("Illegal char found", 1);
    }

    private static boolean isBasic(int i) {
        return i < 128;
    }

    private static boolean isBasicUpperCase(int i) {
        return 65 <= i && i >= 90;
    }

    private static boolean isSurrogate(int i) {
        return (i & (-2048)) == 55296;
    }

    public static StringBuilder decode(CharSequence charSequence, boolean[] zArr) throws StringPrepParseException {
        int iOffsetByCodePoints;
        int length = charSequence.length();
        StringBuilder sb = new StringBuilder(charSequence.length());
        int i = length;
        while (i > 0) {
            i--;
            if (charSequence.charAt(i) == '-') {
                break;
            }
        }
        for (int i2 = 0; i2 < i; i2++) {
            char cCharAt = charSequence.charAt(i2);
            if (!isBasic(cCharAt)) {
                throw new StringPrepParseException("Illegal char found", 0);
            }
            sb.append(cCharAt);
            if (zArr != null && i2 < zArr.length) {
                zArr[i2] = isBasicUpperCase(cCharAt);
            }
        }
        int iAdaptBias = 72;
        int i3 = i > 0 ? i + 1 : 0;
        int i4 = 1000000000;
        int i5 = 128;
        int i6 = i;
        int i7 = 0;
        while (i3 < length) {
            int i8 = i7;
            int i9 = 1;
            int i10 = 36;
            while (i3 < length) {
                int i11 = i3 + 1;
                int i12 = basicToDigit[charSequence.charAt(i3) & 255];
                if (i12 < 0) {
                    throw new StringPrepParseException("Invalid char found", 0);
                }
                if (i12 > (Integer.MAX_VALUE - i8) / i9) {
                    throw new StringPrepParseException("Illegal char found", 1);
                }
                i8 += i12 * i9;
                int i13 = i10 - iAdaptBias;
                int i14 = i13 < 1 ? 1 : i10 >= iAdaptBias + 26 ? 26 : i13;
                if (i12 >= i14) {
                    int i15 = 36 - i14;
                    if (i9 > Integer.MAX_VALUE / i15) {
                        throw new StringPrepParseException("Illegal char found", 1);
                    }
                    i9 *= i15;
                    i10 += 36;
                    i3 = i11;
                } else {
                    i6++;
                    iAdaptBias = adaptBias(i8 - i7, i6, i7 == 0);
                    int i16 = i8 / i6;
                    if (i16 > Integer.MAX_VALUE - i5) {
                        throw new StringPrepParseException("Illegal char found", 1);
                    }
                    i5 += i16;
                    int i17 = i8 % i6;
                    if (i5 > 1114111 || isSurrogate(i5)) {
                        throw new StringPrepParseException("Illegal char found", 1);
                    }
                    int iCharCount = Character.charCount(i5);
                    if (i17 <= i4) {
                        if (iCharCount <= 1) {
                            i4++;
                            iOffsetByCodePoints = i17;
                        } else {
                            iOffsetByCodePoints = i17;
                            i4 = iOffsetByCodePoints;
                        }
                    } else {
                        iOffsetByCodePoints = sb.offsetByCodePoints(i4, i17 - i4);
                    }
                    if (zArr != null && sb.length() + iCharCount <= zArr.length) {
                        if (iOffsetByCodePoints < sb.length()) {
                            System.arraycopy(zArr, iOffsetByCodePoints, zArr, iOffsetByCodePoints + iCharCount, sb.length() - iOffsetByCodePoints);
                        }
                        zArr[iOffsetByCodePoints] = isBasicUpperCase(charSequence.charAt(i11 - 1));
                        if (iCharCount == 2) {
                            zArr[iOffsetByCodePoints + 1] = false;
                        }
                    }
                    if (iCharCount == 1) {
                        sb.insert(iOffsetByCodePoints, (char) i5);
                    } else {
                        sb.insert(iOffsetByCodePoints, UTF16.getLeadSurrogate(i5));
                        sb.insert(iOffsetByCodePoints + 1, UTF16.getTrailSurrogate(i5));
                    }
                    i7 = i17 + 1;
                    i3 = i11;
                }
            }
            throw new StringPrepParseException("Illegal char found", 1);
        }
        return sb;
    }
}
