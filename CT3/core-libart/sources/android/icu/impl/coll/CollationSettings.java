package android.icu.impl.coll;

import java.util.Arrays;

public final class CollationSettings extends SharedObject {

    static final boolean f50assertionsDisabled;
    static final int ALTERNATE_MASK = 12;
    public static final int BACKWARD_SECONDARY = 2048;
    public static final int CASE_FIRST = 512;
    public static final int CASE_FIRST_AND_UPPER_MASK = 768;
    public static final int CASE_LEVEL = 1024;
    public static final int CHECK_FCD = 1;
    private static final int[] EMPTY_INT_ARRAY;
    static final int MAX_VARIABLE_MASK = 112;
    static final int MAX_VARIABLE_SHIFT = 4;
    static final int MAX_VAR_CURRENCY = 3;
    static final int MAX_VAR_PUNCT = 1;
    static final int MAX_VAR_SPACE = 0;
    static final int MAX_VAR_SYMBOL = 2;
    public static final int NUMERIC = 2;
    static final int SHIFTED = 4;
    static final int STRENGTH_MASK = 61440;
    static final int STRENGTH_SHIFT = 12;
    static final int UPPER_FIRST = 256;
    long minHighNoReorder;
    long[] reorderRanges;
    public byte[] reorderTable;
    public long variableTop;
    public int options = 8208;
    public int[] reorderCodes = EMPTY_INT_ARRAY;
    public int fastLatinOptions = -1;
    public char[] fastLatinPrimaries = new char[CollationFastLatin.LATIN_LIMIT];

    CollationSettings() {
    }

    @Override
    public CollationSettings m76clone() {
        CollationSettings newSettings = (CollationSettings) super.m76clone();
        newSettings.fastLatinPrimaries = (char[]) this.fastLatinPrimaries.clone();
        return newSettings;
    }

    public boolean equals(Object other) {
        if (other == null || !getClass().equals(other.getClass())) {
            return false;
        }
        CollationSettings o = (CollationSettings) other;
        if (this.options != o.options) {
            return false;
        }
        return ((this.options & 12) == 0 || this.variableTop == o.variableTop) && Arrays.equals(this.reorderCodes, o.reorderCodes);
    }

    public int hashCode() {
        int h = this.options << 8;
        if ((this.options & 12) != 0) {
            h = (int) (((long) h) ^ this.variableTop);
        }
        int h2 = h ^ this.reorderCodes.length;
        for (int i = 0; i < this.reorderCodes.length; i++) {
            h2 ^= this.reorderCodes[i] << i;
        }
        return h2;
    }

    public void resetReordering() {
        this.reorderTable = null;
        this.minHighNoReorder = 0L;
        this.reorderRanges = null;
        this.reorderCodes = EMPTY_INT_ARRAY;
    }

    void aliasReordering(CollationData data, int[] codesAndRanges, int codesLength, byte[] table) {
        int[] codes;
        if (codesLength == codesAndRanges.length) {
            codes = codesAndRanges;
        } else {
            codes = new int[codesLength];
            System.arraycopy(codesAndRanges, 0, codes, 0, codesLength);
        }
        int rangesLimit = codesAndRanges.length;
        int rangesLength = rangesLimit - codesLength;
        if (table != null && (rangesLength != 0 ? !(rangesLength < 2 || (codesAndRanges[codesLength] & 65535) != 0 || (codesAndRanges[rangesLimit - 1] & 65535) == 0) : !reorderTableHasSplitBytes(table))) {
            this.reorderTable = table;
            this.reorderCodes = codes;
            int firstSplitByteRangeIndex = codesLength;
            while (firstSplitByteRangeIndex < rangesLimit && (codesAndRanges[firstSplitByteRangeIndex] & 16711680) == 0) {
                firstSplitByteRangeIndex++;
            }
            if (firstSplitByteRangeIndex == rangesLimit) {
                if (!f50assertionsDisabled) {
                    if (!(reorderTableHasSplitBytes(table) ? false : true)) {
                        throw new AssertionError();
                    }
                }
                this.minHighNoReorder = 0L;
                this.reorderRanges = null;
                return;
            }
            if (!f50assertionsDisabled) {
                if (!(table[codesAndRanges[firstSplitByteRangeIndex] >>> 24] == 0)) {
                    throw new AssertionError();
                }
            }
            this.minHighNoReorder = ((long) codesAndRanges[rangesLimit - 1]) & Collation.MAX_PRIMARY;
            setReorderRanges(codesAndRanges, firstSplitByteRangeIndex, rangesLimit - firstSplitByteRangeIndex);
            return;
        }
        setReordering(data, codes);
    }

