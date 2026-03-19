package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2_32;
import android.icu.text.UnicodeSet;

public final class CollationData {

    static final boolean f36assertionsDisabled;
    private static final int[] EMPTY_INT_ARRAY;
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

    CollationData(Normalizer2Impl nfc) {
        this.nfcImpl = nfc;
    }

    public int getCE32(int c) {
        return this.trie.get(c);
    }

    int getCE32FromSupplementary(int c) {
        return this.trie.get(c);
    }

    boolean isDigit(int c) {
        if (c < 1632) {
            return c <= 57 && 48 <= c;
        }
        return Collation.hasCE32Tag(getCE32(c), 10);
    }

    public boolean isUnsafeBackward(int c, boolean numeric) {
        if (this.unsafeBackwardSet.contains(c)) {
            return true;
        }
        if (numeric) {
            return isDigit(c);
        }
        return false;
    }

    public boolean isCompressibleLeadByte(int b) {
        return this.compressibleBytes[b];
    }

    public boolean isCompressiblePrimary(long p) {
        return isCompressibleLeadByte(((int) p) >>> 24);
    }

    int getCE32FromContexts(int index) {
        return (this.contexts.charAt(index) << 16) | this.contexts.charAt(index + 1);
    }

    int getIndirectCE32(int ce32) {
        if (!f36assertionsDisabled && !Collation.isSpecialCE32(ce32)) {
            throw new AssertionError();
        }
        int tag = Collation.tagFromCE32(ce32);
        if (tag == 10) {
            return this.ce32s[Collation.indexFromCE32(ce32)];
        }
        if (tag == 13) {
            return -1;
        }
        if (tag == 11) {
            return this.ce32s[0];
        }
        return ce32;
    }

    int getFinalCE32(int ce32) {
        if (Collation.isSpecialCE32(ce32)) {
            return getIndirectCE32(ce32);
        }
        return ce32;
    }

    long getCEFromOffsetCE32(int c, int ce32) {
        long dataCE = this.ces[Collation.indexFromCE32(ce32)];
        return Collation.makeCE(Collation.getThreeBytePrimaryForOffsetData(c, dataCE));
    }

    long getSingleCE(int c) {
        CollationData d;
        int ce32 = getCE32(c);
        if (ce32 == 192) {
            d = this.base;
            ce32 = this.base.getCE32(c);
        } else {
            d = this;
        }
        while (Collation.isSpecialCE32(ce32)) {
            switch (Collation.tagFromCE32(ce32)) {
                case 0:
                case 3:
                    throw new AssertionError(String.format("unexpected CE32 tag for U+%04X (CE32 0x%08x)", Integer.valueOf(c), Integer.valueOf(ce32)));
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
                    throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", Integer.valueOf(c), Integer.valueOf(ce32)));
                case 5:
                    if (Collation.lengthFromCE32(ce32) == 1) {
                        ce32 = d.ce32s[Collation.indexFromCE32(ce32)];
                    } else {
                        throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", Integer.valueOf(c), Integer.valueOf(ce32)));
                    }
                    break;
                case 6:
                    if (Collation.lengthFromCE32(ce32) == 1) {
                        return d.ces[Collation.indexFromCE32(ce32)];
                    }
                    throw new UnsupportedOperationException(String.format("there is not exactly one collation element for U+%04X (CE32 0x%08x)", Integer.valueOf(c), Integer.valueOf(ce32)));
                case 10:
                    ce32 = d.ce32s[Collation.indexFromCE32(ce32)];
                    break;
                case 11:
                    if (!f36assertionsDisabled) {
                        if (!(c == 0)) {
                            throw new AssertionError();
                        }
                    }
                    ce32 = d.ce32s[0];
                    break;
                case 14:
                    return d.getCEFromOffsetCE32(c, ce32);
                case 15:
                    return Collation.unassignedCEFromCodePoint(c);
            }
        }
        return Collation.ceFromSimpleCE32(ce32);
    }

    int getFCD16(int c) {
        return this.nfcImpl.getFCD16(c);
    }

    long getFirstPrimaryForGroup(int script) {
        int index = getScriptIndex(script);
        if (index == 0) {
            return 0L;
        }
        return ((long) this.scriptStarts[index]) << 16;
    }

    public long getLastPrimaryForGroup(int script) {
        int index = getScriptIndex(script);
        if (index == 0) {
            return 0L;
        }
        long limit = this.scriptStarts[index + 1];
        return (limit << 16) - 1;
    }

    public int getGroupForPrimary(long p) {
        long p2 = p >> 16;
        if (p2 < this.scriptStarts[1] || this.scriptStarts[this.scriptStarts.length - 1] <= p2) {
            return -1;
        }
        int index = 1;
        while (p2 >= this.scriptStarts[index + 1]) {
            index++;
        }
        for (int i = 0; i < this.numScripts; i++) {
            if (this.scriptsIndex[i] == index) {
                return i;
            }
        }
        for (int i2 = 0; i2 < 8; i2++) {
            if (this.scriptsIndex[this.numScripts + i2] == index) {
                return i2 + 4096;
            }
        }
        return -1;
    }

    private int getScriptIndex(int script) {
        int script2;
        if (script < 0) {
            return 0;
        }
        if (script < this.numScripts) {
            return this.scriptsIndex[script];
        }
        if (script >= 4096 && script - 4096 < 8) {
            return this.scriptsIndex[this.numScripts + script2];
        }
        return 0;
    }

