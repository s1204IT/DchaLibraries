package android.icu.impl.coll;

import android.icu.text.DateTimePatternGenerator;
import java.util.Arrays;

public final class CollationSettings extends SharedObject {
    static final boolean $assertionsDisabled = false;
    static final int ALTERNATE_MASK = 12;
    public static final int BACKWARD_SECONDARY = 2048;
    public static final int CASE_FIRST = 512;
    public static final int CASE_FIRST_AND_UPPER_MASK = 768;
    public static final int CASE_LEVEL = 1024;
    public static final int CHECK_FCD = 1;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
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
    public CollationSettings mo1clone() {
        CollationSettings collationSettings = (CollationSettings) super.mo1clone();
        collationSettings.fastLatinPrimaries = (char[]) this.fastLatinPrimaries.clone();
        return collationSettings;
    }

    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        CollationSettings collationSettings = (CollationSettings) obj;
        if (this.options != collationSettings.options) {
            return false;
        }
        if (((this.options & 12) != 0 && this.variableTop != collationSettings.variableTop) || !Arrays.equals(this.reorderCodes, collationSettings.reorderCodes)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int i = this.options << 8;
        if ((this.options & 12) != 0) {
            i = (int) (((long) i) ^ this.variableTop);
        }
        int length = i ^ this.reorderCodes.length;
        for (int i2 = 0; i2 < this.reorderCodes.length; i2++) {
            length ^= this.reorderCodes[i2] << i2;
        }
        return length;
    }

    public void resetReordering() {
        this.reorderTable = null;
        this.minHighNoReorder = 0L;
        this.reorderRanges = null;
        this.reorderCodes = EMPTY_INT_ARRAY;
    }

    void aliasReordering(CollationData collationData, int[] iArr, int i, byte[] bArr) {
        int[] iArr2;
        if (i != iArr.length) {
            iArr2 = new int[i];
            System.arraycopy(iArr, 0, iArr2, 0, i);
        } else {
            iArr2 = iArr;
        }
        int length = iArr.length;
        int i2 = length - i;
        if (bArr != null && (i2 != 0 ? !(i2 < 2 || (iArr[i] & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) != 0 || (iArr[length - 1] & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) == 0) : !reorderTableHasSplitBytes(bArr))) {
            this.reorderTable = bArr;
            this.reorderCodes = iArr2;
            while (i < length && (iArr[i] & 16711680) == 0) {
                i++;
            }
            if (i == length) {
                this.minHighNoReorder = 0L;
                this.reorderRanges = null;
                return;
            } else {
                this.minHighNoReorder = ((long) iArr[length - 1]) & Collation.MAX_PRIMARY;
                setReorderRanges(iArr, i, length - i);
                return;
            }
        }
        setReordering(collationData, iArr2);
    }

    public void setReordering(CollationData collationData, int[] iArr) {
        int i;
        int i2;
        if (iArr.length == 0 || (iArr.length == 1 && iArr[0] == 103)) {
            resetReordering();
            return;
        }
        UVector32 uVector32 = new UVector32();
        collationData.makeReorderRanges(iArr, uVector32);
        int size = uVector32.size();
        if (size == 0) {
            resetReordering();
            return;
        }
        int[] buffer = uVector32.getBuffer();
        this.minHighNoReorder = ((long) buffer[size - 1]) & Collation.MAX_PRIMARY;
        byte[] bArr = new byte[256];
        int i3 = -1;
        int i4 = 0;
        for (int i5 = 0; i5 < size; i5++) {
            int i6 = buffer[i5];
            int i7 = i6 >>> 24;
            while (i4 < i7) {
                bArr[i4] = (byte) (i4 + i6);
                i4++;
            }
            if ((i6 & 16711680) != 0) {
                bArr[i7] = 0;
                int i8 = i7 + 1;
                if (i3 < 0) {
                    i3 = i5;
                }
                i4 = i8;
            }
        }
        while (i4 <= 255) {
            bArr[i4] = (byte) i4;
            i4++;
        }
        if (i3 >= 0) {
            i = size - i3;
            i2 = i3;
        } else {
            i2 = 0;
            i = 0;
        }
        setReorderArrays(iArr, buffer, i2, i, bArr);
    }

    private void setReorderArrays(int[] iArr, int[] iArr2, int i, int i2, byte[] bArr) {
        if (iArr == null) {
            iArr = EMPTY_INT_ARRAY;
        }
        this.reorderTable = bArr;
        this.reorderCodes = iArr;
        setReorderRanges(iArr2, i, i2);
    }