    public void setReordering(CollationData data, int[] codes) {
        int rangesStart;
        int rangesLength;
        if (codes.length == 0 || (codes.length == 1 && codes[0] == 103)) {
            resetReordering();
            return;
        }
        UVector32 rangesList = new UVector32();
        data.makeReorderRanges(codes, rangesList);
        int rangesLength2 = rangesList.size();
        if (rangesLength2 == 0) {
            resetReordering();
            return;
        }
        int[] ranges = rangesList.getBuffer();
        if (!f50assertionsDisabled) {
            if (!(rangesLength2 >= 2)) {
                throw new AssertionError();
            }
        }
        if (!f50assertionsDisabled) {
            if (!((ranges[0] & 65535) == 0 && (ranges[rangesLength2 + (-1)] & 65535) != 0)) {
                throw new AssertionError();
            }
        }
        this.minHighNoReorder = ((long) ranges[rangesLength2 - 1]) & Collation.MAX_PRIMARY;
        byte[] table = new byte[256];
        int b = 0;
        int firstSplitByteRangeIndex = -1;
        for (int i = 0; i < rangesLength2; i++) {
            int pair = ranges[i];
            int limit1 = pair >>> 24;
            while (b < limit1) {
                table[b] = (byte) (b + pair);
                b++;
            }
            if ((16711680 & pair) != 0) {
                table[limit1] = 0;
                b = limit1 + 1;
                if (firstSplitByteRangeIndex < 0) {
                    firstSplitByteRangeIndex = i;
                }
            }
        }
        while (b <= 255) {
            table[b] = (byte) b;
            b++;
        }
        if (firstSplitByteRangeIndex < 0) {
            rangesLength = 0;
            rangesStart = 0;
        } else {
            rangesStart = firstSplitByteRangeIndex;
            rangesLength = rangesLength2 - firstSplitByteRangeIndex;
        }
        setReorderArrays(codes, ranges, rangesStart, rangesLength, table);
    }

    private void setReorderArrays(int[] codes, int[] ranges, int rangesStart, int rangesLength, byte[] table) {
        if (codes == null) {
            codes = EMPTY_INT_ARRAY;
        }
        if (!f50assertionsDisabled) {
            if (!((codes.length == 0) == (table == null))) {
                throw new AssertionError();
            }
        }
        this.reorderTable = table;
        this.reorderCodes = codes;
        setReorderRanges(ranges, rangesStart, rangesLength);
    }

    private void setReorderRanges(int[] ranges, int rangesStart, int rangesLength) {
        if (rangesLength == 0) {
            this.reorderRanges = null;
            return;
        }
        this.reorderRanges = new long[rangesLength];
        int i = 0;
        while (true) {
            int i2 = i + 1;
            int rangesStart2 = rangesStart + 1;
            this.reorderRanges[i] = ((long) ranges[rangesStart]) & 4294967295L;
            if (i2 >= rangesLength) {
                return;
            }
            i = i2;
            rangesStart = rangesStart2;
        }
    }

    public void copyReorderingFrom(CollationSettings other) {
        if (!other.hasReordering()) {
            resetReordering();
            return;
        }
        this.minHighNoReorder = other.minHighNoReorder;
        this.reorderTable = other.reorderTable;
        this.reorderRanges = other.reorderRanges;
        this.reorderCodes = other.reorderCodes;
    }

    public boolean hasReordering() {
        return this.reorderTable != null;
    }

    private static boolean reorderTableHasSplitBytes(byte[] table) {
        if (!f50assertionsDisabled) {
            if (!(table[0] == 0)) {
                throw new AssertionError();
            }
        }
        for (int i = 1; i < 256; i++) {
            if (table[i] == 0) {
                return true;
            }
        }
        return false;
    }

