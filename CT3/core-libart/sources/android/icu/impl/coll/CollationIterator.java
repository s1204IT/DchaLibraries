package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.impl.Trie2_32;
import android.icu.util.BytesTrie;
import android.icu.util.CharsTrie;
import android.icu.util.ICUException;

public abstract class CollationIterator {

    static final boolean f44assertionsDisabled;
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

    static {
        f44assertionsDisabled = !CollationIterator.class.desiredAssertionStatus();
    }

    private static final class CEBuffer {
        private static final int INITIAL_CAPACITY = 40;
        int length = 0;
        private long[] buffer = new long[40];

        CEBuffer() {
        }

        void append(long ce) {
            if (this.length >= 40) {
                ensureAppendCapacity(1);
            }
            long[] jArr = this.buffer;
            int i = this.length;
            this.length = i + 1;
            jArr[i] = ce;
        }

        void appendUnsafe(long ce) {
            long[] jArr = this.buffer;
            int i = this.length;
            this.length = i + 1;
            jArr[i] = ce;
        }

        void ensureAppendCapacity(int appCap) {
            int capacity = this.buffer.length;
            if (this.length + appCap <= capacity) {
                return;
            }
            do {
                if (capacity < 1000) {
                    capacity *= 4;
                } else {
                    capacity *= 2;
                }
            } while (capacity < this.length + appCap);
            long[] newBuffer = new long[capacity];
            System.arraycopy(this.buffer, 0, newBuffer, 0, this.length);
            this.buffer = newBuffer;
        }

        void incLength() {
            if (this.length >= 40) {
                ensureAppendCapacity(1);
            }
            this.length++;
        }

        long set(int i, long ce) {
            this.buffer[i] = ce;
            return ce;
        }

        long get(int i) {
            return this.buffer[i];
        }

        long[] getCEs() {
            return this.buffer;
        }
    }

    private static final class SkippedState {

        static final boolean f45assertionsDisabled;
        private int pos;
        private int skipLengthAtMatch;
        private final StringBuilder oldBuffer = new StringBuilder();
        private final StringBuilder newBuffer = new StringBuilder();
        private CharsTrie.State state = new CharsTrie.State();

        static {
            f45assertionsDisabled = !SkippedState.class.desiredAssertionStatus();
        }

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
            int c = this.oldBuffer.codePointAt(this.pos);
            this.pos += Character.charCount(c);
            return c;
        }

        void incBeyond() {
            if (!f45assertionsDisabled) {
                if (!(!hasNext())) {
                    throw new AssertionError();
                }
            }
            this.pos++;
        }

        int backwardNumCodePoints(int n) {
            int length = this.oldBuffer.length();
            int beyond = this.pos - length;
            if (beyond > 0) {
                if (beyond >= n) {
                    this.pos -= n;
                    return n;
                }
                this.pos = this.oldBuffer.offsetByCodePoints(length, beyond - n);
                return beyond;
            }
            this.pos = this.oldBuffer.offsetByCodePoints(this.pos, -n);
            return 0;
        }

        void setFirstSkipped(int c) {
            this.skipLengthAtMatch = 0;
            this.newBuffer.setLength(0);
            this.newBuffer.appendCodePoint(c);
        }

        void skip(int c) {
            this.newBuffer.appendCodePoint(c);
        }

        void recordMatch() {
            this.skipLengthAtMatch = this.newBuffer.length();
        }

        void replaceMatch() {
            int oldLength = this.oldBuffer.length();
            if (this.pos > oldLength) {
                this.pos = oldLength;
            }
            this.oldBuffer.delete(0, this.pos).insert(0, this.newBuffer, 0, this.skipLengthAtMatch);
            this.pos = 0;
        }

        void saveTrieState(CharsTrie trie) {
            trie.saveState(this.state);
        }

