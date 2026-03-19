package android.icu.impl.coll;

public final class CollationFastLatin {

    static final boolean f42assertionsDisabled;
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

    static {
        f42assertionsDisabled = !CollationFastLatin.class.desiredAssertionStatus();
    }

    static int getCharIndex(char c) {
        if (c <= 383) {
            return c;
        }
        if (8192 <= c && c < PUNCT_LIMIT) {
            return c - 7808;
        }
        return -1;
    }

    public static int getOptions(CollationData data, CollationSettings settings, char[] primaries) {
        char c;
        int p;
        char[] header = data.fastLatinTableHeader;
        if (header == null) {
            return -1;
        }
        if (!f42assertionsDisabled) {
            if (!((header[0] >> '\b') == 2)) {
                throw new AssertionError();
            }
        }
        if (!f42assertionsDisabled) {
            if (!(primaries.length == 384)) {
                throw new AssertionError();
            }
        }
        if (primaries.length != 384) {
            return -1;
        }
        if ((settings.options & 12) == 0) {
            c = 3071;
        } else {
            int headerLength = header[0] & 255;
            int i = settings.getMaxVariable() + 1;
            if (i >= headerLength) {
                return -1;
            }
            c = header[i];
        }
        boolean digitsAreReordered = false;
        if (settings.hasReordering()) {
            long prevStart = 0;
            long beforeDigitStart = 0;
            long digitStart = 0;
            long afterDigitStart = 0;
            for (int group = 4096; group < 4104; group++) {
                long start = settings.reorder(data.getFirstPrimaryForGroup(group));
                if (group == 4100) {
                    beforeDigitStart = prevStart;
                    digitStart = start;
                } else if (start == 0) {
                    continue;
                } else {
                    if (start < prevStart) {
                        return -1;
                    }
                    if (digitStart != 0 && afterDigitStart == 0 && prevStart == beforeDigitStart) {
                        afterDigitStart = start;
                    }
                    prevStart = start;
                }
            }
            long latinStart = settings.reorder(data.getFirstPrimaryForGroup(25));
            if (latinStart < prevStart) {
                return -1;
            }
            if (afterDigitStart == 0) {
                afterDigitStart = latinStart;
            }
            if (beforeDigitStart >= digitStart || digitStart >= afterDigitStart) {
                digitsAreReordered = true;
            }
        }
        char[] table = data.fastLatinTable;
        for (int c2 = 0; c2 < 384; c2++) {
            char c3 = table[c2];
            if (c3 >= 4096) {
                p = c3 & 64512;
            } else if (c3 > c) {
                p = c3 & LONG_PRIMARY_MASK;
            } else {
                p = 0;
            }
            primaries[c2] = (char) p;
        }
        if (digitsAreReordered || (settings.options & 2) != 0) {
            for (int c4 = 48; c4 <= 57; c4++) {
                primaries[c4] = 0;
            }
        }
        return (c << 16) | settings.options;
    }