    public long reorder(long p) {
        byte b = this.reorderTable[((int) p) >>> 24];
        if (b != 0 || p <= 1) {
            return ((((long) b) & 255) << 24) | (16777215 & p);
        }
        return reorderEx(p);
    }

    private long reorderEx(long p) {
        if (!f50assertionsDisabled) {
            if (!(this.minHighNoReorder > 0)) {
                throw new AssertionError();
            }
        }
        if (p >= this.minHighNoReorder) {
            return p;
        }
        long q = p | 65535;
        int i = 0;
        while (true) {
            long r = this.reorderRanges[i];
            if (q < r) {
                return (((long) ((short) r)) << 24) + p;
            }
            i++;
        }
    }

    public void setStrength(int value) {
        int noStrength = this.options & (-61441);
        switch (value) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 15:
                this.options = (value << 12) | noStrength;
                return;
            default:
                throw new IllegalArgumentException("illegal strength value " + value);
        }
    }

    public void setStrengthDefault(int defaultOptions) {
        int noStrength = this.options & (-61441);
        this.options = (STRENGTH_MASK & defaultOptions) | noStrength;
    }

    static int getStrength(int options) {
        return options >> 12;
    }

    public int getStrength() {
        return getStrength(this.options);
    }

    public void setFlag(int bit, boolean value) {
        if (value) {
            this.options |= bit;
        } else {
            this.options &= ~bit;
        }
    }

    public void setFlagDefault(int bit, int defaultOptions) {
        this.options = (this.options & (~bit)) | (defaultOptions & bit);
    }

    public boolean getFlag(int bit) {
        return (this.options & bit) != 0;
    }

    public void setCaseFirst(int value) {
        boolean z = true;
        if (!f50assertionsDisabled) {
            if (value != 0 && value != 512 && value != 768) {
                z = false;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        int noCaseFirst = this.options & (-769);
        this.options = noCaseFirst | value;
    }

    public void setCaseFirstDefault(int defaultOptions) {
        int noCaseFirst = this.options & (-769);
        this.options = (defaultOptions & 768) | noCaseFirst;
    }

    public int getCaseFirst() {
        return this.options & 768;
    }

    public void setAlternateHandlingShifted(boolean value) {
        int noAlternate = this.options & (-13);
        if (value) {
            this.options = noAlternate | 4;
        } else {
            this.options = noAlternate;
        }
    }

    public void setAlternateHandlingDefault(int defaultOptions) {
        int noAlternate = this.options & (-13);
        this.options = (defaultOptions & 12) | noAlternate;
    }

    public boolean getAlternateHandling() {
        return (this.options & 12) != 0;
    }

    public void setMaxVariable(int value, int defaultOptions) {
        int noMax = this.options & (-113);
        switch (value) {
            case -1:
                this.options = (defaultOptions & 112) | noMax;
                return;
            case 0:
            case 1:
            case 2:
            case 3:
                this.options = (value << 4) | noMax;
                return;
            default:
                throw new IllegalArgumentException("illegal maxVariable value " + value);
        }
    }

    public int getMaxVariable() {
        return (this.options & 112) >> 4;
    }

    static boolean isTertiaryWithCaseBits(int options) {
        return (options & 1536) == 512;
    }

    static int getTertiaryMask(int options) {
        if (isTertiaryWithCaseBits(options)) {
            return 65343;
        }
        return Collation.ONLY_TERTIARY_MASK;
    }

    static boolean sortsTertiaryUpperCaseFirst(int options) {
        return (options & 1792) == 768;
    }

    public boolean dontCheckFCD() {
        return (this.options & 1) == 0;
    }

    boolean hasBackwardSecondary() {
        return (this.options & 2048) != 0;
    }

    public boolean isNumeric() {
        return (this.options & 2) != 0;
    }

    static {
        f50assertionsDisabled = !CollationSettings.class.desiredAssertionStatus();
        EMPTY_INT_ARRAY = new int[0];
    }
}
