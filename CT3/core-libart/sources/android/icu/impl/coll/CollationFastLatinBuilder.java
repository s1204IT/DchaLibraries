package android.icu.impl.coll;

import android.icu.util.CharsTrie;
import dalvik.bytecode.Opcodes;
import java.lang.reflect.Array;

final class CollationFastLatinBuilder {

    static final boolean f43assertionsDisabled;
    private static final long CONTRACTION_FLAG = 2147483648L;
    private static final int NUM_SPECIAL_GROUPS = 4;
    private long[][] charCEs = (long[][]) Array.newInstance((Class<?>) Long.TYPE, 448, 2);
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

    static {
        f43assertionsDisabled = !CollationFastLatinBuilder.class.desiredAssertionStatus();
    }

    private static final int compareInt64AsUnsigned(long a, long b) {
        long a2 = a - Long.MIN_VALUE;
        long b2 = b - Long.MIN_VALUE;
        if (a2 < b2) {
            return -1;
        }
        if (a2 > b2) {
            return 1;
        }
        return 0;
    }

    private static final int binarySearch(long[] list, int limit, long ce) {
        if (limit == 0) {
            return -1;
        }
        int start = 0;
        while (true) {
            int i = (start + limit) / 2;
            int cmp = compareInt64AsUnsigned(ce, list[i]);
            if (cmp == 0) {
                return i;
            }
            if (cmp < 0) {
                if (i == start) {
                    return ~start;
                }
                limit = i;
            } else {
                if (i == start) {
                    return ~(start + 1);
                }
                start = i;
            }
        }
    }

    CollationFastLatinBuilder() {
    }

    boolean forData(CollationData data) {
        if (this.result.length() != 0) {
            throw new IllegalStateException("attempt to reuse a CollationFastLatinBuilder");
        }
        if (!loadGroups(data)) {
            return false;
        }
        this.firstShortPrimary = this.firstDigitPrimary;
        getCEs(data);
        encodeUniqueCEs();
        if (this.shortPrimaryOverflow) {
            this.firstShortPrimary = this.firstLatinPrimary;
            resetCEs();
            getCEs(data);
            encodeUniqueCEs();
        }
        boolean ok = this.shortPrimaryOverflow ? false : true;
        if (ok) {
            encodeCharCEs();
            encodeContractions();
        }
        this.contractionCEs.removeAllElements();
        this.uniqueCEs.removeAllElements();
        return ok;
    }

    char[] getHeader() {
        char[] resultArray = new char[this.headerLength];
        this.result.getChars(0, this.headerLength, resultArray, 0);
        return resultArray;
    }

    char[] getTable() {
        char[] resultArray = new char[this.result.length() - this.headerLength];
        this.result.getChars(this.headerLength, this.result.length(), resultArray, 0);
        return resultArray;
    }

    private boolean loadGroups(CollationData data) {
        this.headerLength = 5;
        int r0 = this.headerLength | 512;
        this.result.append((char) r0);
        for (int i = 0; i < 4; i++) {
            this.lastSpecialPrimaries[i] = data.getLastPrimaryForGroup(i + 4096);
            if (this.lastSpecialPrimaries[i] == 0) {
                return false;
            }
            this.result.append(0);
        }
        this.firstDigitPrimary = data.getFirstPrimaryForGroup(4100);
        this.firstLatinPrimary = data.getFirstPrimaryForGroup(25);
        this.lastLatinPrimary = data.getLastPrimaryForGroup(25);
        return (this.firstDigitPrimary == 0 || this.firstLatinPrimary == 0) ? false : true;
    }

