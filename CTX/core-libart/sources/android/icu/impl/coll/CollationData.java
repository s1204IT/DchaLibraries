package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2_32;
import android.icu.text.UnicodeSet;

public final class CollationData {
    static final boolean $assertionsDisabled = false;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    static final int JAMO_CE32S_LENGTH = 67;
    static final int MAX_NUM_SPECIAL_REORDER_CODES = 8;
    static final int REORDER_RESERVED_AFTER_LATIN = 4111;
    static final int REORDER_RESERVED_BEFORE_LATIN = 4110;
    public CollationData base;
    int[] ce32s;
    long[] ces;
    public boolean[] compressibleBytes;
    String contexts;
    public char[] fastLatinTable;
    char[] fastLatinTableHeader;
    public Normalizer2Impl nfcImpl;
    int numScripts;
    public long[] rootElements;
    char[] scriptStarts;
    char[] scriptsIndex;
    Trie2_32 trie;
    UnicodeSet unsafeBackwardSet;
    int[] jamoCE32s = new int[67];
    long numericPrimary = 301989888;

    CollationData(Normalizer2Impl normalizer2Impl) {
        this.nfcImpl = normalizer2Impl;
    }

    public int getCE32(int i) {
        return this.trie.get(i);
    }

    int getCE32FromSupplementary(int i) {
        return this.trie.get(i);
    }

    boolean isDigit(int i) {
        if (i < 1632) {
            return i <= 57 && 48 <= i;
        }
        return Collation.hasCE32Tag(getCE32(i), 10);
    }

    public boolean isUnsafeBackward(int i, boolean z) {
        return this.unsafeBackwardSet.contains(i) || (z && isDigit(i));
    }

    public boolean isCompressibleLeadByte(int i) {
        return this.compressibleBytes[i];
    }

    public boolean isCompressiblePrimary(long j) {
        return isCompressibleLeadByte(((int) j) >>> 24);
    }

    int getCE32FromContexts(int i) {
        return this.contexts.charAt(i + 1) | (this.contexts.charAt(i) << 16);
    }

    int getIndirectCE32(int i) {
        int iTagFromCE32 = Collation.tagFromCE32(i);
        if (iTagFromCE32 == 10) {
            return this.ce32s[Collation.indexFromCE32(i)];
        }
        if (iTagFromCE32 == 13) {
            return -1;
        }
        if (iTagFromCE32 == 11) {
            return this.ce32s[0];
        }
        return i;
    }

    int getFinalCE32(int i) {
        if (Collation.isSpecialCE32(i)) {
            return getIndirectCE32(i);
        }
        return i;
    }

    long getCEFromOffsetCE32(int i, int i2) {
        return Collation.makeCE(Collation.getThreeBytePrimaryForOffsetData(i, this.ces[Collation.indexFromCE32(i2)]));
    }

    long getSingleCE(int i) {
        CollationData collationData;
        int ce32 = getCE32(i);
        if (ce32 == 192) {
            collationData = this.base;
            ce32 = this.base.getCE32(i);
        } else {
            collationData = this;
        }
        while (Collation.isSpecialCE32(ce32)) {
            switch (Collation.tagFromCE32(ce32)) {
                case 0:
                case 3:
                    throw new AssertionError(String.format("unexpected CE32 tag for U+%04X (CE32 0x%08x)", Integer.valueOf(i), Integer.valueOf(ce32)));
                case 1:
                    return Collation.ceFromLongPrimaryCE32(ce32);
                case 2:
                    return Collation.ceFromLongSecondaryCE32(ce32);
                case 4:
                case 7:
                case 8:
                case 9:
                case 12:
                case 13:
                    throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", Integer.valueOf(i), Integer.valueOf(ce32)));
                case 5:
                    if (Collation.lengthFromCE32(ce32) == 1) {
                        ce32 = collationData.ce32s[Collation.indexFromCE32(ce32)];
                    } else {
                        throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", Integer.valueOf(i), Integer.valueOf(ce32)));
                    }
                    break;
                case 6:
                    if (Collation.lengthFromCE32(ce32) == 1) {
                        return collationData.ces[Collation.indexFromCE32(ce32)];
                    }
                    throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", Integer.valueOf(i), Integer.valueOf(ce32)));
                case 10:
                    ce32 = collationData.ce32s[Collation.indexFromCE32(ce32)];
                    break;
                case 11:
                    ce32 = collationData.ce32s[0];
                    break;
                case 14:
                    return collationData.getCEFromOffsetCE32(i, ce32);
                case 15:
                    return Collation.unassignedCEFromCodePoint(i);
            }
        }
        return Collation.ceFromSimpleCE32(ce32);
    }