    public static int compareUTF16(char[] cArr, char[] cArr2, int i, CharSequence charSequence, CharSequence charSequence2, int i2) {
        int i3;
        int i4;
        int i5;
        int iLookup;
        int iLookup2;
        int i6 = i >> 16;
        int i7 = i & 65535;
        int i8 = i2;
        int i9 = 0;
        char primaries = 0;
        int i10 = i2;
        while (true) {
            if (i9 != 0) {
                i3 = i8;
                i4 = i10;
                i5 = i9;
            } else if (i10 == charSequence.length()) {
                i5 = 2;
                i3 = i8;
                i4 = i10;
            } else {
                i4 = i10 + 1;
                char cCharAt = charSequence.charAt(i10);
                if (cCharAt <= 383) {
                    char c = cArr2[cCharAt];
                    if (c != 0) {
                        i3 = i8;
                        i5 = c;
                    } else {
                        if (cCharAt <= '9' && cCharAt >= '0' && (i7 & 2) != 0) {
                            return -2;
                        }
                        iLookup2 = cArr[cCharAt];
                    }
                } else if (8192 <= cCharAt && cCharAt < PUNCT_LIMIT) {
                    iLookup2 = cArr[(cCharAt - 8192) + 384];
                } else {
                    iLookup2 = lookup(cArr, cCharAt);
                }
                if (iLookup2 >= 4096) {
                    i3 = i8;
                    i5 = iLookup2 & 64512;
                } else if (iLookup2 > i6) {
                    i3 = i8;
                    i5 = iLookup2 & LONG_PRIMARY_MASK;
                } else {
                    long jNextPair = nextPair(cArr, cCharAt, iLookup2, charSequence, i4);
                    if (jNextPair < 0) {
                        i4++;
                        jNextPair = ~jNextPair;
                    }
                    int i11 = (int) jNextPair;
                    if (i11 == 1) {
                        return -2;
                    }
                    int primaries2 = getPrimaries(i6, i11);
                    i10 = i4;
                    i9 = primaries2;
                    primaries = primaries;
                }
            }
            while (true) {
                if (primaries != 0) {
                    i8 = i3;
                    break;
                }
                if (i3 == charSequence2.length()) {
                    primaries = 2;
                    i8 = i3;
                    break;
                }
                i8 = i3 + 1;
                char cCharAt2 = charSequence2.charAt(i3);
                if (cCharAt2 <= 383) {
                    primaries = cArr2[cCharAt2];
                    if (primaries != 0) {
                        break;
                    }
                    if (cCharAt2 <= '9' && cCharAt2 >= '0' && (i7 & 2) != 0) {
                        return -2;
                    }
                    iLookup = cArr[cCharAt2];
                } else if (8192 <= cCharAt2 && cCharAt2 < PUNCT_LIMIT) {
                    iLookup = cArr[(cCharAt2 - 8192) + 384];
                } else {
                    iLookup = lookup(cArr, cCharAt2);
                }
                if (iLookup >= 4096) {
                    primaries = iLookup & 64512;
                    break;
                }
                if (iLookup > i6) {
                    primaries = iLookup & LONG_PRIMARY_MASK;
                    break;
                }
                long jNextPair2 = nextPair(cArr, cCharAt2, iLookup == true ? 1 : 0, charSequence2, i8);
                if (jNextPair2 < 0) {
                    i8++;
                    jNextPair2 = ~jNextPair2;
                }
                int i12 = (int) jNextPair2;
                if (i12 == 1) {
                    return -2;
                }
                primaries = getPrimaries(i6, i12);
                i3 = i8;
            }
        }
    }

    private static int lookup(char[] table, int c) {
        if (!f42assertionsDisabled) {
            if (!(c > 383)) {
                throw new AssertionError();
            }
        }
        if (8192 <= c && c < PUNCT_LIMIT) {
            return table[(c - 8192) + 384];
        }
        if (c == 65534) {
            return 3;
        }
        return c == 65535 ? 64680 : 1;
    }

    private static long nextPair(char[] table, int c, int ce, CharSequence s16, int sIndex) {
        long result;
        int x;
        if (ce >= MIN_LONG || ce < 1024) {
            return ce;
        }
        if (ce >= 2048) {
            int index = (ce & 1023) + NUM_FAST_CHARS;
            return (((long) table[index + 1]) << 16) | ((long) table[index]);
        }
        int index2 = (ce & 1023) + NUM_FAST_CHARS;
        boolean inc = false;
        if (sIndex != s16.length()) {
            int nextIndex = sIndex + 1;
            int c2 = s16.charAt(sIndex);
            if (c2 > 383) {
                if (8192 <= c2 && c2 < PUNCT_LIMIT) {
                    c2 = (c2 - 8192) + 384;
                } else if (c2 == 65534 || c2 == 65535) {
                    c2 = -1;
                } else {
                    return 1L;
                }
            }
            int i = index2;
            char c3 = table[index2];
            do {
                i += c3 >> '\t';
                c3 = table[i];
                x = c3 & 511;
            } while (x < c2);
            if (x == c2) {
                index2 = i;
                inc = true;
            }
        }
        int length = table[index2] >> '\t';
        if (length == 1) {
            return 1L;
        }
        char c4 = table[index2 + 1];
        if (length == 2) {
            result = c4;
        } else {
            result = (((long) table[index2 + 2]) << 16) | ((long) c4);
        }
        return inc ? ~result : result;
    }

    private static int getPrimaries(int variableTop, int pair) {
        int ce = pair & 65535;
        if (ce >= 4096) {
            return TWO_SHORT_PRIMARIES_MASK & pair;
        }
        if (ce > variableTop) {
            return TWO_LONG_PRIMARIES_MASK & pair;
        }
        if (ce >= MIN_LONG) {
            return 0;
        }
        return pair;
    }

