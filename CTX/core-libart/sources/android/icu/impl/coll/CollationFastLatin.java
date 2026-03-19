package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.text.DateTimePatternGenerator;

public final class CollationFastLatin {
    static final boolean $assertionsDisabled = false;
    static final int BAIL_OUT = 1;
    public static final int BAIL_OUT_RESULT = -2;
    static final int CASE_AND_TERTIARY_MASK = 31;
    static final int CASE_MASK = 24;
    static final int COMMON_SEC = 160;
    static final int COMMON_SEC_PLUS_OFFSET = 192;
    static final int COMMON_TER = 0;
    static final int COMMON_TER_PLUS_OFFSET = 32;
    static final int CONTRACTION = 1024;
    static final int CONTR_CHAR_MASK = 511;
    static final int CONTR_LENGTH_SHIFT = 9;
    static final int EOS = 2;
    static final int EXPANSION = 2048;
    static final int INDEX_MASK = 1023;
    public static final int LATIN_LIMIT = 384;
    public static final int LATIN_MAX = 383;
    static final int LATIN_MAX_UTF8_LEAD = 197;
    static final int LONG_INC = 8;
    static final int LONG_PRIMARY_MASK = 65528;
    static final int LOWER_CASE = 8;
    static final int MAX_LONG = 4088;
    static final int MAX_SEC_AFTER = 352;
    static final int MAX_SEC_BEFORE = 128;
    static final int MAX_SEC_HIGH = 992;
    static final int MAX_SHORT = 64512;
    static final int MAX_TER_AFTER = 7;
    static final int MERGE_WEIGHT = 3;
    static final int MIN_LONG = 3072;
    static final int MIN_SEC_AFTER = 192;
    static final int MIN_SEC_BEFORE = 0;
    static final int MIN_SEC_HIGH = 384;
    static final int MIN_SHORT = 4096;
    static final int NUM_FAST_CHARS = 448;
    static final int PUNCT_LIMIT = 8256;
    static final int PUNCT_START = 8192;
    static final int SECONDARY_MASK = 992;
    static final int SEC_INC = 32;
    static final int SEC_OFFSET = 32;
    static final int SHORT_INC = 1024;
    static final int SHORT_PRIMARY_MASK = 64512;
    static final int TERTIARY_MASK = 7;
    static final int TER_OFFSET = 32;
    static final int TWO_CASES_MASK = 1572888;
    static final int TWO_COMMON_SEC_PLUS_OFFSET = 12583104;
    static final int TWO_COMMON_TER_PLUS_OFFSET = 2097184;
    static final int TWO_LONG_PRIMARIES_MASK = -458760;
    static final int TWO_LOWER_CASES = 524296;
    static final int TWO_SECONDARIES_MASK = 65012704;
    static final int TWO_SEC_OFFSETS = 2097184;
    static final int TWO_SHORT_PRIMARIES_MASK = -67044352;
    static final int TWO_TERTIARIES_MASK = 458759;
    static final int TWO_TER_OFFSETS = 2097184;
    public static final int VERSION = 2;

    static int getCharIndex(char c) {
        if (c <= 383) {
            return c;
        }
        if (8192 <= c && c < PUNCT_LIMIT) {
            return c - 7808;
        }
        return -1;
    }

