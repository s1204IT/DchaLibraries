package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2_32;
import android.icu.util.BytesTrie;
import android.icu.util.CharsTrie;
import android.icu.util.ICUException;

public abstract class CollationIterator {
    static final boolean $assertionsDisabled = false;
    protected static final long NO_CP_AND_CE32 = -4294967104L;
    private CEBuffer ceBuffer;
    private int cesIndex;
    protected final CollationData data;
    private boolean isNumeric;
    private int numCpFwd;
    private SkippedState skipped;
    protected final Trie2_32 trie;

    protected abstract void backwardNumCodePoints(int i);

    protected abstract void forwardNumCodePoints(int i);

    public abstract int getOffset();

    public abstract int nextCodePoint();

    public abstract int previousCodePoint();

    public abstract void resetToOffset(int i);

    private static final class CEBuffer {
        private static final int INITIAL_CAPACITY = 40;
        int length = 0;
        private long[] buffer = new long[40];

        CEBuffer() {
        }

        void append(long j) {
            if (this.length >= 40) {
                ensureAppendCapacity(1);
            }
            long[] jArr = this.buffer;
            int i = this.length;
            this.length = i + 1;
            jArr[i] = j;
        }

        void appendUnsafe(long j) {
            long[] jArr = this.buffer;
            int i = this.length;
            this.length = i + 1;
            jArr[i] = j;
        }

        void ensureAppendCapacity(int i) {
            int length = this.buffer.length;
            if (this.length + i <= length) {
                return;
            }
            do {
                if (length < 1000) {
                    length *= 4;
                } else {
                    length *= 2;
                }
            } while (length < this.length + i);
            long[] jArr = new long[length];
            System.arraycopy(this.buffer, 0, jArr, 0, this.length);
            this.buffer = jArr;
        }

        void incLength() {
            if (this.length >= 40) {
                ensureAppendCapacity(1);
            }
            this.length++;
        }

        long set(int i, long j) {
            this.buffer[i] = j;
            return j;
        }

        long get(int i) {
            return this.buffer[i];
        }

        long[] getCEs() {
            return this.buffer;
        }
    }

    private static final class SkippedState {
        static final boolean $assertionsDisabled = false;
        private int pos;
        private int skipLengthAtMatch;
        private final StringBuilder oldBuffer = new StringBuilder();
        private final StringBuilder newBuffer = new StringBuilder();
        private CharsTrie.State state = new CharsTrie.State();

        SkippedState() {
        }

        void clear() {
            this.oldBuffer.setLength(0);
            this.pos = 0;
        }

        boolean isEmpty() {
            return this.oldBuffer.length() == 0;
        }

        boolean hasNext() {
            return this.pos < this.oldBuffer.length();
        }

        int next() {
            int iCodePointAt = this.oldBuffer.codePointAt(this.pos);
            this.pos += Character.charCount(iCodePointAt);
            return iCodePointAt;
        }

        void incBeyond() {
            this.pos++;
        }

        int backwardNumCodePoints(int i) {
            int length = this.oldBuffer.length();
            int i2 = this.pos - length;
            if (i2 > 0) {
                if (i2 >= i) {
                    this.pos -= i;
                    return i;
                }
                this.pos = this.oldBuffer.offsetByCodePoints(length, i2 - i);
                return i2;
            }
            this.pos = this.oldBuffer.offsetByCodePoints(this.pos, -i);
            return 0;
        }

        void setFirstSkipped(int i) {
            this.skipLengthAtMatch = 0;
            this.newBuffer.setLength(0);
            this.newBuffer.appendCodePoint(i);
        }

        void skip(int i) {
            this.newBuffer.appendCodePoint(i);
        }

        void recordMatch() {
            this.skipLengthAtMatch = this.newBuffer.length();
        }

        void replaceMatch() {
            int length = this.oldBuffer.length();
            if (this.pos > length) {
                this.pos = length;
            }
            this.oldBuffer.delete(0, this.pos).insert(0, this.newBuffer, 0, this.skipLengthAtMatch);
            this.pos = 0;
        }

        void saveTrieState(CharsTrie charsTrie) {
            charsTrie.saveState(this.state);
        }

        void resetToTrieState(CharsTrie charsTrie) {
            charsTrie.resetToState(this.state);
        }
    }

    public CollationIterator(CollationData collationData) {
        this.trie = collationData.trie;
        this.data = collationData;
        this.numCpFwd = -1;
        this.isNumeric = false;
        this.ceBuffer = null;
    }

