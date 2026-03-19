package android.icu.impl.coll;

import android.icu.util.CharsTrie;
import dalvik.bytecode.Opcodes;
import java.lang.reflect.Array;

final class CollationFastLatinBuilder {
    static final boolean $assertionsDisabled = false;
    private static final long CONTRACTION_FLAG = 2147483648L;
    private static final int NUM_SPECIAL_GROUPS = 4;
    private long[][] charCEs = (long[][]) Array.newInstance((Class<?>) long.class, 448, 2);
    long[] lastSpecialPrimaries = new long[4];
    private StringBuilder result = new StringBuilder();
    private long ce0 = 0;
    private long ce1 = 0;
    private UVector64 contractionCEs = new UVector64();
    private UVector64 uniqueCEs = new UVector64();
    private char[] miniCEs = null;
    private long firstDigitPrimary = 0;
    private long firstLatinPrimary = 0;
    private long lastLatinPrimary = 0;
    private long firstShortPrimary = 0;
    private boolean shortPrimaryOverflow = false;
    private int headerLength = 0;

    private static final int compareInt64AsUnsigned(long j, long j2) {
        long j3 = j - Long.MIN_VALUE;
        long j4 = j2 - Long.MIN_VALUE;
        if (j3 < j4) {
            return -1;
        }
        if (j3 > j4) {
            return 1;
        }
        return 0;
    }

    private static final int binarySearch(long[] jArr, int i, long j) {
        if (i == 0) {
            return -1;
        }
        int i2 = 0;
        while (true) {
            int i3 = (int) ((((long) i2) + ((long) i)) / 2);
            int iCompareInt64AsUnsigned = compareInt64AsUnsigned(j, jArr[i3]);
            if (iCompareInt64AsUnsigned == 0) {
                return i3;
            }
            if (iCompareInt64AsUnsigned < 0) {
                if (i3 == i2) {
                    return ~i2;
                }
                i = i3;
            } else {
                if (i3 == i2) {
                    return ~(i2 + 1);
                }
                i2 = i3;
            }
        }
    }

    CollationFastLatinBuilder() {
    }

    boolean forData(CollationData collationData) {
        if (this.result.length() != 0) {
            throw new IllegalStateException("attempt to reuse a CollationFastLatinBuilder");
        }
        if (!loadGroups(collationData)) {
            return false;
        }
        this.firstShortPrimary = this.firstDigitPrimary;
        getCEs(collationData);
        encodeUniqueCEs();
        if (this.shortPrimaryOverflow) {
            this.firstShortPrimary = this.firstLatinPrimary;
            resetCEs();
            getCEs(collationData);
            encodeUniqueCEs();
        }
        boolean z = !this.shortPrimaryOverflow;
        if (z) {
            encodeCharCEs();
            encodeContractions();
        }
        this.contractionCEs.removeAllElements();
        this.uniqueCEs.removeAllElements();
        return z;
    }

    char[] getHeader() {
        char[] cArr = new char[this.headerLength];
        this.result.getChars(0, this.headerLength, cArr, 0);
        return cArr;
    }

    char[] getTable() {
        char[] cArr = new char[this.result.length() - this.headerLength];
        this.result.getChars(this.headerLength, this.result.length(), cArr, 0);
        return cArr;
    }

    private boolean loadGroups(CollationData collationData) {
        this.headerLength = 5;
        this.result.append((char) (this.headerLength | 512));
        for (int i = 0; i < 4; i++) {
            this.lastSpecialPrimaries[i] = collationData.getLastPrimaryForGroup(4096 + i);
            if (this.lastSpecialPrimaries[i] == 0) {
                return false;
            }
            this.result.append(0);
        }
        this.firstDigitPrimary = collationData.getFirstPrimaryForGroup(4100);
        this.firstLatinPrimary = collationData.getFirstPrimaryForGroup(25);
        this.lastLatinPrimary = collationData.getLastPrimaryForGroup(25);
        if (this.firstDigitPrimary == 0 || this.firstLatinPrimary == 0) {
            return false;
        }
        return true;
    }