    public static int getOptions(CollationData collationData, CollationSettings collationSettings, char[] cArr) {
        char c;
        boolean z;
        int i;
        char[] cArr2 = collationData.fastLatinTableHeader;
        if (cArr2 == null || cArr.length != 384) {
            return -1;
        }
        if ((collationSettings.options & 12) == 0) {
            c = 3071;
        } else {
            int i2 = cArr2[0] & 255;
            int maxVariable = collationSettings.getMaxVariable() + 1;
            if (maxVariable >= i2) {
                return -1;
            }
            c = cArr2[maxVariable];
        }
        if (collationSettings.hasReordering()) {
            long j = 0;
            long j2 = 0;
            long j3 = 0;
            long j4 = 0;
            for (int i3 = 4096; i3 < 4104; i3++) {
                long jReorder = collationSettings.reorder(collationData.getFirstPrimaryForGroup(i3));
                if (i3 == 4100) {
                    j4 = jReorder;
                    j3 = j;
                } else if (jReorder == 0) {
                    continue;
                } else {
                    if (jReorder < j) {
                        return -1;
                    }
                    if (j4 != 0 && j2 == 0 && j == j3) {
                        j2 = jReorder;
                    }
                    j = jReorder;
                }
            }
            long jReorder2 = collationSettings.reorder(collationData.getFirstPrimaryForGroup(25));
            if (jReorder2 < j) {
                return -1;
            }
            if (j2 == 0) {
                j2 = jReorder2;
            }
            z = j3 >= j4 || j4 >= j2;
        }
        char[] cArr3 = collationData.fastLatinTable;
        for (int i4 = 0; i4 < 384; i4++) {
            char c2 = cArr3[i4];
            if (c2 >= 4096) {
                i = 64512 & c2;
            } else if (c2 > c) {
                i = LONG_PRIMARY_MASK & c2;
            } else {
                i = 0;
            }
            cArr[i4] = (char) i;
        }
        if (z || (collationSettings.options & 2) != 0) {
            for (int i5 = 48; i5 <= 57; i5++) {
                cArr[i5] = 0;
            }
        }
        return (c << 16) | collationSettings.options;
    }