    public CollationIterator(CollationData collationData, boolean z) {
        this.trie = collationData.trie;
        this.data = collationData;
        this.numCpFwd = -1;
        this.isNumeric = z;
        this.ceBuffer = new CEBuffer();
    }

    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        CollationIterator collationIterator = (CollationIterator) obj;
        if (this.ceBuffer.length != collationIterator.ceBuffer.length || this.cesIndex != collationIterator.cesIndex || this.numCpFwd != collationIterator.numCpFwd || this.isNumeric != collationIterator.isNumeric) {
            return false;
        }
        for (int i = 0; i < this.ceBuffer.length; i++) {
            if (this.ceBuffer.get(i) != collationIterator.ceBuffer.get(i)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return 0;
    }

    public final long nextCE() {
        CollationData collationData;
        if (this.cesIndex < this.ceBuffer.length) {
            CEBuffer cEBuffer = this.ceBuffer;
            int i = this.cesIndex;
            this.cesIndex = i + 1;
            return cEBuffer.get(i);
        }
        this.ceBuffer.incLength();
        long jHandleNextCE32 = handleNextCE32();
        int i2 = (int) (jHandleNextCE32 >> 32);
        int i3 = (int) jHandleNextCE32;
        int i4 = i3 & 255;
        if (i4 < 192) {
            CEBuffer cEBuffer2 = this.ceBuffer;
            int i5 = this.cesIndex;
            this.cesIndex = i5 + 1;
            return cEBuffer2.set(i5, ((long) (i4 << 8)) | (((long) (i3 & 65280)) << 16) | (((long) ((-65536) & i3)) << 32));
        }
        if (i4 == 192) {
            if (i2 < 0) {
                CEBuffer cEBuffer3 = this.ceBuffer;
                int i6 = this.cesIndex;
                this.cesIndex = i6 + 1;
                return cEBuffer3.set(i6, Collation.NO_CE);
            }
            CollationData collationData2 = this.data.base;
            int ce32 = collationData2.getCE32(i2);
            int i7 = ce32 & 255;
            if (i7 < 192) {
                CEBuffer cEBuffer4 = this.ceBuffer;
                int i8 = this.cesIndex;
                this.cesIndex = i8 + 1;
                return cEBuffer4.set(i8, (((long) (ce32 & 65280)) << 16) | (((long) ((-65536) & ce32)) << 32) | ((long) (i7 << 8)));
            }
            collationData = collationData2;
            i3 = ce32;
            i4 = i7;
        } else {
            collationData = this.data;
        }
        if (i4 == 193) {
            CEBuffer cEBuffer5 = this.ceBuffer;
            int i9 = this.cesIndex;
            this.cesIndex = i9 + 1;
            return cEBuffer5.set(i9, (((long) (i3 - i4)) << 32) | 83887360);
        }
        return nextCEFromCE32(collationData, i2, i3);
    }

    public final int fetchCEs() {
        while (nextCE() != Collation.NO_CE) {
            this.cesIndex = this.ceBuffer.length;
        }
        return this.ceBuffer.length;
    }

    final void setCurrentCE(long j) {
        this.ceBuffer.set(this.cesIndex - 1, j);
    }

    public final long previousCE(UVector32 uVector32) {
        CollationData collationData;
        if (this.ceBuffer.length > 0) {
            CEBuffer cEBuffer = this.ceBuffer;
            CEBuffer cEBuffer2 = this.ceBuffer;
            int i = cEBuffer2.length - 1;
            cEBuffer2.length = i;
            return cEBuffer.get(i);
        }
        uVector32.removeAllElements();
        int offset = getOffset();
        int iPreviousCodePoint = previousCodePoint();
        if (iPreviousCodePoint < 0) {
            return Collation.NO_CE;
        }
        if (this.data.isUnsafeBackward(iPreviousCodePoint, this.isNumeric)) {
            return previousCEUnsafe(iPreviousCodePoint, uVector32);
        }
        int ce32 = this.data.getCE32(iPreviousCodePoint);
        if (ce32 == 192) {
            CollationData collationData2 = this.data.base;
            collationData = collationData2;
            ce32 = collationData2.getCE32(iPreviousCodePoint);
        } else {
            collationData = this.data;
        }
        if (Collation.isSimpleOrLongCE32(ce32)) {
            return Collation.ceFromCE32(ce32);
        }
        appendCEsFromCE32(collationData, iPreviousCodePoint, ce32, false);
        if (this.ceBuffer.length > 1) {
            uVector32.addElement(getOffset());
            while (uVector32.size() <= this.ceBuffer.length) {
                uVector32.addElement(offset);
            }
        }
        CEBuffer cEBuffer3 = this.ceBuffer;
        CEBuffer cEBuffer4 = this.ceBuffer;
        int i2 = cEBuffer4.length - 1;
        cEBuffer4.length = i2;
        return cEBuffer3.get(i2);
    }

    public final int getCEsLength() {
        return this.ceBuffer.length;
    }

    public final long getCE(int i) {
        return this.ceBuffer.get(i);
    }

    public final long[] getCEs() {
        return this.ceBuffer.getCEs();
    }

    final void clearCEs() {
        this.ceBuffer.length = 0;
        this.cesIndex = 0;
    }

    public final void clearCEsIfNoneRemaining() {
        if (this.cesIndex == this.ceBuffer.length) {
            clearCEs();
        }
    }

    protected final void reset() {
        this.ceBuffer.length = 0;
        this.cesIndex = 0;
        if (this.skipped != null) {
            this.skipped.clear();
        }
    }

    protected final void reset(boolean z) {
        if (this.ceBuffer == null) {
            this.ceBuffer = new CEBuffer();
        }
        reset();
        this.isNumeric = z;
    }

    protected long handleNextCE32() {
        int iNextCodePoint = nextCodePoint();
        return iNextCodePoint < 0 ? NO_CP_AND_CE32 : makeCodePointAndCE32Pair(iNextCodePoint, this.data.getCE32(iNextCodePoint));
    }

    protected long makeCodePointAndCE32Pair(int i, int i2) {
        return (((long) i2) & 4294967295L) | (((long) i) << 32);
    }

    protected char handleGetTrailSurrogate() {
        return (char) 0;
    }

    protected boolean forbidSurrogateCodePoints() {
        return false;
    }

    protected int getDataCE32(int i) {
        return this.data.getCE32(i);
    }

    protected int getCE32FromBuilderData(int i) {
        throw new ICUException("internal program error: should be unreachable");
    }

    protected final void appendCEsFromCE32(CollationData collationData, int i, int i2, boolean z) {
        int iNextSkippedCodePoint;
        while (true) {
            int i3 = i2;
            while (Collation.isSpecialCE32(i3)) {
                switch (Collation.tagFromCE32(i3)) {
                    case 0:
                    case 3:
                        throw new ICUException("internal program error: should be unreachable");
                    case 1:
                        this.ceBuffer.append(Collation.ceFromLongPrimaryCE32(i3));
                        return;
                    case 2:
                        this.ceBuffer.append(Collation.ceFromLongSecondaryCE32(i3));
                        return;
                    case 4:
                        this.ceBuffer.ensureAppendCapacity(2);
                        this.ceBuffer.set(this.ceBuffer.length, Collation.latinCE0FromCE32(i3));
                        this.ceBuffer.set(this.ceBuffer.length + 1, Collation.latinCE1FromCE32(i3));
                        this.ceBuffer.length += 2;
                        return;
                    case 5:
                        int iIndexFromCE32 = Collation.indexFromCE32(i3);
                        int iLengthFromCE32 = Collation.lengthFromCE32(i3);
                        this.ceBuffer.ensureAppendCapacity(iLengthFromCE32);
                        while (true) {
                            int i4 = iIndexFromCE32 + 1;
                            this.ceBuffer.appendUnsafe(Collation.ceFromCE32(collationData.ce32s[iIndexFromCE32]));
                            iLengthFromCE32--;
                            if (iLengthFromCE32 <= 0) {
                                return;
                            } else {
                                iIndexFromCE32 = i4;
                            }
                        }
                        break;
                    case 6:
                        int iIndexFromCE322 = Collation.indexFromCE32(i3);
                        int iLengthFromCE322 = Collation.lengthFromCE32(i3);
                        this.ceBuffer.ensureAppendCapacity(iLengthFromCE322);
                        while (true) {
                            int i5 = iIndexFromCE322 + 1;
                            this.ceBuffer.appendUnsafe(collationData.ces[iIndexFromCE322]);
                            iLengthFromCE322--;
                            if (iLengthFromCE322 <= 0) {
                                return;
                            } else {
                                iIndexFromCE322 = i5;
                            }
                        }
                        break;
                    case 7:
                        i2 = getCE32FromBuilderData(i3);
                        if (i2 == 192) {
                            collationData = this.data.base;
                            i2 = collationData.getCE32(i);
                        } else {
                            continue;
                        }
                        break;
                    case 8:
                        if (z) {
                            backwardNumCodePoints(1);
                        }
                        i2 = getCE32FromPrefix(collationData, i3);
                        if (z) {
                            forwardNumCodePoints(1);
                        } else {
                            continue;
                        }
                        break;
                    case 9:
                        int iIndexFromCE323 = Collation.indexFromCE32(i3);
                        int cE32FromContexts = collationData.getCE32FromContexts(iIndexFromCE323);
                        if (z) {
                            if (this.skipped != null || this.numCpFwd >= 0) {
                                iNextSkippedCodePoint = nextSkippedCodePoint();
                                if (iNextSkippedCodePoint >= 0) {
                                    if ((i3 & 512) == 0 || CollationFCD.mayHaveLccc(iNextSkippedCodePoint)) {
                                        i2 = nextCE32FromContraction(collationData, i3, collationData.contexts, iIndexFromCE323 + 2, cE32FromContexts, iNextSkippedCodePoint);
                                        if (i2 != 1) {
                                            return;
                                        }
                                    } else {
                                        backwardNumSkipped(1);
                                    }
                                }
                                break;
                            } else {
                                iNextSkippedCodePoint = nextCodePoint();
                                if (iNextSkippedCodePoint >= 0) {
                                    if ((i3 & 512) == 0 || CollationFCD.mayHaveLccc(iNextSkippedCodePoint)) {
                                        i2 = nextCE32FromContraction(collationData, i3, collationData.contexts, iIndexFromCE323 + 2, cE32FromContexts, iNextSkippedCodePoint);
                                        if (i2 != 1) {
                                        }
                                    } else {
                                        backwardNumCodePoints(1);
                                    }
                                }
                            }
                        }
                        i3 = cE32FromContexts;
                        break;
                    case 10:
                        if (this.isNumeric) {
                            appendNumericCEs(i3, z);
                            return;
                        } else {
                            i2 = collationData.ce32s[Collation.indexFromCE32(i3)];
                            continue;
                        }
                    case 11:
                        i2 = collationData.ce32s[0];
                        continue;
                    case 12:
                        int[] iArr = collationData.jamoCE32s;
                        int i6 = i - Normalizer2Impl.Hangul.HANGUL_BASE;
                        int i7 = i6 % 28;
                        int i8 = i6 / 28;
                        int i9 = i8 % 21;
                        int i10 = i8 / 21;
                        if ((i3 & 256) != 0) {
                            this.ceBuffer.ensureAppendCapacity(i7 == 0 ? 2 : 3);
                            this.ceBuffer.set(this.ceBuffer.length, Collation.ceFromCE32(iArr[i10]));
                            this.ceBuffer.set(this.ceBuffer.length + 1, Collation.ceFromCE32(iArr[19 + i9]));
                            this.ceBuffer.length += 2;
                            if (i7 != 0) {
                                this.ceBuffer.appendUnsafe(Collation.ceFromCE32(iArr[39 + i7]));
                                return;
                            }
                            return;
                        }
                        appendCEsFromCE32(collationData, -1, iArr[i10], z);
                        appendCEsFromCE32(collationData, -1, iArr[19 + i9], z);
                        if (i7 == 0) {
                            return;
                        }
                        i3 = iArr[39 + i7];
                        i = -1;
                        break;
                    case 13:
                        char cHandleGetTrailSurrogate = handleGetTrailSurrogate();
                        if (Character.isLowSurrogate(cHandleGetTrailSurrogate)) {
                            i = Character.toCodePoint((char) i, cHandleGetTrailSurrogate);
                            int i11 = i3 & CollationSettings.CASE_FIRST_AND_UPPER_MASK;
                            if (i11 != 0) {
                                if (i11 == 256 || (i2 = collationData.getCE32FromSupplementary(i)) == 192) {
                                    collationData = collationData.base;
                                    i2 = collationData.getCE32FromSupplementary(i);
                                }
                            }
                        }
                        i3 = -1;
                        break;
                    case 14:
                        this.ceBuffer.append(collationData.getCEFromOffsetCE32(i, i3));
                        return;
                    case 15:
                        if (isSurrogate(i) && forbidSurrogateCodePoints()) {
                            i2 = -195323;
                            continue;
                        }
                        break;
                }
            }
            this.ceBuffer.append(Collation.ceFromSimpleCE32(i3));
            return;
        }
    }

    private static final boolean isSurrogate(int i) {
        return (i & (-2048)) == 55296;
    }

    protected static final boolean isLeadSurrogate(int i) {
        return (i & (-1024)) == 55296;
    }

    protected static final boolean isTrailSurrogate(int i) {
        return (i & (-1024)) == 56320;
    }

    private final long nextCEFromCE32(CollationData collationData, int i, int i2) {
        this.ceBuffer.length--;
        appendCEsFromCE32(collationData, i, i2, true);
        CEBuffer cEBuffer = this.ceBuffer;
        int i3 = this.cesIndex;
        this.cesIndex = i3 + 1;
        return cEBuffer.get(i3);
    }

    private final int getCE32FromPrefix(CollationData collationData, int i) {
        BytesTrie.Result resultNextForCodePoint;
        int iIndexFromCE32 = Collation.indexFromCE32(i);
        int cE32FromContexts = collationData.getCE32FromContexts(iIndexFromCE32);
        CharsTrie charsTrie = new CharsTrie(collationData.contexts, iIndexFromCE32 + 2);
        int i2 = 0;
        do {
            int iPreviousCodePoint = previousCodePoint();
            if (iPreviousCodePoint < 0) {
                break;
            }
            i2++;
            resultNextForCodePoint = charsTrie.nextForCodePoint(iPreviousCodePoint);
            if (resultNextForCodePoint.hasValue()) {
                cE32FromContexts = charsTrie.getValue();
            }
        } while (resultNextForCodePoint.hasNext());
        forwardNumCodePoints(i2);
        return cE32FromContexts;
    }

    private final int nextSkippedCodePoint() {
        if (this.skipped != null && this.skipped.hasNext()) {
            return this.skipped.next();
        }
        if (this.numCpFwd == 0) {
            return -1;
        }
        int iNextCodePoint = nextCodePoint();
        if (this.skipped != null && !this.skipped.isEmpty() && iNextCodePoint >= 0) {
            this.skipped.incBeyond();
        }
        if (this.numCpFwd > 0 && iNextCodePoint >= 0) {
            this.numCpFwd--;
        }
        return iNextCodePoint;
    }

    private final void backwardNumSkipped(int i) {
        if (this.skipped != null && !this.skipped.isEmpty()) {
            i = this.skipped.backwardNumCodePoints(i);
        }
        backwardNumCodePoints(i);
        if (this.numCpFwd >= 0) {
            this.numCpFwd += i;
        }
    }

    private final int nextCE32FromContraction(CollationData collationData, int i, CharSequence charSequence, int i2, int i3, int i4) {
        int i5;
        int iNextSkippedCodePoint;
        int iNextSkippedCodePoint2;
        int iNextSkippedCodePoint3;
        CharsTrie charsTrie = new CharsTrie(charSequence, i2);
        if (this.skipped != null && !this.skipped.isEmpty()) {
            this.skipped.saveTrieState(charsTrie);
        }
        BytesTrie.Result resultFirstForCodePoint = charsTrie.firstForCodePoint(i4);
        int i6 = i3;
        int i7 = i4;
        int i8 = 1;
        int i9 = 1;
        while (true) {
            if (resultFirstForCodePoint.hasValue()) {
                int value = charsTrie.getValue();
                if (!resultFirstForCodePoint.hasNext() || (iNextSkippedCodePoint3 = nextSkippedCodePoint()) < 0) {
                    break;
                }
                if (this.skipped != null && !this.skipped.isEmpty()) {
                    this.skipped.saveTrieState(charsTrie);
                }
                i7 = iNextSkippedCodePoint3;
                i6 = value;
                i8 = 1;
            } else {
                if (resultFirstForCodePoint == BytesTrie.Result.NO_MATCH || (iNextSkippedCodePoint2 = nextSkippedCodePoint()) < 0) {
                    break;
                }
                i8++;
                i7 = iNextSkippedCodePoint2;
            }
            i9++;
            resultFirstForCodePoint = charsTrie.nextForCodePoint(i7);
        }
        if ((i & 1024) != 0 && ((i & 256) == 0 || i8 < i9)) {
            if (i8 > 1) {
                backwardNumSkipped(i8);
                int i10 = i9 - (i8 - 1);
                iNextSkippedCodePoint = nextSkippedCodePoint();
                i8 = 1;
                i5 = i10;
            } else {
                i5 = i9;
                iNextSkippedCodePoint = i7;
            }
            if (collationData.getFCD16(iNextSkippedCodePoint) > 255) {
                return nextCE32FromDiscontiguousContraction(collationData, charsTrie, i6, i5, iNextSkippedCodePoint);
            }
        }
        backwardNumSkipped(i8);
        return i6;
    }

    private final int nextCE32FromDiscontiguousContraction(CollationData collationData, CharsTrie charsTrie, int i, int i2, int i3) {
        int fcd16 = collationData.getFCD16(i3);
        int iNextSkippedCodePoint = nextSkippedCodePoint();
        if (iNextSkippedCodePoint < 0) {
            backwardNumSkipped(1);
            return i;
        }
        int i4 = i2 + 1;
        int i5 = fcd16 & 255;
        int fcd162 = collationData.getFCD16(iNextSkippedCodePoint);
        int i6 = 2;
        if (fcd162 <= 255) {
            backwardNumSkipped(2);
            return i;
        }
        if (this.skipped == null || this.skipped.isEmpty()) {
            if (this.skipped == null) {
                this.skipped = new SkippedState();
            }
            charsTrie.reset();
            if (i4 > 2) {
                backwardNumCodePoints(i4);
                charsTrie.firstForCodePoint(nextCodePoint());
                for (int i7 = 3; i7 < i4; i7++) {
                    charsTrie.nextForCodePoint(nextCodePoint());
                }
                forwardNumCodePoints(2);
            }
            this.skipped.saveTrieState(charsTrie);
        } else {
            this.skipped.resetToTrieState(charsTrie);
        }
        this.skipped.setFirstSkipped(i3);
        do {
            if (i5 < (fcd162 >> 8)) {
                BytesTrie.Result resultNextForCodePoint = charsTrie.nextForCodePoint(iNextSkippedCodePoint);
                if (resultNextForCodePoint.hasValue()) {
                    i = charsTrie.getValue();
                    i6 = 0;
                    this.skipped.recordMatch();
                    if (!resultNextForCodePoint.hasNext()) {
                        break;
                    }
                    this.skipped.saveTrieState(charsTrie);
                } else {
                    this.skipped.skip(iNextSkippedCodePoint);
                    this.skipped.resetToTrieState(charsTrie);
                    i5 = fcd162 & 255;
                }
                iNextSkippedCodePoint = nextSkippedCodePoint();
                if (iNextSkippedCodePoint < 0) {
                    break;
                }
                i6++;
                fcd162 = collationData.getFCD16(iNextSkippedCodePoint);
            }
        } while (fcd162 > 255);
        backwardNumSkipped(i6);
        boolean zIsEmpty = this.skipped.isEmpty();
        this.skipped.replaceMatch();
        if (zIsEmpty && !this.skipped.isEmpty()) {
            int next = -1;
            while (true) {
                appendCEsFromCE32(collationData, next, i, true);
                if (this.skipped.hasNext()) {
                    next = this.skipped.next();
                    i = getDataCE32(next);
                    if (i == 192) {
                        collationData = this.data.base;
                        i = collationData.getCE32(next);
                    } else {
                        collationData = this.data;
                    }
                } else {
                    this.skipped.clear();
                    return 1;
                }
            }
        } else {
            return i;
        }
    }

    private final long previousCEUnsafe(int i, UVector32 uVector32) {
        int iPreviousCodePoint;
        int i2 = 1;
        do {
            iPreviousCodePoint = previousCodePoint();
            if (iPreviousCodePoint < 0) {
                break;
            }
            i2++;
        } while (this.data.isUnsafeBackward(iPreviousCodePoint, this.isNumeric));
        this.numCpFwd = i2;
        this.cesIndex = 0;
        int offset = getOffset();
        while (this.numCpFwd > 0) {
            this.numCpFwd--;
            nextCE();
            this.cesIndex = this.ceBuffer.length;
            uVector32.addElement(offset);
            offset = getOffset();
            while (uVector32.size() < this.ceBuffer.length) {
                uVector32.addElement(offset);
            }
        }
        uVector32.addElement(offset);
        this.numCpFwd = -1;
        backwardNumCodePoints(i2);
        this.cesIndex = 0;
        CEBuffer cEBuffer = this.ceBuffer;
        CEBuffer cEBuffer2 = this.ceBuffer;
        int i3 = cEBuffer2.length - 1;
        cEBuffer2.length = i3;
        return cEBuffer.get(i3);
    }

    private final void appendNumericCEs(int i, boolean z) {
        int iNextCodePoint;
        StringBuilder sb = new StringBuilder();
        if (z) {
            while (true) {
                sb.append(Collation.digitFromCE32(i));
                if (this.numCpFwd == 0 || (iNextCodePoint = nextCodePoint()) < 0) {
                    break;
                }
                int ce32 = this.data.getCE32(iNextCodePoint);
                if (ce32 == 192) {
                    i = this.data.base.getCE32(iNextCodePoint);
                } else {
                    i = ce32;
                }
                if (!Collation.hasCE32Tag(i, 10)) {
                    backwardNumCodePoints(1);
                    break;
                } else if (this.numCpFwd > 0) {
                    this.numCpFwd--;
                }
            }
        } else {
            while (true) {
                sb.append(Collation.digitFromCE32(i));
                int iPreviousCodePoint = previousCodePoint();
                if (iPreviousCodePoint < 0) {
                    break;
                }
                int ce322 = this.data.getCE32(iPreviousCodePoint);
                if (ce322 == 192) {
                    i = this.data.base.getCE32(iPreviousCodePoint);
                } else {
                    i = ce322;
                }
                if (!Collation.hasCE32Tag(i, 10)) {
                    forwardNumCodePoints(1);
                    break;
                }
            }
            sb.reverse();
        }
        int i2 = 0;
        while (true) {
            if (i2 >= sb.length() - 1 || sb.charAt(i2) != 0) {
                int length = sb.length() - i2;
                if (length > 254) {
                    length = 254;
                }
                int i3 = length + i2;
                appendNumericSegmentCEs(sb.subSequence(i2, i3));
                if (i3 < sb.length()) {
                    i2 = i3;
                } else {
                    return;
                }
            } else {
                i2++;
            }
        }
    }

    private final void appendNumericSegmentCEs(CharSequence charSequence) {
        int iCharAt;
        int i;
        int length = charSequence.length();
        long j = this.data.numericPrimary;
        int i2 = 8;
        if (length <= 7) {
            int iCharAt2 = charSequence.charAt(0);
            for (int i3 = 1; i3 < length; i3++) {
                iCharAt2 = (iCharAt2 * 10) + charSequence.charAt(i3);
            }
            if (iCharAt2 < 74) {
                this.ceBuffer.append(Collation.makeCE(j | ((long) ((2 + iCharAt2) << 16))));
                return;
            }
            int i4 = iCharAt2 - 74;
            if (i4 < 10160) {
                this.ceBuffer.append(Collation.makeCE(j | ((long) ((76 + (i4 / 254)) << 16)) | ((long) ((2 + (i4 % 254)) << 8))));
                return;
            }
            int i5 = i4 - 10160;
            if (i5 < 1032256) {
                long j2 = j | ((long) ((i5 % 254) + 2));
                int i6 = i5 / 254;
                this.ceBuffer.append(Collation.makeCE(j2 | ((long) ((2 + (i6 % 254)) << 8)) | ((long) ((116 + ((i6 / 254) % 254)) << 16))));
                return;
            }
        }
        long j3 = ((long) ((128 + ((length + 1) / 2)) << 16)) | j;
        while (charSequence.charAt(length - 1) == 0 && charSequence.charAt(length - 2) == 0) {
            length -= 2;
        }
        if ((length & 1) != 0) {
            iCharAt = charSequence.charAt(0);
            i = 1;
        } else {
            iCharAt = (charSequence.charAt(0) * '\n') + charSequence.charAt(1);
            i = 2;
        }
        int iCharAt3 = (iCharAt * 2) + 11;
        while (i < length) {
            if (i2 == 0) {
                this.ceBuffer.append(Collation.makeCE(((long) iCharAt3) | j3));
                j3 = j;
                i2 = 16;
            } else {
                j3 |= (long) (iCharAt3 << i2);
                i2 -= 8;
            }
            iCharAt3 = (((charSequence.charAt(i) * '\n') + charSequence.charAt(i + 1)) * 2) + 11;
            i += 2;
        }
        this.ceBuffer.append(Collation.makeCE(((long) ((iCharAt3 - 1) << i2)) | j3));
    }
}