    int getFCD16(int i) {
        return this.nfcImpl.getFCD16(i);
    }

    long getFirstPrimaryForGroup(int i) {
        int scriptIndex = getScriptIndex(i);
        if (scriptIndex == 0) {
            return 0L;
        }
        return ((long) this.scriptStarts[scriptIndex]) << 16;
    }

    public long getLastPrimaryForGroup(int i) {
        int scriptIndex = getScriptIndex(i);
        if (scriptIndex == 0) {
            return 0L;
        }
        return (((long) this.scriptStarts[scriptIndex + 1]) << 16) - 1;
    }

    public int getGroupForPrimary(long j) {
        long j2 = j >> 16;
        int i = 1;
        if (j2 < this.scriptStarts[1] || this.scriptStarts[this.scriptStarts.length - 1] <= j2) {
            return -1;
        }
        while (true) {
            int i2 = i + 1;
            if (j2 < this.scriptStarts[i2]) {
                break;
            }
            i = i2;
        }
        for (int i3 = 0; i3 < this.numScripts; i3++) {
            if (this.scriptsIndex[i3] == i) {
                return i3;
            }
        }
        for (int i4 = 0; i4 < 8; i4++) {
            if (this.scriptsIndex[this.numScripts + i4] == i) {
                return 4096 + i4;
            }
        }
        return -1;
    }

    private int getScriptIndex(int i) {
        int i2;
        if (i < 0) {
            return 0;
        }
        if (i < this.numScripts) {
            return this.scriptsIndex[i];
        }
        if (i < 4096 || i - 4096 >= 8) {
            return 0;
        }
        return this.scriptsIndex[this.numScripts + i2];
    }

    public int[] getEquivalentScripts(int i) {
        int scriptIndex = getScriptIndex(i);
        if (scriptIndex == 0) {
            return EMPTY_INT_ARRAY;
        }
        if (i >= 4096) {
            return new int[]{i};
        }
        int i2 = 0;
        for (int i3 = 0; i3 < this.numScripts; i3++) {
            if (this.scriptsIndex[i3] == scriptIndex) {
                i2++;
            }
        }
        int[] iArr = new int[i2];
        if (i2 == 1) {
            iArr[0] = i;
            return iArr;
        }
        int i4 = 0;
        for (int i5 = 0; i5 < this.numScripts; i5++) {
            if (this.scriptsIndex[i5] == scriptIndex) {
                iArr[i4] = i5;
                i4++;
            }
        }
        return iArr;
    }

    void makeReorderRanges(int[] iArr, UVector32 uVector32) {
        makeReorderRanges(iArr, false, uVector32);
    }