        void resetToTrieState(CharsTrie trie) {
            trie.resetToState(this.state);
        }
    }

    public CollationIterator(CollationData d) {
        this.trie = d.trie;
        this.data = d;
        this.numCpFwd = -1;
        this.isNumeric = false;
        this.ceBuffer = null;
    }

    public CollationIterator(CollationData d, boolean numeric) {
        this.trie = d.trie;
        this.data = d;
        this.numCpFwd = -1;
        this.isNumeric = numeric;
        this.ceBuffer = new CEBuffer();
    }

    public boolean equals(Object other) {
        if (other == null || !getClass().equals(other.getClass())) {
            return false;
        }
        CollationIterator o = (CollationIterator) other;
        if (this.ceBuffer.length != o.ceBuffer.length || this.cesIndex != o.cesIndex || this.numCpFwd != o.numCpFwd || this.isNumeric != o.isNumeric) {
            return false;
        }
        for (int i = 0; i < this.ceBuffer.length; i++) {
            if (this.ceBuffer.get(i) != o.ceBuffer.get(i)) {
                return false;
            }
        }
        return true;
    }

    public final long nextCE() {
        CollationData d;
        if (this.cesIndex < this.ceBuffer.length) {
            CEBuffer cEBuffer = this.ceBuffer;
            int i = this.cesIndex;
            this.cesIndex = i + 1;
            return cEBuffer.get(i);
        }
        if (!f44assertionsDisabled) {
            if (!(this.cesIndex == this.ceBuffer.length)) {
                throw new AssertionError();
            }
        }
        this.ceBuffer.incLength();
        long cAndCE32 = handleNextCE32();
        int c = (int) (cAndCE32 >> 32);
        int ce32 = (int) cAndCE32;
        int t = ce32 & 255;
        if (t < 192) {
            CEBuffer cEBuffer2 = this.ceBuffer;
            int i2 = this.cesIndex;
            this.cesIndex = i2 + 1;
            return cEBuffer2.set(i2, (((long) (ce32 & (-65536))) << 32) | (((long) (65280 & ce32)) << 16) | ((long) (t << 8)));
        }
        if (t == 192) {
            if (c < 0) {
                CEBuffer cEBuffer3 = this.ceBuffer;
                int i3 = this.cesIndex;
                this.cesIndex = i3 + 1;
                return cEBuffer3.set(i3, Collation.NO_CE);
            }
            d = this.data.base;
            ce32 = d.getCE32(c);
            t = ce32 & 255;
            if (t < 192) {
                CEBuffer cEBuffer4 = this.ceBuffer;
                int i4 = this.cesIndex;
                this.cesIndex = i4 + 1;
                return cEBuffer4.set(i4, (((long) (ce32 & (-65536))) << 32) | (((long) (65280 & ce32)) << 16) | ((long) (t << 8)));
            }
        } else {
            d = this.data;
        }
        if (t == 193) {
            CEBuffer cEBuffer5 = this.ceBuffer;
            int i5 = this.cesIndex;
            this.cesIndex = i5 + 1;
            return cEBuffer5.set(i5, (((long) (ce32 - t)) << 32) | 83887360);
        }
        return nextCEFromCE32(d, c, ce32);
    }

    public final int fetchCEs() {
        while (nextCE() != Collation.NO_CE) {
            this.cesIndex = this.ceBuffer.length;
        }
        return this.ceBuffer.length;
    }

    final void setCurrentCE(long ce) {
        if (!f44assertionsDisabled) {
            if (!(this.cesIndex > 0)) {
                throw new AssertionError();
            }
        }
        this.ceBuffer.set(this.cesIndex - 1, ce);
    }

    public final long previousCE(UVector32 offsets) {
        CollationData d;
        if (this.ceBuffer.length > 0) {
            CEBuffer cEBuffer = this.ceBuffer;
            CEBuffer cEBuffer2 = this.ceBuffer;
            int i = cEBuffer2.length - 1;
            cEBuffer2.length = i;
            return cEBuffer.get(i);
        }
        offsets.removeAllElements();
        int limitOffset = getOffset();
        int c = previousCodePoint();
        if (c < 0) {
            return Collation.NO_CE;
        }
        if (this.data.isUnsafeBackward(c, this.isNumeric)) {
            return previousCEUnsafe(c, offsets);
        }
        int ce32 = this.data.getCE32(c);
        if (ce32 == 192) {
            d = this.data.base;
            ce32 = d.getCE32(c);
        } else {
            d = this.data;
        }
        if (Collation.isSimpleOrLongCE32(ce32)) {
            return Collation.ceFromCE32(ce32);
        }
        appendCEsFromCE32(d, c, ce32, false);
        if (this.ceBuffer.length > 1) {
            offsets.addElement(getOffset());
            while (offsets.size() <= this.ceBuffer.length) {
                offsets.addElement(limitOffset);
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

    protected final void reset(boolean numeric) {
        if (this.ceBuffer == null) {
            this.ceBuffer = new CEBuffer();
        }
        reset();
        this.isNumeric = numeric;
    }

    protected long handleNextCE32() {
        int c = nextCodePoint();
        return c < 0 ? NO_CP_AND_CE32 : makeCodePointAndCE32Pair(c, this.data.getCE32(c));
    }

    protected long makeCodePointAndCE32Pair(int c, int ce32) {
        return (((long) c) << 32) | (((long) ce32) & 4294967295L);
    }

    protected char handleGetTrailSurrogate() {
        return (char) 0;
    }

    protected boolean forbidSurrogateCodePoints() {
        return false;
    }

    protected int getDataCE32(int c) {
        return this.data.getCE32(c);
    }

    protected int getCE32FromBuilderData(int ce32) {
        throw new ICUException("internal program error: should be unreachable");
    }

    protected final void appendCEsFromCE32(CollationData d, int c, int ce32, boolean forward) {
        int nextCp;
        while (Collation.isSpecialCE32(ce32)) {
            switch (Collation.tagFromCE32(ce32)) {
                case 0:
                case 3:
                    throw new ICUException("internal program error: should be unreachable");
                case 1:
                    this.ceBuffer.append(Collation.ceFromLongPrimaryCE32(ce32));
                    return;
                case 2:
                    this.ceBuffer.append(Collation.ceFromLongSecondaryCE32(ce32));
                    return;
                case 4:
                    this.ceBuffer.ensureAppendCapacity(2);
                    this.ceBuffer.set(this.ceBuffer.length, Collation.latinCE0FromCE32(ce32));
                    this.ceBuffer.set(this.ceBuffer.length + 1, Collation.latinCE1FromCE32(ce32));
                    this.ceBuffer.length += 2;
                    return;
                case 5:
                    int index = Collation.indexFromCE32(ce32);
                    int length = Collation.lengthFromCE32(ce32);
                    this.ceBuffer.ensureAppendCapacity(length);
                    while (true) {
                        int index2 = index + 1;
                        this.ceBuffer.appendUnsafe(Collation.ceFromCE32(d.ce32s[index]));
                        length--;
                        if (length <= 0) {
                            return;
                        } else {
                            index = index2;
                        }
                    }
                    break;
                case 6:
                    int index3 = Collation.indexFromCE32(ce32);
                    int length2 = Collation.lengthFromCE32(ce32);
                    this.ceBuffer.ensureAppendCapacity(length2);
                    while (true) {
                        int index4 = index3 + 1;
                        this.ceBuffer.appendUnsafe(d.ces[index3]);
                        length2--;
                        if (length2 <= 0) {
                            return;
                        } else {
                            index3 = index4;
                        }
                    }
                    break;
                case 7:
                    ce32 = getCE32FromBuilderData(ce32);
                    if (ce32 == 192) {
                        d = this.data.base;
                        ce32 = d.getCE32(c);
                    }
                    break;
                case 8:
                    if (forward) {
                        backwardNumCodePoints(1);
                    }
                    ce32 = getCE32FromPrefix(d, ce32);
                    if (forward) {
                        forwardNumCodePoints(1);
                    }
                    break;
                case 9:
                    int index5 = Collation.indexFromCE32(ce32);
                    int defaultCE32 = d.getCE32FromContexts(index5);
                    if (!forward) {
                        ce32 = defaultCE32;
                        break;
                    } else if (this.skipped == null && this.numCpFwd < 0) {
                        nextCp = nextCodePoint();
                        if (nextCp < 0) {
                            ce32 = defaultCE32;
                            break;
                        } else if ((ce32 & 512) != 0 && !CollationFCD.mayHaveLccc(nextCp)) {
                            backwardNumCodePoints(1);
                            ce32 = defaultCE32;
                            break;
                        } else {
                            ce32 = nextCE32FromContraction(d, ce32, d.contexts, index5 + 2, defaultCE32, nextCp);
                            if (ce32 != 1) {
                            }
                        }
                    } else {
                        nextCp = nextSkippedCodePoint();
                        if (nextCp < 0) {
                            ce32 = defaultCE32;
                        } else if ((ce32 & 512) != 0 && !CollationFCD.mayHaveLccc(nextCp)) {
                            backwardNumSkipped(1);
                            ce32 = defaultCE32;
                        } else {
                            ce32 = nextCE32FromContraction(d, ce32, d.contexts, index5 + 2, defaultCE32, nextCp);
                            if (ce32 != 1) {
                                return;
                            }
                        }
                        break;
                    }
                    break;
                case 10:
                    if (this.isNumeric) {
                        appendNumericCEs(ce32, forward);
                        return;
                    }
                    ce32 = d.ce32s[Collation.indexFromCE32(ce32)];
                    break;
                case 11:
                    if (!f44assertionsDisabled) {
                        if (!(c == 0)) {
                            throw new AssertionError();
                        }
                    }
                    ce32 = d.ce32s[0];
                    break;
                case 12:
                    int[] jamoCE32s = d.jamoCE32s;
                    int c2 = c - Normalizer2Impl.Hangul.HANGUL_BASE;
                    int t = c2 % 28;
                    int c3 = c2 / 28;
                    int v = c3 % 21;
                    int c4 = c3 / 21;
                    if ((ce32 & 256) != 0) {
                        this.ceBuffer.ensureAppendCapacity(t == 0 ? 2 : 3);
                        this.ceBuffer.set(this.ceBuffer.length, Collation.ceFromCE32(jamoCE32s[c4]));
                        this.ceBuffer.set(this.ceBuffer.length + 1, Collation.ceFromCE32(jamoCE32s[v + 19]));
                        this.ceBuffer.length += 2;
                        if (t != 0) {
                            this.ceBuffer.appendUnsafe(Collation.ceFromCE32(jamoCE32s[t + 39]));
                            return;
                        }
                        return;
                    }
                    appendCEsFromCE32(d, -1, jamoCE32s[c4], forward);
                    appendCEsFromCE32(d, -1, jamoCE32s[v + 19], forward);
                    if (t == 0) {
                        return;
                    }
                    ce32 = jamoCE32s[t + 39];
                    c = -1;
                    break;
                    break;
                case 13:
                    if (!f44assertionsDisabled && !forward) {
                        throw new AssertionError();
                    }
                    if (!f44assertionsDisabled && !isLeadSurrogate(c)) {
                        throw new AssertionError();
                    }
                    char trail = handleGetTrailSurrogate();
                    if (Character.isLowSurrogate(trail)) {
                        c = Character.toCodePoint((char) c, trail);
                        int ce322 = ce32 & 768;
                        if (ce322 == 0) {
                            ce32 = -1;
                        } else {
                            if (ce322 != 256) {
                                ce32 = d.getCE32FromSupplementary(c);
                                if (ce32 != 192) {
                                    break;
                                }
                            }
                            d = d.base;
                            ce32 = d.getCE32FromSupplementary(c);
                        }
                    } else {
                        ce32 = -1;
                    }
                    break;
                    break;
                case 14:
                    if (!f44assertionsDisabled) {
                        if (!(c >= 0)) {
                            throw new AssertionError();
                        }
                    }
                    this.ceBuffer.append(d.getCEFromOffsetCE32(c, ce32));
                    return;
                case 15:
                    if (!f44assertionsDisabled) {
                        if (!(c >= 0)) {
                            throw new AssertionError();
                        }
                    }
                    if (isSurrogate(c) && forbidSurrogateCodePoints()) {
                        ce32 = -195323;
                    } else {
                        this.ceBuffer.append(Collation.unassignedCEFromCodePoint(c));
                        return;
                    }
                    break;
            }
        }
        this.ceBuffer.append(Collation.ceFromSimpleCE32(ce32));
    }

    private static final boolean isSurrogate(int c) {
        return (c & (-2048)) == 55296;
    }

    protected static final boolean isLeadSurrogate(int c) {
        return (c & (-1024)) == 55296;
    }

    protected static final boolean isTrailSurrogate(int c) {
        return (c & (-1024)) == 56320;
    }

    private final long nextCEFromCE32(CollationData d, int c, int ce32) {
        CEBuffer cEBuffer = this.ceBuffer;
        cEBuffer.length--;
        appendCEsFromCE32(d, c, ce32, true);
        CEBuffer cEBuffer2 = this.ceBuffer;
        int i = this.cesIndex;
        this.cesIndex = i + 1;
        return cEBuffer2.get(i);
    }

    private final int getCE32FromPrefix(CollationData d, int ce32) {
        BytesTrie.Result match;
        int index = Collation.indexFromCE32(ce32);
        int ce322 = d.getCE32FromContexts(index);
        int lookBehind = 0;
        CharsTrie prefixes = new CharsTrie(d.contexts, index + 2);
        do {
            int c = previousCodePoint();
            if (c < 0) {
                break;
            }
            lookBehind++;
            match = prefixes.nextForCodePoint(c);
            if (match.hasValue()) {
                ce322 = prefixes.getValue();
            }
        } while (match.hasNext());
        forwardNumCodePoints(lookBehind);
        return ce322;
    }

    private final int nextSkippedCodePoint() {
        if (this.skipped != null && this.skipped.hasNext()) {
            return this.skipped.next();
        }
        if (this.numCpFwd == 0) {
            return -1;
        }
        int c = nextCodePoint();
        if (this.skipped != null && !this.skipped.isEmpty() && c >= 0) {
            this.skipped.incBeyond();
        }
        if (this.numCpFwd > 0 && c >= 0) {
            this.numCpFwd--;
        }
        return c;
    }

    private final void backwardNumSkipped(int n) {
        if (this.skipped != null && !this.skipped.isEmpty()) {
            n = this.skipped.backwardNumCodePoints(n);
        }
        backwardNumCodePoints(n);
        if (this.numCpFwd >= 0) {
            this.numCpFwd += n;
        }
    }

    private final int nextCE32FromContraction(CollationData d, int contractionCE32, CharSequence trieChars, int trieOffset, int ce32, int c) {
        int nextCp;
        int lookAhead = 1;
        int sinceMatch = 1;
        CharsTrie suffixes = new CharsTrie(trieChars, trieOffset);
        if (this.skipped != null && !this.skipped.isEmpty()) {
            this.skipped.saveTrieState(suffixes);
        }
        BytesTrie.Result match = suffixes.firstForCodePoint(c);
        while (true) {
            if (match.hasValue()) {
                ce32 = suffixes.getValue();
                if (!match.hasNext() || (c = nextSkippedCodePoint()) < 0) {
                    break;
                }
                if (this.skipped != null && !this.skipped.isEmpty()) {
                    this.skipped.saveTrieState(suffixes);
                }
                sinceMatch = 1;
            } else {
                if (match == BytesTrie.Result.NO_MATCH || (nextCp = nextSkippedCodePoint()) < 0) {
                    break;
                }
                c = nextCp;
                sinceMatch++;
            }
            lookAhead++;
            match = suffixes.nextForCodePoint(c);
        }
        if ((contractionCE32 & 1024) != 0 && ((contractionCE32 & 256) == 0 || sinceMatch < lookAhead)) {
            if (sinceMatch > 1) {
                backwardNumSkipped(sinceMatch);
                c = nextSkippedCodePoint();
                lookAhead -= sinceMatch - 1;
                sinceMatch = 1;
            }
            if (d.getFCD16(c) > 255) {
                return nextCE32FromDiscontiguousContraction(d, suffixes, ce32, lookAhead, c);
            }
        }
        backwardNumSkipped(sinceMatch);
        return ce32;
    }

    private final int nextCE32FromDiscontiguousContraction(CollationData d, CharsTrie suffixes, int ce32, int lookAhead, int c) {
        int fcd16 = d.getFCD16(c);
        if (!f44assertionsDisabled) {
            if (!(fcd16 > 255)) {
                throw new AssertionError();
            }
        }
        int nextCp = nextSkippedCodePoint();
        if (nextCp < 0) {
            backwardNumSkipped(1);
            return ce32;
        }
        int lookAhead2 = lookAhead + 1;
        int prevCC = fcd16 & 255;
        int fcd162 = d.getFCD16(nextCp);
        if (fcd162 <= 255) {
            backwardNumSkipped(2);
            return ce32;
        }
        if (this.skipped == null || this.skipped.isEmpty()) {
            if (this.skipped == null) {
                this.skipped = new SkippedState();
            }
            suffixes.reset();
            if (lookAhead2 > 2) {
                backwardNumCodePoints(lookAhead2);
                suffixes.firstForCodePoint(nextCodePoint());
                for (int i = 3; i < lookAhead2; i++) {
                    suffixes.nextForCodePoint(nextCodePoint());
                }
                forwardNumCodePoints(2);
            }
            this.skipped.saveTrieState(suffixes);
        } else {
            this.skipped.resetToTrieState(suffixes);
        }
        this.skipped.setFirstSkipped(c);
        int sinceMatch = 2;
        int c2 = nextCp;
        do {
            if (prevCC < (fcd162 >> 8)) {
                BytesTrie.Result match = suffixes.nextForCodePoint(c2);
                if (match.hasValue()) {
                    ce32 = suffixes.getValue();
                    sinceMatch = 0;
                    this.skipped.recordMatch();
                    if (!match.hasNext()) {
                        break;
                    }
                    this.skipped.saveTrieState(suffixes);
                } else {
                    this.skipped.skip(c2);
                    this.skipped.resetToTrieState(suffixes);
                    prevCC = fcd162 & 255;
                }
                c2 = nextSkippedCodePoint();
                if (c2 < 0) {
                    break;
                }
                sinceMatch++;
                fcd162 = d.getFCD16(c2);
            }
        } while (fcd162 > 255);
        backwardNumSkipped(sinceMatch);
        boolean isTopDiscontiguous = this.skipped.isEmpty();
        this.skipped.replaceMatch();
        if (isTopDiscontiguous && !this.skipped.isEmpty()) {
            int c3 = -1;
            while (true) {
                appendCEsFromCE32(d, c3, ce32, true);
                if (this.skipped.hasNext()) {
                    c3 = this.skipped.next();
                    ce32 = getDataCE32(c3);
                    if (ce32 == 192) {
                        d = this.data.base;
                        ce32 = d.getCE32(c3);
                    } else {
                        d = this.data;
                    }
                } else {
                    this.skipped.clear();
                    return 1;
                }
            }
        } else {
            return ce32;
        }
    }

    private final long previousCEUnsafe(int c, UVector32 offsets) {
        int c2;
        int numBackward = 1;
        do {
            c2 = previousCodePoint();
            if (c2 < 0) {
                break;
            }
            numBackward++;
        } while (this.data.isUnsafeBackward(c2, this.isNumeric));
        this.numCpFwd = numBackward;
        this.cesIndex = 0;
        if (!f44assertionsDisabled) {
            if (!(this.ceBuffer.length == 0)) {
                throw new AssertionError();
            }
        }
        int offset = getOffset();
        while (this.numCpFwd > 0) {
            this.numCpFwd--;
            nextCE();
            if (!f44assertionsDisabled) {
                if (!(this.ceBuffer.get(this.ceBuffer.length + (-1)) != Collation.NO_CE)) {
                    throw new AssertionError();
                }
            }
            this.cesIndex = this.ceBuffer.length;
            if (!f44assertionsDisabled) {
                if (!(offsets.size() < this.ceBuffer.length)) {
                    throw new AssertionError();
                }
            }
            offsets.addElement(offset);
            offset = getOffset();
            while (offsets.size() < this.ceBuffer.length) {
                offsets.addElement(offset);
            }
        }
        if (!f44assertionsDisabled) {
            if (!(offsets.size() == this.ceBuffer.length)) {
                throw new AssertionError();
            }
        }
        offsets.addElement(offset);
        this.numCpFwd = -1;
        backwardNumCodePoints(numBackward);
        this.cesIndex = 0;
        CEBuffer cEBuffer = this.ceBuffer;
        CEBuffer cEBuffer2 = this.ceBuffer;
        int i = cEBuffer2.length - 1;
        cEBuffer2.length = i;
        return cEBuffer.get(i);
    }

    private final void appendNumericCEs(int ce32, boolean forward) {
        int c;
        StringBuilder digits = new StringBuilder();
        if (forward) {
            while (true) {
                char digit = Collation.digitFromCE32(ce32);
                digits.append(digit);
                if (this.numCpFwd == 0 || (c = nextCodePoint()) < 0) {
                    break;
                }
                ce32 = this.data.getCE32(c);
                if (ce32 == 192) {
                    ce32 = this.data.base.getCE32(c);
                }
                if (!Collation.hasCE32Tag(ce32, 10)) {
                    backwardNumCodePoints(1);
                    break;
                } else if (this.numCpFwd > 0) {
                    this.numCpFwd--;
                }
            }
        } else {
            while (true) {
                char digit2 = Collation.digitFromCE32(ce32);
                digits.append(digit2);
                int c2 = previousCodePoint();
                if (c2 < 0) {
                    break;
                }
                ce32 = this.data.getCE32(c2);
                if (ce32 == 192) {
                    ce32 = this.data.base.getCE32(c2);
                }
                if (!Collation.hasCE32Tag(ce32, 10)) {
                    forwardNumCodePoints(1);
                    break;
                }
            }
            digits.reverse();
        }
        int pos = 0;
        while (true) {
            if (pos >= digits.length() - 1 || digits.charAt(pos) != 0) {
                int segmentLength = digits.length() - pos;
                if (segmentLength > 254) {
                    segmentLength = 254;
                }
                appendNumericSegmentCEs(digits.subSequence(pos, pos + segmentLength));
                pos += segmentLength;
                if (pos >= digits.length()) {
                    return;
                }
            } else {
                pos++;
            }
        }
    }

    private final void appendNumericSegmentCEs(CharSequence digits) {
        int pair;
        int pos;
        int length = digits.length();
        if (!f44assertionsDisabled) {
            if (!(1 <= length && length <= 254)) {
                throw new AssertionError();
            }
        }
        if (!f44assertionsDisabled) {
            if (!(length == 1 || digits.charAt(0) != 0)) {
                throw new AssertionError();
            }
        }
        long numericPrimary = this.data.numericPrimary;
        if (length <= 7) {
            int value = digits.charAt(0);
            for (int i = 1; i < length; i++) {
                value = (value * 10) + digits.charAt(i);
            }
            if (value < 74) {
                long primary = numericPrimary | ((long) ((value + 2) << 16));
                this.ceBuffer.append(Collation.makeCE(primary));
                return;
            }
            int value2 = value - 74;
            if (value2 < 10160) {
                long primary2 = ((long) (((value2 / 254) + 76) << 16)) | numericPrimary | ((long) (((value2 % 254) + 2) << 8));
                this.ceBuffer.append(Collation.makeCE(primary2));
                return;
            }
            int value3 = value2 - 10160;
            int i2 = 76 + 40;
            if (value3 < 1032256) {
                long primary3 = numericPrimary | ((long) ((value3 % 254) + 2));
                int value4 = value3 / 254;
                this.ceBuffer.append(Collation.makeCE(primary3 | ((long) (((value4 % 254) + 2) << 8)) | ((long) ((((value4 / 254) % 254) + 116) << 16))));
                return;
            }
        }
        if (!f44assertionsDisabled) {
            if (!(length >= 7)) {
                throw new AssertionError();
            }
        }
        int numPairs = (length + 1) / 2;
        long primary4 = numericPrimary | ((long) ((numPairs + 128) << 16));
        while (digits.charAt(length - 1) == 0 && digits.charAt(length - 2) == 0) {
            length -= 2;
        }
        if ((length & 1) != 0) {
            pair = digits.charAt(0);
            pos = 1;
        } else {
            pair = (digits.charAt(0) * '\n') + digits.charAt(1);
            pos = 2;
        }
        int pair2 = (pair * 2) + 11;
        int shift = 8;
        while (pos < length) {
            if (shift == 0) {
                this.ceBuffer.append(Collation.makeCE(primary4 | ((long) pair2)));
                primary4 = numericPrimary;
                shift = 16;
            } else {
                primary4 |= (long) (pair2 << shift);
                shift -= 8;
            }
            pair2 = (((digits.charAt(pos) * '\n') + digits.charAt(pos + 1)) * 2) + 11;
            pos += 2;
        }
        this.ceBuffer.append(Collation.makeCE(primary4 | ((long) ((pair2 - 1) << shift))));
    }
}
