package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;
import android.icu.text.UCharacterIterator;

public final class FCDIterCollationIterator extends IterCollationIterator {
    static final boolean $assertionsDisabled = false;
    private int limit;
    private final Normalizer2Impl nfcImpl;
    private StringBuilder normalized;
    private int pos;
    private StringBuilder s;
    private int start;
    private State state;

    private enum State {
        ITER_CHECK_FWD,
        ITER_CHECK_BWD,
        ITER_IN_FCD_SEGMENT,
        IN_NORM_ITER_AT_LIMIT,
        IN_NORM_ITER_AT_START
    }

    public FCDIterCollationIterator(CollationData collationData, boolean z, UCharacterIterator uCharacterIterator, int i) {
        super(collationData, z, uCharacterIterator);
        this.state = State.ITER_CHECK_FWD;
        this.start = i;
        this.nfcImpl = collationData.nfcImpl;
    }

    @Override
    public void resetToOffset(int i) {
        super.resetToOffset(i);
        this.start = i;
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
                int next = this.iter.next();
                if (next < 0) {
                    return next;
                }
                if (!CollationFCD.hasTccc(next) || (!CollationFCD.maybeTibetanCompositeVowel(next) && !CollationFCD.hasLccc(this.iter.current()))) {
                    break;
                }
                this.iter.previous();
                if (!nextSegment()) {
                    return -1;
                }
            } else {
                if (this.state == State.ITER_IN_FCD_SEGMENT && this.pos != this.limit) {
                    int iNextCodePoint = this.iter.nextCodePoint();
                    this.pos += Character.charCount(iNextCodePoint);
                    return iNextCodePoint;
                }
                if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) >= 0 && this.pos != this.normalized.length()) {
                    int iCodePointAt = this.normalized.codePointAt(this.pos);
                    this.pos += Character.charCount(iCodePointAt);
                    return iCodePointAt;
                }
                switchToForward();
            }
        }
    }

    @Override
    public int previousCodePoint() {
        int iPrevious;
        while (true) {
            if (this.state == State.ITER_CHECK_BWD) {
                int iPrevious2 = this.iter.previous();
                if (iPrevious2 < 0) {
                    this.pos = 0;
                    this.start = 0;
                    this.state = State.ITER_IN_FCD_SEGMENT;
                    return -1;
                }
                if (!CollationFCD.hasLccc(iPrevious2)) {
                    break;
                }
                if (!CollationFCD.maybeTibetanCompositeVowel(iPrevious2)) {
                    iPrevious = this.iter.previous();
                    if (!CollationFCD.hasTccc(iPrevious)) {
                        if (isTrailSurrogate(iPrevious2)) {
                            if (iPrevious < 0) {
                                iPrevious = this.iter.previous();
                            }
                            if (isLeadSurrogate(iPrevious)) {
                                return Character.toCodePoint((char) iPrevious, (char) iPrevious2);
                            }
                        }
                        if (iPrevious >= 0) {
                            this.iter.next();
                        }
                    }
                } else {
                    iPrevious = -1;
                }
                this.iter.next();
                if (iPrevious >= 0) {
                    this.iter.next();
                }
                if (!previousSegment()) {
                    return -1;
                }
            } else {
                if (this.state == State.ITER_IN_FCD_SEGMENT && this.pos != this.start) {
                    int iPreviousCodePoint = this.iter.previousCodePoint();
                    this.pos -= Character.charCount(iPreviousCodePoint);
                    return iPreviousCodePoint;
                }
                if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) >= 0 && this.pos != 0) {
                    int iCodePointBefore = this.normalized.codePointBefore(this.pos);
                    this.pos -= Character.charCount(iCodePointBefore);
                    return iCodePointBefore;
                }
                switchToBackward();
            }
        }
    }

    @Override
    protected long handleNextCE32() {
        int next;
        while (true) {
            if (this.state == State.ITER_CHECK_FWD) {
                next = this.iter.next();
                if (next < 0) {
                    return -4294967104L;
                }
                if (!CollationFCD.hasTccc(next) || (!CollationFCD.maybeTibetanCompositeVowel(next) && !CollationFCD.hasLccc(this.iter.current()))) {
                    break;
                }
                this.iter.previous();
                if (!nextSegment()) {
                    return 192L;
                }
            } else {
                if (this.state == State.ITER_IN_FCD_SEGMENT && this.pos != this.limit) {
                    next = this.iter.next();
                    this.pos++;
                    break;
                }
                if (this.state.compareTo(State.IN_NORM_ITER_AT_LIMIT) >= 0 && this.pos != this.normalized.length()) {
                    StringBuilder sb = this.normalized;
                    int i = this.pos;
                    this.pos = i + 1;
                    next = sb.charAt(i);
                    break;
                }
                switchToForward();
            }
        }
        return makeCodePointAndCE32Pair(next, this.trie.getFromU16SingleLead((char) next));
    }

    @Override
    protected char handleGetTrailSurrogate() {
        if (this.state.compareTo(State.ITER_IN_FCD_SEGMENT) <= 0) {
            int next = this.iter.next();
            if (isTrailSurrogate(next)) {
                if (this.state == State.ITER_IN_FCD_SEGMENT) {
                    this.pos++;
                }
            } else if (next >= 0) {
                this.iter.previous();
            }
            return (char) next;
        }
        char cCharAt = this.normalized.charAt(this.pos);
        if (Character.isLowSurrogate(cCharAt)) {
            this.pos++;
        }
        return cCharAt;
    }

    @Override
    protected void forwardNumCodePoints(int i) {
        while (i > 0 && nextCodePoint() >= 0) {
            i--;
        }
    }

    @Override
    protected void backwardNumCodePoints(int i) {
        while (i > 0 && previousCodePoint() >= 0) {
            i--;
        }
    }

    private void switchToForward() {
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
        this.pos = this.iter.getIndex();
        if (this.s == null) {
            this.s = new StringBuilder();
        } else {
            this.s.setLength(0);
        }
        int i = 0;
        while (true) {
            int iNextCodePoint = this.iter.nextCodePoint();
            if (iNextCodePoint < 0) {
                break;
            }
            int fcd16 = this.nfcImpl.getFCD16(iNextCodePoint);
            int i2 = fcd16 >> 8;
            if (i2 == 0 && this.s.length() != 0) {
                this.iter.previousCodePoint();
                break;
            }
            this.s.appendCodePoint(iNextCodePoint);
            if (i2 != 0 && (i > i2 || CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                break;
            }
            i = fcd16 & 255;
            if (i == 0) {
                break;
            }
        }
        this.limit = this.pos + this.s.length();
        this.iter.moveIndex(-this.s.length());
        this.state = State.ITER_IN_FCD_SEGMENT;
        return true;
    }

    private void switchToBackward() {
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
        int fcd16;
        int iPreviousCodePoint;
        this.pos = this.iter.getIndex();
        int i = 0;
        if (this.s == null) {
            this.s = new StringBuilder();
        } else {
            this.s.setLength(0);
        }
        while (true) {
            int iPreviousCodePoint2 = this.iter.previousCodePoint();
            if (iPreviousCodePoint2 < 0) {
                break;
            }
            fcd16 = this.nfcImpl.getFCD16(iPreviousCodePoint2);
            int i2 = fcd16 & 255;
            if (i2 == 0 && this.s.length() != 0) {
                this.iter.nextCodePoint();
                break;
            }
            this.s.appendCodePoint(iPreviousCodePoint2);
            if (i2 != 0 && ((i != 0 && i2 > i) || CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                break;
            }
            i = fcd16 >> 8;
            if (i == 0) {
                break;
            }
        }
        while (true) {
            if (fcd16 <= 255 || (iPreviousCodePoint = this.iter.previousCodePoint()) < 0) {
                break;
            }
            fcd16 = this.nfcImpl.getFCD16(iPreviousCodePoint);
            if (fcd16 == 0) {
                this.iter.nextCodePoint();
                break;
            }
            this.s.appendCodePoint(iPreviousCodePoint);
        }
        this.s.reverse();
        normalize(this.s);
        this.limit = this.pos;
        this.start = this.pos - this.s.length();
        this.state = State.IN_NORM_ITER_AT_START;
        this.pos = this.normalized.length();
        return true;
    }

    private void normalize(CharSequence charSequence) {
        if (this.normalized == null) {
            this.normalized = new StringBuilder();
        }
        this.nfcImpl.decompose(charSequence, this.normalized);
    }
}