    public int[] getEquivalentScripts(int script) {
        int index = getScriptIndex(script);
        if (index == 0) {
            return EMPTY_INT_ARRAY;
        }
        if (script >= 4096) {
            return new int[]{script};
        }
        int length = 0;
        for (int i = 0; i < this.numScripts; i++) {
            if (this.scriptsIndex[i] == index) {
                length++;
            }
        }
        int[] dest = new int[length];
        if (length == 1) {
            dest[0] = script;
            return dest;
        }
        int length2 = 0;
        for (int i2 = 0; i2 < this.numScripts; i2++) {
            if (this.scriptsIndex[i2] == index) {
                dest[length2] = i2;
                length2++;
            }
        }
        return dest;
    }

    void makeReorderRanges(int[] reorder, UVector32 ranges) {
        makeReorderRanges(reorder, false, ranges);
    }

    private void makeReorderRanges(int[] iArr, boolean z, UVector32 uVector32) {
        uVector32.removeAllElements();
        int length = iArr.length;
        if (length == 0) {
            return;
        }
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
        if (!f36assertionsDisabled) {
            if (!(this.scriptStarts.length >= 2)) {
                throw new AssertionError();
            }
        }
        if (!f36assertionsDisabled) {
            if (!(this.scriptStarts[0] == 0)) {
                throw new AssertionError();
            }
        }
        char c3 = this.scriptStarts[1];
        if (!f36assertionsDisabled) {
            if (!(c3 == 768)) {
                throw new AssertionError();
            }
        }
        char cAddHighScriptRange = this.scriptStarts[this.scriptStarts.length - 1];
        if (!f36assertionsDisabled) {
            if (!(cAddHighScriptRange == 65280)) {
                throw new AssertionError();
            }
        }
        int i = 0;
        for (int i2 : iArr) {
            int i3 = i2 - 4096;
            if (i3 >= 0 && i3 < 8) {
                i |= 1 << i3;
            }
        }
        int i4 = 0;
        int iAddLowScriptRange = c3;
        while (i4 < 8) {
            char c4 = this.scriptsIndex[this.numScripts + i4];
            if (c4 != 0 && ((1 << i4) & i) == 0) {
                iAddLowScriptRange = addLowScriptRange(sArr, c4, iAddLowScriptRange);
            }
            i4++;
            iAddLowScriptRange = iAddLowScriptRange;
        }
        int i5 = 0;
        int i6 = iAddLowScriptRange;
        if (i == 0) {
            i6 = iAddLowScriptRange;
            i6 = iAddLowScriptRange;
            if (iArr[0] == 25 && !z) {
                char c5 = this.scriptsIndex[25];
                if (!f36assertionsDisabled) {
                    if (!(c5 != 0)) {
                        throw new AssertionError();
                    }
                }
                char c6 = this.scriptStarts[c5];
                if (!f36assertionsDisabled) {
                    if (!(iAddLowScriptRange <= c6)) {
                        throw new AssertionError();
                    }
                }
                i5 = c6 - iAddLowScriptRange;
                i6 = c6;
            }
        }
        boolean z2 = false;
        int i7 = 0;
        int iAddLowScriptRange2 = i6;
        while (true) {
            if (i7 >= length) {
                break;
            }
            int i8 = i7 + 1;
            int i9 = iArr[i7];
            if (i9 == 103) {
                z2 = true;
                while (i8 < length) {
                    length--;
                    int i10 = iArr[length];
                    if (i10 == 103) {
                        throw new IllegalArgumentException("setReorderCodes(): duplicate UScript.UNKNOWN");
                    }
                    if (i10 == -1) {
                        throw new IllegalArgumentException("setReorderCodes(): UScript.DEFAULT together with other scripts");
                    }
                    int scriptIndex = getScriptIndex(i10);
                    if (scriptIndex != 0) {
                        if (sArr[scriptIndex] != 0) {
                            throw new IllegalArgumentException("setReorderCodes(): duplicate or equivalent script " + scriptCodeString(i10));
                        }
                        cAddHighScriptRange = addHighScriptRange(sArr, scriptIndex, cAddHighScriptRange);
                    }
                }
            } else {
                if (i9 == -1) {
                    throw new IllegalArgumentException("setReorderCodes(): UScript.DEFAULT together with other scripts");
                }
                int scriptIndex2 = getScriptIndex(i9);
                if (scriptIndex2 != 0) {
                    if (sArr[scriptIndex2] != 0) {
                        throw new IllegalArgumentException("setReorderCodes(): duplicate or equivalent script " + scriptCodeString(i9));
                    }
                    iAddLowScriptRange2 = addLowScriptRange(sArr, scriptIndex2, iAddLowScriptRange2);
                }
                i7 = i8;
                iAddLowScriptRange2 = iAddLowScriptRange2;
            }
        }
    }

    private int addLowScriptRange(short[] table, int index, int lowStart) {
        char c = this.scriptStarts[index];
        if ((c & 255) < (lowStart & 255)) {
            lowStart += 256;
        }
        table[index] = (short) (lowStart >> 8);
        char c2 = this.scriptStarts[index + 1];
        return ((lowStart & Normalizer2Impl.JAMO_VT) + ((c2 & 65280) - (65280 & c))) | (c2 & 255);
    }

    private int addHighScriptRange(short[] table, int index, int highLimit) {
        char c = this.scriptStarts[index + 1];
        if ((c & 255) > (highLimit & 255)) {
            highLimit -= 256;
        }
        char c2 = this.scriptStarts[index];
        int highLimit2 = ((highLimit & Normalizer2Impl.JAMO_VT) - ((c & 65280) - (65280 & c2))) | (c2 & 255);
        table[index] = (short) (highLimit2 >> 8);
        return highLimit2;
    }

    private static String scriptCodeString(int script) {
        return script < 4096 ? Integer.toString(script) : "0x" + Integer.toHexString(script);
    }

    static {
        f36assertionsDisabled = !CollationData.class.desiredAssertionStatus();
        EMPTY_INT_ARRAY = new int[0];
    }
}