    private static int getSecondariesFromOneShortCE(int ce) {
        int ce2 = ce & 992;
        if (ce2 < 384) {
            return ce2 + 32;
        }
        return ((ce2 + 32) << 16) | 192;
    }

    private static int getSecondaries(int variableTop, int pair) {
        if (pair <= 65535) {
            if (pair >= 4096) {
                return getSecondariesFromOneShortCE(pair);
            }
            if (pair > variableTop) {
                return 192;
            }
            if (pair >= MIN_LONG) {
                return 0;
            }
            return pair;
        }
        int ce = pair & 65535;
        if (ce >= 4096) {
            return (TWO_SECONDARIES_MASK & pair) + 2097184;
        }
        if (ce > variableTop) {
            return TWO_COMMON_SEC_PLUS_OFFSET;
        }
        if (!f42assertionsDisabled) {
            if (!(ce >= MIN_LONG)) {
                throw new AssertionError();
            }
        }
        return 0;
    }

    private static int getCases(int variableTop, boolean strengthIsPrimary, int pair) {
        if (pair <= 65535) {
            if (pair >= 4096) {
                int pair2 = pair & 24;
                if (!strengthIsPrimary && (pair & 992) >= 384) {
                    return pair2 | 524288;
                }
                return pair2;
            }
            if (pair > variableTop) {
                return 8;
            }
            if (pair >= MIN_LONG) {
                return 0;
            }
            return pair;
        }
        int ce = pair & 65535;
        if (ce >= 4096) {
            if (strengthIsPrimary && ((-67108864) & pair) == 0) {
                return pair & 24;
            }
            return pair & TWO_CASES_MASK;
        }
        if (ce > variableTop) {
            return TWO_LOWER_CASES;
        }
        if (!f42assertionsDisabled) {
            if (!(ce >= MIN_LONG)) {
                throw new AssertionError();
            }
        }
        return 0;
    }

    private static int getTertiaries(int variableTop, boolean withCaseBits, int pair) {
        int pair2;
        if (pair <= 65535) {
            if (pair >= 4096) {
                if (withCaseBits) {
                    int pair3 = (pair & 31) + 32;
                    if ((pair & 992) >= 384) {
                        return pair3 | 2621440;
                    }
                    return pair3;
                }
                int pair4 = (pair & 7) + 32;
                if ((pair & 992) >= 384) {
                    return pair4 | 2097152;
                }
                return pair4;
            }
            if (pair > variableTop) {
                int pair5 = (pair & 7) + 32;
                if (withCaseBits) {
                    return pair5 | 8;
                }
                return pair5;
            }
            if (pair >= MIN_LONG) {
                return 0;
            }
            return pair;
        }
        int ce = pair & 65535;
        if (ce >= 4096) {
            if (withCaseBits) {
                pair2 = pair & 2031647;
            } else {
                pair2 = pair & TWO_TERTIARIES_MASK;
            }
            return pair2 + 2097184;
        }
        if (ce > variableTop) {
            int pair6 = (pair & TWO_TERTIARIES_MASK) + 2097184;
            if (withCaseBits) {
                return pair6 | TWO_LOWER_CASES;
            }
            return pair6;
        }
        if (!f42assertionsDisabled) {
            if (!(ce >= MIN_LONG)) {
                throw new AssertionError();
            }
        }
        return 0;
    }

    private static int getQuaternaries(int variableTop, int pair) {
        if (pair <= 65535) {
            if (pair >= 4096) {
                if ((pair & 992) >= 384) {
                    return TWO_SHORT_PRIMARIES_MASK;
                }
                return 64512;
            }
            if (pair > variableTop) {
                return 64512;
            }
            if (pair >= MIN_LONG) {
                return pair & LONG_PRIMARY_MASK;
            }
            return pair;
        }
        int ce = pair & 65535;
        if (ce > variableTop) {
            return TWO_SHORT_PRIMARIES_MASK;
        }
        if (!f42assertionsDisabled) {
            if (!(ce >= MIN_LONG)) {
                throw new AssertionError();
            }
        }
        return pair & TWO_LONG_PRIMARIES_MASK;
    }

    private CollationFastLatin() {
    }
}
