package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.text.UCharacterIterator;

public final class FCDIterCollationIterator extends IterCollationIterator {

    static final boolean f54assertionsDisabled;
    private int limit;
    private final Normalizer2Impl nfcImpl;
    private StringBuilder normalized;
    private int pos;
    private StringBuilder s;
    private int start;
    private State state;

    static {
        f54assertionsDisabled = !FCDIterCollationIterator.class.desiredAssertionStatus();
    }

    public FCDIterCollationIterator(CollationData data, boolean numeric, UCharacterIterator ui, int startIndex) {
        super(data, numeric, ui);
        this.state = State.ITER_CHECK_FWD;
        this.start = startIndex;
        this.nfcImpl = data.nfcImpl;
    }

    @Override
    public void resetToOffset(int newOffset) {
        super.resetToOffset(newOffset);
        this.start = newOffset;
        this.state = State.ITER_CHECK_FWD;
    }

    @Override
    public int getOffset() {
        if (this.state.compareTo(State.ITER_CHECK_BWD) <= 0) {
            return this.iter.getIndex();
        }
        if (this.state == State.ITER_IN_FCD_SEGMENT) {
            return this.pos;
        }
        if (this.pos == 0) {
            return this.start;
        }
        return this.limit;
    }

    @Override
    public int nextCodePoint() {
        while (true) {
            if (this.state == State.ITER_CHECK_FWD) {
                int c = this.iter.next();
                if (c < 0) {
                    return c;
                }
                if (!CollationFCD.hasTccc(c) || (!CollationFCD.maybeTibetanCompositeVowel(c) && !CollationFCD.hasLccc(this.iter.current()))) {
                    break;
                }
                this.iter.previous();
                if (!nextSegment()) {
                    return -1;
                }
            } else {
                if (this.state == State.ITER_IN_FCD_SEGMENT && this.pos != this.limit) {
                    int c2 = this.iter.nextCodePoint();
                    this.pos += Character.charCount(c2);
                    if (!f54assertionsDisabled) {
                        if (!(c2 >= 0)) {
                            throw new AssertionError();
                        }
                    }
                    return c2;
                }
                if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) >= 0 && this.pos != this.normalized.length()) {
                    int c3 = this.normalized.codePointAt(this.pos);
                    this.pos += Character.charCount(c3);
                    return c3;
                }
                switchToForward();
            }
        }
    }

    @Override
    public int previousCodePoint() {
        while (true) {
            if (this.state == State.ITER_CHECK_BWD) {
                int c = this.iter.previous();
                if (c < 0) {
                    this.pos = 0;
                    this.start = 0;
                    this.state = State.ITER_IN_FCD_SEGMENT;
                    return -1;
                }
                if (!CollationFCD.hasLccc(c)) {
                    break;
                }
                int prev = -1;
                if (!CollationFCD.maybeTibetanCompositeVowel(c)) {
                    prev = this.iter.previous();
                    if (!CollationFCD.hasTccc(prev)) {
                        if (isTrailSurrogate(c)) {
                            if (prev < 0) {
                                prev = this.iter.previous();
                            }
                            if (isLeadSurrogate(prev)) {
                                return Character.toCodePoint((char) prev, (char) c);
                            }
                        }
                        if (prev >= 0) {
                            this.iter.next();
                        }
                    }
                }
                this.iter.next();
                if (prev >= 0) {
                    this.iter.next();
                }
                if (!previousSegment()) {
                    return -1;
                }
            } else {
                if (this.state == State.ITER_IN_FCD_SEGMENT && this.pos != this.start) {
                    int c2 = this.iter.previousCodePoint();
                    this.pos -= Character.charCount(c2);
                    if (!f54assertionsDisabled) {
                        if (!(c2 >= 0)) {
                            throw new AssertionError();
                        }
                    }
                    return c2;
                }
                if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) >= 0 && this.pos != 0) {
                    int c3 = this.normalized.codePointBefore(this.pos);
                    this.pos -= Character.charCount(c3);
                    return c3;
                }
                switchToBackward();
            }
        }
    }

    @Override
    protected long handleNextCE32() {
        int c;
        while (true) {
            if (this.state == State.ITER_CHECK_FWD) {
                c = this.iter.next();
                if (c < 0) {
                    return -4294967104L;
                }
                if (!CollationFCD.hasTccc(c) || (!CollationFCD.maybeTibetanCompositeVowel(c) && !CollationFCD.hasLccc(this.iter.current()))) {
                    break;
                }
                this.iter.previous();
                if (!nextSegment()) {
                    return 192L;
                }
            } else if (this.state == State.ITER_IN_FCD_SEGMENT && this.pos != this.limit) {
                c = this.iter.next();
                this.pos++;
                if (!f54assertionsDisabled) {
                    if (!(c >= 0)) {
                        throw new AssertionError();
                    }
                }
            } else {
                if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) >= 0 && this.pos != this.normalized.length()) {
                    StringBuilder sb = this.normalized;
                    int i = this.pos;
                    this.pos = i + 1;
                    c = sb.charAt(i);
                    break;
                }
                switchToForward();
            }
        }
        return makeCodePointAndCE32Pair(c, this.trie.getFromU16SingleLead((char) c));
    }

    @Override
    protected char handleGetTrailSurrogate() {
        if (this.state.compareTo(State.ITER_IN_FCD_SEGMENT) <= 0) {
            int trail = this.iter.next();
            if (isTrailSurrogate(trail)) {
                if (this.state == State.ITER_IN_FCD_SEGMENT) {
                    this.pos++;
                }
            } else if (trail >= 0) {
                this.iter.previous();
            }
            return (char) trail;
        }
        if (!f54assertionsDisabled) {
            if (!(this.pos < this.normalized.length())) {
                throw new AssertionError();
            }
        }
        char trail2 = this.normalized.charAt(this.pos);
        if (Character.isLowSurrogate(trail2)) {
            this.pos++;
        }
        return trail2;
    }

    @Override
    protected void forwardNumCodePoints(int num) {
        while (num > 0 && nextCodePoint() >= 0) {
            num--;
        }
    }

    @Override
    protected void backwardNumCodePoints(int num) {
        while (num > 0 && previousCodePoint() >= 0) {
            num--;
        }
    }

    private void switchToForward() {
        boolean z = true;
        if (!f54assertionsDisabled) {
            if (this.state != State.ITER_CHECK_BWD && ((this.state != State.ITER_IN_FCD_SEGMENT || this.pos != this.limit) && (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) < 0 || this.pos != this.normalized.length()))) {
                z = false;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        if (this.state == State.ITER_CHECK_BWD) {
            int index = this.iter.getIndex();
            this.pos = index;
            this.start = index;
            if (this.pos == this.limit) {
                this.state = State.ITER_CHECK_FWD;
                return;
            } else {
                this.state = State.ITER_IN_FCD_SEGMENT;
                return;
            }
        }
        if (this.state != State.ITER_IN_FCD_SEGMENT) {
            if (this.state == State.IN_NORM_ITER_AT_START) {
                this.iter.moveIndex(this.limit - this.start);
            }
            this.start = this.limit;
        }
        this.state = State.ITER_CHECK_FWD;
    }

    private boolean nextSegment() {
        if (!f54assertionsDisabled) {
            if (!(this.state == State.ITER_CHECK_FWD)) {
                throw new AssertionError();
            }
        }
        this.pos = this.iter.getIndex();
        if (this.s == null) {
            this.s = new StringBuilder();
        } else {
            this.s.setLength(0);
        }
        int prevCC = 0;
        while (true) {
            int c = this.iter.nextCodePoint();
            if (c < 0) {
                break;
            }
            int fcd16 = this.nfcImpl.getFCD16(c);
            int leadCC = fcd16 >> 8;
            if (leadCC == 0 && this.s.length() != 0) {
                this.iter.previousCodePoint();
                break;
            }
            this.s.appendCodePoint(c);
            if (leadCC != 0 && (prevCC > leadCC || CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                break;
            }
            prevCC = fcd16 & 255;
            if (prevCC == 0) {
                break;
            }
        }
        this.limit = this.pos + this.s.length();
        if (!f54assertionsDisabled) {
            if (!(this.pos != this.limit)) {
                throw new AssertionError();
            }
        }
        this.iter.moveIndex(-this.s.length());
        this.state = State.ITER_IN_FCD_SEGMENT;
        return true;
    }

    private void switchToBackward() {
        boolean z = true;
        if (!f54assertionsDisabled) {
            if (this.state != State.ITER_CHECK_FWD && ((this.state != State.ITER_IN_FCD_SEGMENT || this.pos != this.start) && (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) < 0 || this.pos != 0))) {
                z = false;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        if (this.state == State.ITER_CHECK_FWD) {
            int index = this.iter.getIndex();
            this.pos = index;
            this.limit = index;
            if (this.pos == this.start) {
                this.state = State.ITER_CHECK_BWD;
                return;
            } else {
                this.state = State.ITER_IN_FCD_SEGMENT;
                return;
            }
        }
        if (this.state != State.ITER_IN_FCD_SEGMENT) {
            if (this.state == State.IN_NORM_ITER_AT_LIMIT) {
                this.iter.moveIndex(this.start - this.limit);
            }
            this.limit = this.start;
        }
        this.state = State.ITER_CHECK_BWD;
    }

    private boolean previousSegment() {
        if (!f54assertionsDisabled) {
            if (!(this.state == State.ITER_CHECK_BWD)) {
                throw new AssertionError();
            }
        }
        this.pos = this.iter.getIndex();
        if (this.s == null) {
            this.s = new StringBuilder();
        } else {
            this.s.setLength(0);
        }
        int nextCC = 0;
        while (true) {
            int c = this.iter.previousCodePoint();
            if (c < 0) {
                break;
            }
            int fcd16 = this.nfcImpl.getFCD16(c);
            int trailCC = fcd16 & 255;
            if (trailCC == 0 && this.s.length() != 0) {
                this.iter.nextCodePoint();
                break;
            }
            this.s.appendCodePoint(c);
            if (trailCC != 0 && ((nextCC != 0 && trailCC > nextCC) || CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                break;
            }
            nextCC = fcd16 >> 8;
            if (nextCC == 0) {
                break;
            }
        }
        this.start = this.pos - this.s.length();
        if (!f54assertionsDisabled) {
            if (!(this.pos != this.start)) {
                throw new AssertionError();
            }
        }
        this.iter.moveIndex(this.s.length());
        this.state = State.ITER_IN_FCD_SEGMENT;
        return true;
    }

    private void normalize(CharSequence s) {
        if (this.normalized == null) {
            this.normalized = new StringBuilder();
        }
        this.nfcImpl.decompose(s, this.normalized);
    }

    private enum State {
        ITER_CHECK_FWD,
        ITER_CHECK_BWD,
        ITER_IN_FCD_SEGMENT,
        IN_NORM_ITER_AT_LIMIT,
        IN_NORM_ITER_AT_START;

        public static State[] valuesCustom() {
            return values();
        }
    }
}