    private void makeReorderRanges(int[] iArr, boolean z, UVector32 uVector32) {
        int i;
        boolean z2;
        int i2;
        uVector32.removeAllElements();
        int length = iArr.length;
        if (length == 0) {
            return;
        }
        int i3 = 103;
        int i4 = 0;
        if (length == 1 && iArr[0] == 103) {
            return;
        }
        short[] sArr = new short[this.scriptStarts.length - 1];
        char c = this.scriptsIndex[(this.numScripts + 4110) - 4096];
        if (c != 0) {
            sArr[c] = 255;
        }
        char c2 = this.scriptsIndex[(this.numScripts + 4111) - 4096];
        if (c2 != 0) {
            sArr[c2] = 255;
        }
        char c3 = this.scriptStarts[1];
        char cAddHighScriptRange = this.scriptStarts[this.scriptStarts.length - 1];
        int i5 = 0;
        for (int i6 : iArr) {
            int i7 = i6 - 4096;
            if (i7 >= 0 && i7 < 8) {
                i5 |= 1 << i7;
            }
        }
        int iAddLowScriptRange = c3;
        for (int i8 = 0; i8 < 8; i8++) {
            char c4 = this.scriptsIndex[this.numScripts + i8];
            if (c4 != 0 && ((1 << i8) & i5) == 0) {
                iAddLowScriptRange = addLowScriptRange(sArr, c4, iAddLowScriptRange);
            }
        }
        if (i5 == 0 && iArr[0] == 25 && !z) {
            char c5 = this.scriptStarts[this.scriptsIndex[25]];
            int i9 = c5 - iAddLowScriptRange;
            iAddLowScriptRange = c5;
            i = i9;
        } else {
            i = 0;
        }
        int iAddLowScriptRange2 = iAddLowScriptRange;
        int i10 = 0;
        while (true) {
            if (i10 < length) {
                int i11 = i10 + 1;
                int i12 = iArr[i10];
                if (i12 == i3) {
                    while (i11 < length) {
                        length--;
                        int i13 = iArr[length];
                        if (i13 == i3) {
                            throw new IllegalArgumentException("setReorderCodes(): duplicate UScript.UNKNOWN");
                        }
                        if (i13 == -1) {
                            throw new IllegalArgumentException("setReorderCodes(): UScript.DEFAULT together with other scripts");
                        }
                        int scriptIndex = getScriptIndex(i13);
                        if (scriptIndex != 0) {
                            if (sArr[scriptIndex] != 0) {
                                throw new IllegalArgumentException("setReorderCodes(): duplicate or equivalent script " + scriptCodeString(i13));
                            }
                            cAddHighScriptRange = addHighScriptRange(sArr, scriptIndex, cAddHighScriptRange);
                        }
                        i3 = 103;
                    }
                    z2 = true;
                    i2 = cAddHighScriptRange;
                } else {
                    if (i12 == -1) {
                        throw new IllegalArgumentException("setReorderCodes(): UScript.DEFAULT together with other scripts");
                    }
                    int scriptIndex2 = getScriptIndex(i12);
                    if (scriptIndex2 != 0) {
                        if (sArr[scriptIndex2] != 0) {
                            throw new IllegalArgumentException("setReorderCodes(): duplicate or equivalent script " + scriptCodeString(i12));
                        }
                        iAddLowScriptRange2 = addLowScriptRange(sArr, scriptIndex2, iAddLowScriptRange2);
                    }
                    i10 = i11;
                    i3 = 103;
                }
            } else {
                z2 = false;
                i2 = cAddHighScriptRange;
                break;
            }
        }
    }

    private int addLowScriptRange(short[] sArr, int i, int i2) {
        char c = this.scriptStarts[i];
        if ((c & 255) < (i2 & 255)) {
            i2 += 256;
        }
        sArr[i] = (short) (i2 >> 8);
        char c2 = this.scriptStarts[i + 1];
        return (c2 & 255) | ((i2 & 65280) + ((c2 & 65280) - (65280 & c)));
    }

    private int addHighScriptRange(short[] sArr, int i, int i2) {
        char c = this.scriptStarts[i + 1];
        if ((c & 255) > (i2 & 255)) {
            i2 -= 256;
        }
        char c2 = this.scriptStarts[i];
        int i3 = ((i2 & 65280) - ((c & 65280) - (65280 & c2))) | (c2 & 255);
        sArr[i] = (short) (i3 >> 8);
        return i3;
    }

    private static String scriptCodeString(int i) {
        if (i < 4096) {
            return Integer.toString(i);
        }
        return "0x" + Integer.toHexString(i);
    }
}