    public static int compareUTF16(char[] cArr, char[] cArr2, int i, CharSequence charSequence, CharSequence charSequence2, int i2) {
        int i3;
        int i4;
        int cases;
        int i5;
        int i6;
        int secondariesFromOneShortCE;
        int secondariesFromOneShortCE2;
        int iLookup;
        int iLookup2;
        int i7;
        int i8 = i >> 16;
        int i9 = i & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
        int i10 = i2;
        int i11 = i10;
        loop0: while (true) {
            int primaries = 0;
            int primaries2 = 0;
            while (true) {
                char c = 8192;
                char c2 = 383;
                if (primaries == 0) {
                    if (i10 == charSequence.length()) {
                        primaries = 2;
                    } else {
                        int i12 = i10 + 1;
                        char cCharAt = charSequence.charAt(i10);
                        if (cCharAt <= 383) {
                            char c3 = cArr2[cCharAt];
                            if (c3 != 0) {
                                i10 = i12;
                                primaries = c3;
                            } else {
                                if (cCharAt <= '9' && cCharAt >= '0' && (i9 & 2) != 0) {
                                    return -2;
                                }
                                iLookup2 = cArr[cCharAt];
                            }
                        } else {
                            iLookup2 = (8192 > cCharAt || cCharAt >= PUNCT_LIMIT) ? lookup(cArr, cCharAt) : cArr[(cCharAt - 8192) + 384];
                        }
                        if (iLookup2 >= 4096) {
                            i7 = 64512 & iLookup2;
                        } else if (iLookup2 > i8) {
                            i7 = LONG_PRIMARY_MASK & iLookup2;
                        } else {
                            long jNextPair = nextPair(cArr, cCharAt, iLookup2, charSequence, i12);
                            if (jNextPair < 0) {
                                i12++;
                                jNextPair = ~jNextPair;
                            }
                            i10 = i12;
                            int i13 = (int) jNextPair;
                            if (i13 == 1) {
                                return -2;
                            }
                            primaries = getPrimaries(i8, i13);
                        }
                        int i14 = i7;
                        i10 = i12;
                        primaries = i14;
                    }
                }
                while (true) {
                    if (primaries2 != 0) {
                        int i15 = primaries2;
                        i3 = i11;
                        i4 = i15;
                        break;
                    }
                    if (i11 == charSequence2.length()) {
                        i3 = i11;
                        i4 = 2;
                        break;
                    }
                    i3 = i11 + 1;
                    char cCharAt2 = charSequence2.charAt(i11);
                    if (cCharAt2 <= c2) {
                        char c4 = cArr2[cCharAt2];
                        if (c4 != 0) {
                            i4 = c4;
                            break;
                        }
                        if (cCharAt2 <= '9' && cCharAt2 >= '0' && (i9 & 2) != 0) {
                            return -2;
                        }
                        iLookup = cArr[cCharAt2];
                    } else {
                        iLookup = (c > cCharAt2 || cCharAt2 >= PUNCT_LIMIT) ? lookup(cArr, cCharAt2) : cArr[(cCharAt2 - 8192) + 384];
                    }
                    if (iLookup >= 4096) {
                        i4 = 64512 & iLookup;
                        break;
                    }
                    if (iLookup > i8) {
                        i4 = LONG_PRIMARY_MASK & iLookup;
                        break;
                    }
                    long jNextPair2 = nextPair(cArr, cCharAt2, iLookup, charSequence2, i3);
                    if (jNextPair2 < 0) {
                        i3++;
                        jNextPair2 = ~jNextPair2;
                    }
                    i11 = i3;
                    int i16 = (int) jNextPair2;
                    if (i16 == 1) {
                        return -2;
                    }
                    primaries2 = getPrimaries(i8, i16);
                    c2 = 383;
                    c = 8192;
                }
                if (primaries == i4) {
                    break;
                }
                int i17 = primaries & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                int i18 = i4 & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                if (i17 != i18) {
                    return i17 < i18 ? -1 : 1;
                }
                if (primaries == 2) {
                    break loop0;
                }
                primaries >>>= 16;
                int i19 = i4 >>> 16;
                i11 = i3;
                primaries2 = i19;
            }
            i11 = i3;
        }
        if (CollationSettings.getStrength(i9) >= 1) {
            int i20 = i2;
            int i21 = i20;
            loop3: while (true) {
                int secondaries = 0;
                int secondaries2 = 0;
                while (true) {
                    if (secondaries == 0) {
                        if (i20 == charSequence.length()) {
                            secondaries = 2;
                        } else {
                            int i22 = i20 + 1;
                            char cCharAt3 = charSequence.charAt(i20);
                            int iLookup3 = cCharAt3 <= 383 ? cArr[cCharAt3] : (8192 > cCharAt3 || cCharAt3 >= PUNCT_LIMIT) ? lookup(cArr, cCharAt3) : cArr[(cCharAt3 - 8192) + 384];
                            if (iLookup3 >= 4096) {
                                secondariesFromOneShortCE2 = getSecondariesFromOneShortCE(iLookup3);
                            } else if (iLookup3 > i8) {
                                secondariesFromOneShortCE2 = 192;
                            } else {
                                long jNextPair3 = nextPair(cArr, cCharAt3, iLookup3, charSequence, i22);
                                if (jNextPair3 < 0) {
                                    i22++;
                                    jNextPair3 = ~jNextPair3;
                                }
                                i20 = i22;
                                secondaries = getSecondaries(i8, (int) jNextPair3);
                            }
                            int i23 = secondariesFromOneShortCE2;
                            i20 = i22;
                            secondaries = i23;
                        }
                    }
                    while (true) {
                        if (secondaries2 != 0) {
                            i5 = i20;
                            int i24 = secondaries2;
                            i6 = i21;
                            secondariesFromOneShortCE = i24;
                            break;
                        }
                        if (i21 == charSequence2.length()) {
                            i5 = i20;
                            i6 = i21;
                            secondariesFromOneShortCE = 2;
                            break;
                        }
                        i6 = i21 + 1;
                        char cCharAt4 = charSequence2.charAt(i21);
                        int iLookup4 = cCharAt4 <= 383 ? cArr[cCharAt4] : (8192 > cCharAt4 || cCharAt4 >= PUNCT_LIMIT) ? lookup(cArr, cCharAt4) : cArr[(cCharAt4 - 8192) + 384];
                        if (iLookup4 >= 4096) {
                            secondariesFromOneShortCE = getSecondariesFromOneShortCE(iLookup4);
                            break;
                        }
                        if (iLookup4 > i8) {
                            secondariesFromOneShortCE = 192;
                            break;
                        }
                        int i25 = i20;
                        long jNextPair4 = nextPair(cArr, cCharAt4, iLookup4, charSequence2, i6);
                        if (jNextPair4 < 0) {
                            i6++;
                            jNextPair4 = ~jNextPair4;
                        }
                        i21 = i6;
                        secondaries2 = getSecondaries(i8, (int) jNextPair4);
                        i20 = i25;
                    }
                    if (secondaries == secondariesFromOneShortCE) {
                        break;
                    }
                    int i26 = secondaries & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    int i27 = secondariesFromOneShortCE & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    if (i26 != i27) {
                        if ((i9 & 2048) != 0) {
                            return -2;
                        }
                        return i26 < i27 ? -1 : 1;
                    }
                    if (secondaries == 2) {
                        break loop3;
                    }
                    secondaries >>>= 16;
                    int i28 = secondariesFromOneShortCE >>> 16;
                    i21 = i6;
                    i20 = i5;
                    secondaries2 = i28;
                }
                i21 = i6;
                i20 = i5;
            }
        }
        if ((i9 & 1024) != 0) {
            boolean z = CollationSettings.getStrength(i9) == 0;
            int i29 = i2;
            int i30 = i29;
            loop6: do {
                cases = 0;
                int cases2 = 0;
                while (true) {
                    if (cases == 0) {
                        if (i29 == charSequence.length()) {
                            cases = 2;
                        } else {
                            int i31 = i29 + 1;
                            char cCharAt5 = charSequence.charAt(i29);
                            int iLookup5 = cCharAt5 <= 383 ? cArr[cCharAt5] : lookup(cArr, cCharAt5);
                            if (iLookup5 < MIN_LONG) {
                                long jNextPair5 = nextPair(cArr, cCharAt5, iLookup5, charSequence, i31);
                                if (jNextPair5 < 0) {
                                    i31++;
                                    jNextPair5 = ~jNextPair5;
                                }
                                iLookup5 = (int) jNextPair5;
                            }
                            i29 = i31;
                            cases = getCases(i8, z, iLookup5);
                        }
                    }
                    while (true) {
                        if (cases2 != 0) {
                            break;
                        }
                        if (i30 == charSequence2.length()) {
                            cases2 = 2;
                            break;
                        }
                        int i32 = i30 + 1;
                        char cCharAt6 = charSequence2.charAt(i30);
                        int iLookup6 = cCharAt6 <= 383 ? cArr[cCharAt6] : lookup(cArr, cCharAt6);
                        if (iLookup6 < MIN_LONG) {
                            long jNextPair6 = nextPair(cArr, cCharAt6, iLookup6, charSequence2, i32);
                            if (jNextPair6 < 0) {
                                i32++;
                                jNextPair6 = ~jNextPair6;
                            }
                            iLookup6 = (int) jNextPair6;
                        }
                        i30 = i32;
                        cases2 = getCases(i8, z, iLookup6);
                    }
                    if (cases == cases2) {
                        break;
                    }
                    int i33 = cases & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    int i34 = cases2 & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                    if (i33 != i34) {
                        return (i9 & 256) == 0 ? i33 < i34 ? -1 : 1 : i33 < i34 ? 1 : -1;
                    }
                    if (cases == 2) {
                        break loop6;
                    }
                    cases >>>= 16;
                    cases2 >>>= 16;
                }
            } while (cases != 2);
        }
        if (CollationSettings.getStrength(i9) <= 1) {
            return 0;
        }
        boolean zIsTertiaryWithCaseBits = CollationSettings.isTertiaryWithCaseBits(i9);
        int i35 = i2;
        int i36 = i35;
        loop9: while (true) {
            int tertiaries = 0;
            int tertiaries2 = 0;
            while (true) {
                if (tertiaries == 0) {
                    if (i35 == charSequence.length()) {
                        tertiaries = 2;
                    } else {
                        int i37 = i35 + 1;
                        char cCharAt7 = charSequence.charAt(i35);
                        int iLookup7 = cCharAt7 <= 383 ? cArr[cCharAt7] : lookup(cArr, cCharAt7);
                        if (iLookup7 < MIN_LONG) {
                            long jNextPair7 = nextPair(cArr, cCharAt7, iLookup7, charSequence, i37);
                            if (jNextPair7 < 0) {
                                i37++;
                                jNextPair7 = ~jNextPair7;
                            }
                            iLookup7 = (int) jNextPair7;
                        }
                        i35 = i37;
                        tertiaries = getTertiaries(i8, zIsTertiaryWithCaseBits, iLookup7);
                    }
                }
                while (true) {
                    if (tertiaries2 != 0) {
                        break;
                    }
                    if (i36 == charSequence2.length()) {
                        tertiaries2 = 2;
                        break;
                    }
                    int i38 = i36 + 1;
                    char cCharAt8 = charSequence2.charAt(i36);
                    int iLookup8 = cCharAt8 <= 383 ? cArr[cCharAt8] : lookup(cArr, cCharAt8);
                    if (iLookup8 < MIN_LONG) {
                        long jNextPair8 = nextPair(cArr, cCharAt8, iLookup8, charSequence2, i38);
                        if (jNextPair8 < 0) {
                            i38++;
                            jNextPair8 = ~jNextPair8;
                        }
                        iLookup8 = (int) jNextPair8;
                    }
                    i36 = i38;
                    tertiaries2 = getTertiaries(i8, zIsTertiaryWithCaseBits, iLookup8);
                }
                if (tertiaries == tertiaries2) {
                    break;
                }
                int i39 = tertiaries & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                int i40 = tertiaries2 & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                if (i39 != i40) {
                    if (CollationSettings.sortsTertiaryUpperCaseFirst(i9)) {
                        if (i39 > 3) {
                            i39 ^= 24;
                        }
                        if (i40 > 3) {
                            i40 ^= 24;
                        }
                    }
                    return i39 < i40 ? -1 : 1;
                }
                int i41 = 2;
                if (tertiaries == 2) {
                    break loop9;
                }
                tertiaries >>>= 16;
                tertiaries2 >>>= 16;
            }
        }
    }