    private void setReorderRanges(int[] iArr, int i, int i2) {
        if (i2 == 0) {
            this.reorderRanges = null;
            return;
        }
        this.reorderRanges = new long[i2];
        int i3 = 0;
        while (true) {
            int i4 = i3 + 1;
            int i5 = i + 1;
            this.reorderRanges[i3] = ((long) iArr[i]) & 4294967295L;
            if (i4 < i2) {
                i3 = i4;
                i = i5;
            } else {
                return;
            }
        }
    }

    public void copyReorderingFrom(CollationSettings collationSettings) {
        if (!collationSettings.hasReordering()) {
            resetReordering();
            return;
        }
        this.minHighNoReorder = collationSettings.minHighNoReorder;
        this.reorderTable = collationSettings.reorderTable;
        this.reorderRanges = collationSettings.reorderRanges;
        this.reorderCodes = collationSettings.reorderCodes;
    }

    public boolean hasReordering() {
        return this.reorderTable != null;
    }

    private static boolean reorderTableHasSplitBytes(byte[] bArr) {
        for (int i = 1; i < 256; i++) {
            if (bArr[i] == 0) {
                return true;
            }
        }
        return false;
    }

    public long reorder(long j) {
        byte b = this.reorderTable[((int) j) >>> 24];
        if (b != 0 || j <= 1) {
            return (j & 16777215) | ((((long) b) & 255) << 24);
        }
        return reorderEx(j);
    }

    private long reorderEx(long j) {
        if (j >= this.minHighNoReorder) {
            return j;
        }
        long j2 = 65535 | j;
        int i = 0;
        while (true) {
            long j3 = this.reorderRanges[i];
            if (j2 < j3) {
                return j + (((long) ((short) j3)) << 24);
            }
            i++;
        }
    }

    public void setStrength(int i) {
        int i2 = this.options & (-61441);
        if (i != 15) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                case 3:
                    break;
                default:
                    throw new IllegalArgumentException("illegal strength value " + i);
            }
        }
        this.options = (i << 12) | i2;
    }

    public void setStrengthDefault(int i) {
        this.options = (i & STRENGTH_MASK) | (this.options & (-61441));
    }

    static int getStrength(int i) {
        return i >> 12;
    }

    public int getStrength() {
        return getStrength(this.options);
    }

    public void setFlag(int i, boolean z) {
        if (z) {
            this.options = i | this.options;
        } else {
            this.options = (~i) & this.options;
        }
    }

    public void setFlagDefault(int i, int i2) {
        this.options = (i & i2) | (this.options & (~i));
    }

    public boolean getFlag(int i) {
        return (i & this.options) != 0;
    }

    public void setCaseFirst(int i) {
        this.options = i | (this.options & (-769));
    }

    public void setCaseFirstDefault(int i) {
        this.options = (i & CASE_FIRST_AND_UPPER_MASK) | (this.options & (-769));
    }

    public int getCaseFirst() {
        return this.options & CASE_FIRST_AND_UPPER_MASK;
    }

    public void setAlternateHandlingShifted(boolean z) {
        int i = this.options & (-13);
        if (z) {
            this.options = i | 4;
        } else {
            this.options = i;
        }
    }

    public void setAlternateHandlingDefault(int i) {
        this.options = (i & 12) | (this.options & (-13));
    }

    public boolean getAlternateHandling() {
        return (this.options & 12) != 0;
    }

    public void setMaxVariable(int i, int i2) {
        int i3 = this.options & (-113);
        switch (i) {
            case -1:
                this.options = (i2 & 112) | i3;
                return;
            case 0:
            case 1:
            case 2:
            case 3:
                this.options = (i << 4) | i3;
                return;
            default:
                throw new IllegalArgumentException("illegal maxVariable value " + i);
        }
    }

    public int getMaxVariable() {
        return (this.options & 112) >> 4;
    }

    static boolean isTertiaryWithCaseBits(int i) {
        return (i & 1536) == 512;
    }

    static int getTertiaryMask(int i) {
        if (isTertiaryWithCaseBits(i)) {
            return 65343;
        }
        return Collation.ONLY_TERTIARY_MASK;
    }

    static boolean sortsTertiaryUpperCaseFirst(int i) {
        return (i & 1792) == 768;
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
}