    private boolean inSameGroup(long p, long q) {
        if (p >= this.firstShortPrimary) {
            return q >= this.firstShortPrimary;
        }
        if (q >= this.firstShortPrimary) {
            return false;
        }
        long lastVariablePrimary = this.lastSpecialPrimaries[3];
        if (p > lastVariablePrimary) {
            return q > lastVariablePrimary;
        }
        if (q > lastVariablePrimary) {
            return false;
        }
        if (!f43assertionsDisabled) {
            if (!((p == 0 || q == 0) ? false : true)) {
                throw new AssertionError();
            }
        }
        int i = 0;
        while (true) {
            long lastPrimary = this.lastSpecialPrimaries[i];
            if (p <= lastPrimary) {
                return q <= lastPrimary;
            }
            if (q > lastPrimary) {
                i++;
            } else {
                return false;
            }
        }
    }

    private void resetCEs() {
        this.contractionCEs.removeAllElements();
        this.uniqueCEs.removeAllElements();
        this.shortPrimaryOverflow = false;
        this.result.setLength(this.headerLength);
    }

    private void getCEs(CollationData data) {
        CollationData d;
        int i = 0;
        char c = 0;
        while (true) {
            if (c == 384) {
                c = 8192;
            } else if (c == 8256) {
                this.contractionCEs.addElement(511L);
                return;
            }
            int ce32 = data.getCE32(c);
            if (ce32 == 192) {
                d = data.base;
                ce32 = d.getCE32(c);
            } else {
                d = data;
            }
            if (getCEsFromCE32(d, c, ce32)) {
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
                if (!f43assertionsDisabled && !this.contractionCEs.isEmpty()) {
                    throw new AssertionError();
                }
                addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, this.ce0, this.ce1);
                this.charCEs[0][0] = 6442450944L;
                this.charCEs[0][1] = 0;
            }
            i++;
            c = (char) (c + 1);
        }
    }

    private boolean getCEsFromCE32(CollationData data, int c, int ce32) {
        int ce322 = data.getFinalCE32(ce32);
        this.ce1 = 0L;
        if (Collation.isSimpleOrLongCE32(ce322)) {
            this.ce0 = Collation.ceFromCE32(ce322);
        } else {
            switch (Collation.tagFromCE32(ce322)) {
                case 4:
                    this.ce0 = Collation.latinCE0FromCE32(ce322);
                    this.ce1 = Collation.latinCE1FromCE32(ce322);
                    break;
                case 5:
                    int index = Collation.indexFromCE32(ce322);
                    int length = Collation.lengthFromCE32(ce322);
                    if (length <= 2) {
                        this.ce0 = Collation.ceFromCE32(data.ce32s[index]);
                        if (length == 2) {
                            this.ce1 = Collation.ceFromCE32(data.ce32s[index + 1]);
                        }
                    } else {
                        return false;
                    }
                    break;
                case 6:
                    int index2 = Collation.indexFromCE32(ce322);
                    int length2 = Collation.lengthFromCE32(ce322);
                    if (length2 <= 2) {
                        this.ce0 = data.ces[index2];
                        if (length2 == 2) {
                            this.ce1 = data.ces[index2 + 1];
                        }
                    } else {
                        return false;
                    }
                    break;
                case 7:
                case 8:
                case 10:
                case 11:
                case 12:
                case 13:
                default:
                    return false;
                case 9:
                    if (!f43assertionsDisabled) {
                        if (!(c >= 0)) {
                            throw new AssertionError();
                        }
                    }
                    return getCEsFromContractionCE32(data, ce322);
                case 14:
                    if (!f43assertionsDisabled) {
                        if (!(c >= 0)) {
                            throw new AssertionError();
                        }
                    }
                    this.ce0 = data.getCEFromOffsetCE32(c, ce322);
                    break;
            }
        }
        if (this.ce0 == 0) {
            return this.ce1 == 0;
        }
        long p0 = this.ce0 >>> 32;
        if (p0 == 0 || p0 > this.lastLatinPrimary) {
            return false;
        }
        int lower32_0 = (int) this.ce0;
        if (p0 < this.firstShortPrimary) {
            int sc0 = lower32_0 & (-16384);
            if (sc0 != 83886080) {
                return false;
            }
        }
        if ((lower32_0 & Collation.ONLY_TERTIARY_MASK) < 1280) {
            return false;
        }
        if (this.ce1 != 0) {
            long p1 = this.ce1 >>> 32;
            if (p1 == 0) {
                if (p0 < this.firstShortPrimary) {
                    return false;
                }
            } else if (!inSameGroup(p0, p1)) {
                return false;
            }
            int lower32_1 = (int) this.ce1;
            if ((lower32_1 >>> 16) == 0) {
                return false;
            }
            if (p1 != 0 && p1 < this.firstShortPrimary) {
                int sc1 = lower32_1 & (-16384);
                if (sc1 != 83886080) {
                    return false;
                }
            }
            if ((lower32_0 & Collation.ONLY_TERTIARY_MASK) < 1280) {
                return false;
            }
        }
        return ((this.ce0 | this.ce1) & 192) == 0;
    }

    private boolean getCEsFromContractionCE32(CollationData data, int ce32) {
        int trieIndex = Collation.indexFromCE32(ce32);
        int ce322 = data.getCE32FromContexts(trieIndex);
        if (!f43assertionsDisabled) {
            if (!(!Collation.isContractionCE32(ce322))) {
                throw new AssertionError();
            }
        }
        int contractionIndex = this.contractionCEs.size();
        if (getCEsFromCE32(data, -1, ce322)) {
            addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, this.ce0, this.ce1);
        } else {
            addContractionEntry(Opcodes.OP_CHECK_CAST_JUMBO, Collation.NO_CE, 0L);
        }
        int prevX = -1;
        boolean addContraction = false;
        CharsTrie.Iterator suffixes = CharsTrie.iterator(data.contexts, trieIndex + 2, 0);
        while (suffixes.hasNext()) {
            CharsTrie.Entry entry = suffixes.next();
            CharSequence suffix = entry.chars;
            int x = CollationFastLatin.getCharIndex(suffix.charAt(0));
            if (x >= 0) {
                if (x == prevX) {
                    if (addContraction) {
                        addContractionEntry(x, Collation.NO_CE, 0L);
                        addContraction = false;
                    }
                } else {
                    if (addContraction) {
                        addContractionEntry(prevX, this.ce0, this.ce1);
                    }
                    int ce323 = entry.value;
                    if (suffix.length() == 1 && getCEsFromCE32(data, -1, ce323)) {
                        addContraction = true;
                    } else {
                        addContractionEntry(x, Collation.NO_CE, 0L);
                        addContraction = false;
                    }
                    prevX = x;
                }
            }
        }
        if (addContraction) {
            addContractionEntry(prevX, this.ce0, this.ce1);
        }
        this.ce0 = ((long) contractionIndex) | 6442450944L;
        this.ce1 = 0L;
        return true;
    }

    private void addContractionEntry(int x, long cce0, long cce1) {
        this.contractionCEs.addElement(x);
        this.contractionCEs.addElement(cce0);
        this.contractionCEs.addElement(cce1);
        addUniqueCE(cce0);
        addUniqueCE(cce1);
    }

    private void addUniqueCE(long ce) {
        long ce2;
        int i;
        if (ce == 0 || (ce >>> 32) == 1 || (i = binarySearch(this.uniqueCEs.getBuffer(), this.uniqueCEs.size(), (ce2 = ce & (-49153)))) >= 0) {
            return;
        }
        this.uniqueCEs.insertElementAt(ce2, ~i);
    }

    private int getMiniCE(long ce) {
        int index = binarySearch(this.uniqueCEs.getBuffer(), this.uniqueCEs.size(), ce & (-49153));
        if (!f43assertionsDisabled) {
            if (!(index >= 0)) {
                throw new AssertionError();
            }
        }
        return this.miniCEs[index];
    }

    private void encodeUniqueCEs() {
        int s;
        int t;
        this.miniCEs = new char[this.uniqueCEs.size()];
        int group = 0;
        long lastGroupPrimary = this.lastSpecialPrimaries[0];
        if (!f43assertionsDisabled) {
            if (!((((int) this.uniqueCEs.elementAti(0)) >>> 16) != 0)) {
                throw new AssertionError();
            }
        }
        long prevPrimary = 0;
        int prevSecondary = 0;
        int pri = 0;
        int sec = 0;
        int ter = 0;
        for (int i = 0; i < this.uniqueCEs.size(); i++) {
            long ce = this.uniqueCEs.elementAti(i);
            long p = ce >>> 32;
            if (p != prevPrimary) {
                while (true) {
                    if (p <= lastGroupPrimary) {
                        break;
                    }
                    if (!f43assertionsDisabled) {
                        if (!(pri <= 4088)) {
                            throw new AssertionError();
                        }
                    }
                    this.result.setCharAt(group + 1, (char) pri);
                    group++;
                    if (group < 4) {
                        lastGroupPrimary = this.lastSpecialPrimaries[group];
                    } else {
                        lastGroupPrimary = 4294967295L;
                        break;
                    }
                }
                if (p < this.firstShortPrimary) {
                    if (pri == 0) {
                        pri = 3072;
                    } else if (pri < 4088) {
                        pri += 8;
                    } else {
                        this.miniCEs[i] = 1;
                    }
                    prevPrimary = p;
                    prevSecondary = Collation.COMMON_WEIGHT16;
                    sec = 160;
                    ter = 0;
                } else {
                    if (pri < 4096) {
                        pri = 4096;
                    } else if (pri < 63488) {
                        pri += 1024;
                    } else {
                        this.shortPrimaryOverflow = true;
                        this.miniCEs[i] = 1;
                    }
                    prevPrimary = p;
                    prevSecondary = Collation.COMMON_WEIGHT16;
                    sec = 160;
                    ter = 0;
                }
                int lower32 = (int) ce;
                s = lower32 >>> 16;
                if (s != prevSecondary) {
                    if (pri == 0) {
                        if (sec == 0) {
                            sec = CollationFastLatin.LATIN_LIMIT;
                        } else if (sec < 992) {
                            sec += 32;
                        } else {
                            this.miniCEs[i] = 1;
                        }
                        prevSecondary = s;
                        ter = 0;
                    } else if (s < 1280) {
                        if (sec == 160) {
                            sec = 0;
                        } else if (sec < 128) {
                            sec += 32;
                        } else {
                            this.miniCEs[i] = 1;
                        }
                        prevSecondary = s;
                        ter = 0;
                    } else {
                        if (s == 1280) {
                            sec = 160;
                        } else if (sec < 192) {
                            sec = 192;
                        } else if (sec < 352) {
                            sec += 32;
                        } else {
                            this.miniCEs[i] = 1;
                        }
                        prevSecondary = s;
                        ter = 0;
                    }
                    if (!f43assertionsDisabled) {
                    }
                    t = lower32 & Collation.ONLY_TERTIARY_MASK;
                    if (t > 1280) {
                    }
                } else {
                    if (!f43assertionsDisabled) {
                        if (!((49152 & lower32) == 0)) {
                            throw new AssertionError();
                        }
                    }
                    t = lower32 & Collation.ONLY_TERTIARY_MASK;
                    if (t > 1280) {
                        if (ter < 7) {
                            ter++;
                            if (3072 > pri) {
                                this.miniCEs[i] = (char) (pri | sec | ter);
                            }
                        } else {
                            this.miniCEs[i] = 1;
                        }
                    } else if (3072 > pri && pri <= 4088) {
                        if (!f43assertionsDisabled) {
                            if (!(sec == 160)) {
                                throw new AssertionError();
                            }
                        }
                        this.miniCEs[i] = (char) (pri | ter);
                    } else {
                        this.miniCEs[i] = (char) (pri | sec | ter);
                    }
                }
            } else {
                int lower322 = (int) ce;
                s = lower322 >>> 16;
                if (s != prevSecondary) {
                }
            }
        }
    }

    private void encodeCharCEs() {
        int miniCEsStart = this.result.length();
        for (int i = 0; i < 448; i++) {
            this.result.append(0);
        }
        int indexBase = this.result.length();
        for (int i2 = 0; i2 < 448; i2++) {
            long ce = this.charCEs[i2][0];
            if (!isContractionCharCE(ce)) {
                int miniCE = encodeTwoCEs(ce, this.charCEs[i2][1]);
                if ((miniCE >>> 16) > 0) {
                    int expansionIndex = this.result.length() - indexBase;
                    if (expansionIndex > 1023) {
                        miniCE = 1;
                    } else {
                        this.result.append((char) (miniCE >> 16)).append((char) miniCE);
                        miniCE = expansionIndex | 2048;
                    }
                }
                this.result.setCharAt(miniCEsStart + i2, (char) miniCE);
            }
        }
    }

    private void encodeContractions() {
        int indexBase = this.headerLength + 448;
        int firstContractionIndex = this.result.length();
        for (int i = 0; i < 448; i++) {
            long ce = this.charCEs[i][0];
            if (isContractionCharCE(ce)) {
                int contractionIndex = this.result.length() - indexBase;
                if (contractionIndex > 1023) {
                    this.result.setCharAt(this.headerLength + i, (char) 1);
                } else {
                    boolean firstTriple = true;
                    int index = ((int) ce) & Integer.MAX_VALUE;
                    while (true) {
                        long x = this.contractionCEs.elementAti(index);
                        if (x == 511 && !firstTriple) {
                            break;
                        }
                        long cce0 = this.contractionCEs.elementAti(index + 1);
                        long cce1 = this.contractionCEs.elementAti(index + 2);
                        int miniCE = encodeTwoCEs(cce0, cce1);
                        if (miniCE == 1) {
                            this.result.append((char) (512 | x));
                        } else if ((miniCE >>> 16) == 0) {
                            this.result.append((char) (1024 | x));
                            this.result.append((char) miniCE);
                        } else {
                            this.result.append((char) (1536 | x));
                            this.result.append((char) (miniCE >> 16)).append((char) miniCE);
                        }
                        firstTriple = false;
                        index += 3;
                    }
                    this.result.setCharAt(this.headerLength + i, (char) (contractionIndex | 1024));
                }
            }
        }
        if (this.result.length() <= firstContractionIndex) {
            return;
        }
        this.result.append((char) 511);
    }

    private int encodeTwoCEs(long first, long second) {
        if (first == 0) {
            return 0;
        }
        if (first == Collation.NO_CE) {
            return 1;
        }
        if (!f43assertionsDisabled) {
            if (!((first >>> 32) != 1)) {
                throw new AssertionError();
            }
        }
        int miniCE = getMiniCE(first);
        if (miniCE == 1) {
            return miniCE;
        }
        if (miniCE >= 4096) {
            int c = (((int) first) & Collation.CASE_MASK) >> 11;
            miniCE |= c + 8;
        }
        if (second == 0) {
            return miniCE;
        }
        int miniCE1 = getMiniCE(second);
        if (miniCE1 == 1) {
            return miniCE1;
        }
        int case1 = ((int) second) & Collation.CASE_MASK;
        if (miniCE >= 4096 && (miniCE & 992) == 160) {
            int sec1 = miniCE1 & 992;
            int ter1 = miniCE1 & 7;
            if (sec1 >= 384 && case1 == 0 && ter1 == 0) {
                return (miniCE & (-993)) | sec1;
            }
        }
        if (miniCE1 <= 992 || 4096 <= miniCE1) {
            miniCE1 |= (case1 >> 11) + 8;
        }
        return (miniCE << 16) | miniCE1;
    }

    private static boolean isContractionCharCE(long ce) {
        return (ce >>> 32) == 1 && ce != Collation.NO_CE;
    }
}