    private static int lookup(char[] cArr, int i) {
        if (8192 <= i && i < PUNCT_LIMIT) {
            return cArr[(i - 8192) + 384];
        }
        if (i == 65534) {
            return 3;
        }
        if (i == 65535) {
            return 64680;
        }
        return 1;
    }

    private static long nextPair(char[] cArr, int i, int i2, CharSequence charSequence, int i3) {
        int i4;
        long j;
        int i5;
        if (i2 >= MIN_LONG || i2 < 1024) {
            return i2;
        }
        if (i2 >= 2048) {
            int i6 = NUM_FAST_CHARS + (i2 & 1023);
            return (((long) cArr[i6 + 1]) << 16) | ((long) cArr[i6]);
        }
        int i7 = NUM_FAST_CHARS + (i2 & 1023);
        boolean z = false;
        if (i3 == charSequence.length()) {
            i4 = i7;
        } else {
            int iCharAt = charSequence.charAt(i3);
            if (iCharAt > 383) {
                if (8192 <= iCharAt && iCharAt < PUNCT_LIMIT) {
                    iCharAt = (iCharAt - 8192) + 384;
                } else {
                    if (iCharAt != 65534 && iCharAt != 65535) {
                        return 1L;
                    }
                    iCharAt = -1;
                }
            }
            char c = cArr[i7];
            i4 = i7;
            do {
                i4 += c >> '\t';
                c = cArr[i4];
                i5 = c & 511;
            } while (i5 < iCharAt);
            if (i5 == iCharAt) {
                z = true;
            }
        }
        int i8 = cArr[i4] >> '\t';
        if (i8 == 1) {
            return 1L;
        }
        char c2 = cArr[i4 + 1];
        if (i8 == 2) {
            j = c2;
        } else {
            j = (((long) cArr[i4 + 2]) << 16) | ((long) c2);
        }
        return z ? ~j : j;
    }