    private boolean inSameGroup(long j, long j2) {
        if (j >= this.firstShortPrimary) {
            return j2 >= this.firstShortPrimary;
        }
        if (j2 >= this.firstShortPrimary) {
            return false;
        }
        long j3 = this.lastSpecialPrimaries[3];
        if (j > j3) {
            return j2 > j3;
        }
        if (j2 > j3) {
            return false;
        }
        int i = 0;
        while (true) {
            long j4 = this.lastSpecialPrimaries[i];
            if (j <= j4) {
                return j2 <= j4;
            }
            if (j2 <= j4) {
                return false;
            }
            i++;
        }
    }

    private void resetCEs() {
        this.contractionCEs.removeAllElements();
        this.uniqueCEs.removeAllElements();
        this.shortPrimaryOverflow = false;
        this.result.setLength(this.headerLength);
    }

    private void getCEs(CollationData collationData) {
        int ce32;
        CollationData collationData2;
        char c = 0;
        int i = 0;
        while (true) {
            if (c == 384) {
                c = 8192;
            } else if (c == 8256) {
                this.contractionCEs.addElement(511L);
                return;
            }
            int ce322 = collationData.getCE32(c);
            if (ce322 == 192) {
                collationData2 = collationData.base;
                ce32 = collationData2.getCE32(c);
            } else {
                ce32 = ce322;
                collationData2 = collationData;
            }
            if (getCEsFromCE32(collationData2, c, ce32)) {
                this.charCEs[i][0] = this.ce0;
                this.charCEs[i][1] = this.ce1;
                addUniqueCE(this.ce0);
                addUniqueCE(this.ce1);
            } else {
                long[] jArr = this.charCEs[i];
                this.ce0 = Collation.NO_CE;
                jArr[0] = 4311744768L;
                long[] jArr2 = this.charCEs[i];
                this.ce1 = 0L;
                jArr2[1] = 0;
            }
            if (c == 0 && !isContractionCharCE(this.ce0)) {
                addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, this.ce0, this.ce1);
                this.charCEs[0][0] = 6442450944L;
                this.charCEs[0][1] = 0;
            }
            i++;
            c = (char) (c + 1);
        }
    }

    private boolean getCEsFromCE32(CollationData collationData, int i, int i2) {
        int i3;
        int finalCE32 = collationData.getFinalCE32(i2);
        this.ce1 = 0L;
        if (Collation.isSimpleOrLongCE32(finalCE32)) {
            this.ce0 = Collation.ceFromCE32(finalCE32);
        } else {
            int iTagFromCE32 = Collation.tagFromCE32(finalCE32);
            if (iTagFromCE32 == 9) {
                return getCEsFromContractionCE32(collationData, finalCE32);
            }
            if (iTagFromCE32 != 14) {
                switch (iTagFromCE32) {
                    case 4:
                        this.ce0 = Collation.latinCE0FromCE32(finalCE32);
                        this.ce1 = Collation.latinCE1FromCE32(finalCE32);
                        break;
                    case 5:
                        int iIndexFromCE32 = Collation.indexFromCE32(finalCE32);
                        int iLengthFromCE32 = Collation.lengthFromCE32(finalCE32);
                        if (iLengthFromCE32 > 2) {
                            return false;
                        }
                        this.ce0 = Collation.ceFromCE32(collationData.ce32s[iIndexFromCE32]);
                        if (iLengthFromCE32 == 2) {
                            this.ce1 = Collation.ceFromCE32(collationData.ce32s[iIndexFromCE32 + 1]);
                        }
                        break;
                        break;
                    case 6:
                        int iIndexFromCE322 = Collation.indexFromCE32(finalCE32);
                        int iLengthFromCE322 = Collation.lengthFromCE32(finalCE32);
                        if (iLengthFromCE322 > 2) {
                            return false;
                        }
                        this.ce0 = collationData.ces[iIndexFromCE322];
                        if (iLengthFromCE322 == 2) {
                            this.ce1 = collationData.ces[iIndexFromCE322 + 1];
                        }
                        break;
                        break;
                    default:
                        return false;
                }
            } else {
                this.ce0 = collationData.getCEFromOffsetCE32(i, finalCE32);
            }
        }
        if (this.ce0 == 0) {
            return this.ce1 == 0;
        }
        long j = this.ce0 >>> 32;
        if (j == 0 || j > this.lastLatinPrimary) {
            return false;
        }
        int i4 = (int) this.ce0;
        if ((j < this.firstShortPrimary && (i4 & (-16384)) != 83886080) || (i3 = i4 & Collation.ONLY_TERTIARY_MASK) < 1280) {
            return false;
        }
        if (this.ce1 != 0) {
            long j2 = this.ce1 >>> 32;
            if (j2 != 0 ? !inSameGroup(j, j2) : j < this.firstShortPrimary) {
                return false;
            }
            int i5 = (int) this.ce1;
            if ((i5 >>> 16) == 0) {
                return false;
            }
            if ((j2 != 0 && j2 < this.firstShortPrimary && (i5 & (-16384)) != 83886080) || i3 < 1280) {
                return false;
            }
        }
        return ((this.ce0 | this.ce1) & 192) == 0;
    }

    private boolean getCEsFromContractionCE32(CollationData collationData, int i) {
        boolean z;
        int charIndex;
        int iIndexFromCE32 = Collation.indexFromCE32(i);
        int cE32FromContexts = collationData.getCE32FromContexts(iIndexFromCE32);
        int size = this.contractionCEs.size();
        if (getCEsFromCE32(collationData, -1, cE32FromContexts)) {
            addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, this.ce0, this.ce1);
        } else {
            addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, Collation.NO_CE, 0L);
        }
        CharsTrie.Iterator it = CharsTrie.iterator(collationData.contexts, iIndexFromCE32 + 2, 0);
        int i2 = -1;
        loop0: while (true) {
            z = false;
            while (it.hasNext()) {
                CharsTrie.Entry next = it.next();
                CharSequence charSequence = next.chars;
                charIndex = CollationFastLatin.getCharIndex(charSequence.charAt(0));
                if (charIndex >= 0) {
                    if (charIndex == i2) {
                        if (z) {
                            break;
                        }
                    } else {
                        if (z) {
                            int i3 = i2;
                            i2 = charIndex;
                            addContractionEntry(i3, this.ce0, this.ce1);
                        } else {
                            i2 = charIndex;
                        }
                        int i4 = next.value;
                        if (charSequence.length() != 1 || !getCEsFromCE32(collationData, -1, i4)) {
                            addContractionEntry(i2, Collation.NO_CE, 0L);
                            z = false;
                        } else {
                            z = true;
                        }
                    }
                }
            }
            addContractionEntry(charIndex, Collation.NO_CE, 0L);
        }
        if (z) {
            addContractionEntry(i2, this.ce0, this.ce1);
        }
        this.ce0 = 6442450944L | ((long) size);
        this.ce1 = 0L;
        return true;
    }

    private void addContractionEntry(int i, long j, long j2) {
        this.contractionCEs.addElement(i);
        this.contractionCEs.addElement(j);
        this.contractionCEs.addElement(j2);
        addUniqueCE(j);
        addUniqueCE(j2);
    }

    private void addUniqueCE(long j) {
        long j2;
        int iBinarySearch;
        if (j != 0 && (j >>> 32) != 1 && (iBinarySearch = binarySearch(this.uniqueCEs.getBuffer(), this.uniqueCEs.size(), (j2 = j & (-49153)))) < 0) {
            this.uniqueCEs.insertElementAt(j2, ~iBinarySearch);
        }
    }

    private int getMiniCE(long j) {
        return this.miniCEs[binarySearch(this.uniqueCEs.getBuffer(), this.uniqueCEs.size(), j & (-49153))];
    }

    private void encodeUniqueCEs() {
        int i;
        int i2;
        this.miniCEs = new char[this.uniqueCEs.size()];
        long j = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        long j2 = this.lastSpecialPrimaries[0];
        int i6 = 0;
        int i7 = 0;
        for (int i8 = 0; i8 < this.uniqueCEs.size(); i8++) {
            long jElementAti = this.uniqueCEs.elementAti(i8);
            long j3 = jElementAti >>> 32;
            if (j3 != j) {
                while (true) {
                    if (j3 <= j2) {
                        break;
                    }
                    i6++;
                    this.result.setCharAt(i6, (char) i7);
                    if (i6 >= 4) {
                        j2 = 4294967295L;
                        break;
                    }
                    j2 = this.lastSpecialPrimaries[i6];
                }
                int i9 = i6;
                if (j3 < this.firstShortPrimary) {
                    if (i7 == 0) {
                        i7 = 3072;
                    } else if (i7 < 4088) {
                        i7 += 8;
                    } else {
                        this.miniCEs[i8] = 1;
                        i6 = i9;
                    }
                    i6 = i9;
                    i = 0;
                    i3 = Collation.COMMON_WEIGHT16;
                    i4 = 160;
                } else {
                    if (i7 < 4096) {
                        i7 = 4096;
                    } else if (i7 < 63488) {
                        i7 += 1024;
                    } else {
                        this.shortPrimaryOverflow = true;
                        this.miniCEs[i8] = 1;
                        i6 = i9;
                    }
                    i6 = i9;
                    i = 0;
                    i3 = Collation.COMMON_WEIGHT16;
                    i4 = 160;
                }
            } else {
                j3 = j;
                i = i5;
            }
            int i10 = (int) jElementAti;
            int i11 = i10 >>> 16;
            if (i11 != i3) {
                if (i7 == 0) {
                    if (i4 == 0) {
                        i2 = CollationFastLatin.LATIN_LIMIT;
                    } else if (i4 < 992) {
                        i2 = i4 + 32;
                    } else {
                        this.miniCEs[i8] = 1;
                        i5 = i;
                    }
                    i4 = i2;
                    i = 0;
                } else {
                    if (i11 >= 1280) {
                        if (i11 == 1280) {
                            i4 = 160;
                        } else if (i4 < 192) {
                            i4 = 192;
                        } else if (i4 < 352) {
                            i2 = i4 + 32;
                            i4 = i2;
                        } else {
                            this.miniCEs[i8] = 1;
                            i5 = i;
                        }
                        i = 0;
                    } else if (i4 == 160) {
                        i4 = 0;
                        i = 0;
                    } else if (i4 < 128) {
                        i2 = i4 + 32;
                        i4 = i2;
                        i = 0;
                    } else {
                        this.miniCEs[i8] = 1;
                        i5 = i;
                    }
                }
                j = j3;
            } else {
                i11 = i3;
            }
            if ((i10 & Collation.ONLY_TERTIARY_MASK) <= 1280) {
                if (3072 <= i7 || i7 > 4088) {
                    this.miniCEs[i8] = (char) (i7 | i4 | i);
                } else {
                    this.miniCEs[i8] = (char) (i7 | i);
                }
                i5 = i;
                i3 = i11;
                j = j3;
            } else if (i < 7) {
                i++;
                if (3072 <= i7) {
                    this.miniCEs[i8] = (char) (i7 | i4 | i);
                    i5 = i;
                    i3 = i11;
                    j = j3;
                }
            } else {
                this.miniCEs[i8] = 1;
                i5 = i;
                i3 = i11;
                j = j3;
            }
        }
    }

    private void encodeCharCEs() {
        int length = this.result.length();
        for (int i = 0; i < 448; i++) {
            this.result.append(0);
        }
        int length2 = this.result.length();
        for (int i2 = 0; i2 < 448; i2++) {
            long j = this.charCEs[i2][0];
            if (!isContractionCharCE(j)) {
                int i3 = 1;
                int iEncodeTwoCEs = encodeTwoCEs(j, this.charCEs[i2][1]);
                if ((iEncodeTwoCEs >>> 16) > 0) {
                    int length3 = this.result.length() - length2;
                    if (length3 <= 1023) {
                        StringBuilder sb = this.result;
                        sb.append((char) (iEncodeTwoCEs >> 16));
                        sb.append((char) iEncodeTwoCEs);
                        i3 = 2048 | length3;
                    }
                } else {
                    i3 = iEncodeTwoCEs;
                }
                this.result.setCharAt(length + i2, (char) i3);
            }
        }
    }

    private void encodeContractions() {
        int i = this.headerLength + 448;
        int length = this.result.length();
        char c = 0;
        int i2 = 0;
        while (i2 < 448) {
            long j = this.charCEs[i2][c];
            if (isContractionCharCE(j)) {
                int length2 = this.result.length() - i;
                if (length2 > 1023) {
                    this.result.setCharAt(this.headerLength + i2, (char) 1);
                } else {
                    int i3 = ((int) j) & Integer.MAX_VALUE;
                    boolean z = true;
                    while (true) {
                        if (this.contractionCEs.elementAti(i3) == 511 && !z) {
                            break;
                        }
                        int i4 = i2;
                        int iEncodeTwoCEs = encodeTwoCEs(this.contractionCEs.elementAti(i3 + 1), this.contractionCEs.elementAti(i3 + 2));
                        if (iEncodeTwoCEs == 1) {
                            this.result.append((char) (r11 | 512));
                        } else if ((iEncodeTwoCEs >>> 16) == 0) {
                            this.result.append((char) (r11 | 1024));
                            this.result.append((char) iEncodeTwoCEs);
                        } else {
                            this.result.append((char) (r11 | 1536));
                            StringBuilder sb = this.result;
                            sb.append((char) (iEncodeTwoCEs >> 16));
                            sb.append((char) iEncodeTwoCEs);
                        }
                        i3 += 3;
                        i2 = i4;
                        c = 0;
                        z = false;
                    }
                    this.result.setCharAt(this.headerLength + i2, (char) (length2 | 1024));
                }
            }
            i2++;
        }
        if (this.result.length() > length) {
            this.result.append((char) 511);
        }
    }

    private int encodeTwoCEs(long j, long j2) {
        if (j == 0) {
            return 0;
        }
        if (j == Collation.NO_CE) {
            return 1;
        }
        int miniCE = getMiniCE(j);
        if (miniCE == 1) {
            return miniCE;
        }
        if (miniCE >= 4096) {
            miniCE |= ((((int) j) & Collation.CASE_MASK) >> 11) + 8;
        }
        if (j2 == 0) {
            return miniCE;
        }
        int miniCE2 = getMiniCE(j2);
        if (miniCE2 == 1) {
            return miniCE2;
        }
        int i = ((int) j2) & Collation.CASE_MASK;
        if (miniCE >= 4096 && (miniCE & 992) == 160) {
            int i2 = miniCE2 & 992;
            int i3 = miniCE2 & 7;
            if (i2 >= 384 && i == 0 && i3 == 0) {
                return (miniCE & (-993)) | i2;
            }
        }
        if (miniCE2 <= 992 || 4096 <= miniCE2) {
            miniCE2 |= (i >> 11) + 8;
        }
        return miniCE2 | (miniCE << 16);
    }

    private static boolean isContractionCharCE(long j) {
        return (j >>> 32) == 1 && j != Collation.NO_CE;
    }
}