    private static int getPrimaries(int i, int i2) {
        int i3 = 65535 & i2;
        if (i3 >= 4096) {
            return TWO_SHORT_PRIMARIES_MASK & i2;
        }
        if (i3 > i) {
            return TWO_LONG_PRIMARIES_MASK & i2;
        }
        if (i3 >= MIN_LONG) {
            return 0;
        }
        return i2;
    }

    private static int getSecondariesFromOneShortCE(int i) {
        int i2 = i & 992;
        if (i2 < 384) {
            return i2 + 32;
        }
        return ((i2 + 32) << 16) | 192;
    }

    private static int getSecondaries(int i, int i2) {
        if (i2 <= 65535) {
            if (i2 >= 4096) {
                return getSecondariesFromOneShortCE(i2);
            }
            if (i2 > i) {
                return 192;
            }
            if (i2 < MIN_LONG) {
                return i2;
            }
        } else {
            int i3 = 65535 & i2;
            if (i3 >= 4096) {
                return 2097184 + (TWO_SECONDARIES_MASK & i2);
            }
            if (i3 > i) {
                return TWO_COMMON_SEC_PLUS_OFFSET;
            }
        }
        return 0;
    }

    private static int getCases(int i, boolean z, int i2) {
        if (i2 <= 65535) {
            if (i2 >= 4096) {
                int i3 = i2 & 24;
                if (!z && (i2 & 992) >= 384) {
                    i3 |= 524288;
                }
                return i3;
            }
            if (i2 > i) {
                return 8;
            }
            if (i2 < MIN_LONG) {
                return i2;
            }
        } else {
            int i4 = 65535 & i2;
            if (i4 >= 4096) {
                if (z && ((-67108864) & i2) == 0) {
                    return i2 & 24;
                }
                return i2 & TWO_CASES_MASK;
            }
            if (i4 > i) {
                return TWO_LOWER_CASES;
            }
        }
        return 0;
    }

    private static int getTertiaries(int i, boolean z, int i2) {
        int i3;
        int i4;
        int i5;
        if (i2 <= 65535) {
            if (i2 >= 4096) {
                if (z) {
                    i4 = (i2 & 31) + 32;
                    if ((i2 & 992) >= 384) {
                        i5 = 2621440 | i4;
                        return i5;
                    }
                    return i4;
                }
                i4 = (i2 & 7) + 32;
                if ((i2 & 992) >= 384) {
                    i5 = 2097152 | i4;
                    return i5;
                }
                return i4;
            }
            if (i2 > i) {
                int i6 = (i2 & 7) + 32;
                if (z) {
                    return i6 | 8;
                }
                return i6;
            }
            if (i2 < MIN_LONG) {
                return i2;
            }
        } else {
            int i7 = 65535 & i2;
            if (i7 >= 4096) {
                if (z) {
                    i3 = 2031647 & i2;
                } else {
                    i3 = i2 & TWO_TERTIARIES_MASK;
                }
                return i3 + 2097184;
            }
            if (i7 > i) {
                int i8 = (i2 & TWO_TERTIARIES_MASK) + 2097184;
                if (z) {
                    return i8 | TWO_LOWER_CASES;
                }
                return i8;
            }
        }
        return 0;
    }

    private static int getQuaternaries(int i, int i2) {
        if (i2 <= 65535) {
            if (i2 < 4096) {
                if (i2 <= i) {
                    if (i2 >= MIN_LONG) {
                        return i2 & LONG_PRIMARY_MASK;
                    }
                    return i2;
                }
            }
            return Normalizer2Impl.MIN_NORMAL_MAYBE_YES;
        }
        if ((i2 & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) <= i) {
            return i2 & TWO_LONG_PRIMARIES_MASK;
        }
        return TWO_SHORT_PRIMARIES_MASK;
    }

    private CollationFastLatin() {
    }
}
