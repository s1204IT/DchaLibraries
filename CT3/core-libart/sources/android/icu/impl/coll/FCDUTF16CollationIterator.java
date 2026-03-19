package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;

public final class FCDUTF16CollationIterator extends UTF16CollationIterator {

    static final boolean f55assertionsDisabled;
    private static final int rawStart = 0;
    private int checkDir;
    private final Normalizer2Impl nfcImpl;
    private StringBuilder normalized;
    private int rawLimit;
    private CharSequence rawSeq;
    private int segmentLimit;
    private int segmentStart;

    static {
        f55assertionsDisabled = !FCDUTF16CollationIterator.class.desiredAssertionStatus();
    }

    public FCDUTF16CollationIterator(CollationData d) {
        super(d);
        this.nfcImpl = d.nfcImpl;
    }

    public FCDUTF16CollationIterator(CollationData data, boolean numeric, CharSequence s, int p) {
        super(data, numeric, s, p);
        this.rawSeq = s;
        this.segmentStart = p;
        this.rawLimit = s.length();
        this.nfcImpl = data.nfcImpl;
        this.checkDir = 1;
    }

    @Override
    public boolean equals(Object other) {
        if (!equals(other)) {
            return false;
        }
        FCDUTF16CollationIterator o = (FCDUTF16CollationIterator) other;
        if (this.checkDir != o.checkDir) {
            return false;
        }
        if (this.checkDir == 0) {
            if ((this.seq == this.rawSeq) != (o.seq == o.rawSeq)) {
                return false;
            }
        }
        return (this.checkDir != 0 || this.seq == this.rawSeq) ? this.pos + 0 == o.pos + 0 : this.segmentStart + 0 == o.segmentStart + 0 && this.pos - this.start == o.pos - o.start;
    }

    @Override
    public int hashCode() {
        if (f55assertionsDisabled) {
            return 42;
        }
        throw new AssertionError("hashCode not designed");
    }

    @Override
    public void resetToOffset(int newOffset) {
        reset();
        this.seq = this.rawSeq;
        int i = newOffset + 0;
        this.pos = i;
        this.segmentStart = i;
        this.start = i;
        this.limit = this.rawLimit;
        this.checkDir = 1;
    }

    @Override
    public int getOffset() {
        if (this.checkDir != 0 || this.seq == this.rawSeq) {
            return this.pos + 0;
        }
        if (this.pos == this.start) {
            return this.segmentStart + 0;
        }
        return this.segmentLimit + 0;
    }

    @Override
    public void setText(boolean numeric, CharSequence s, int p) {
        super.setText(numeric, s, p);
        this.rawSeq = s;
        this.segmentStart = p;
        int length = s.length();
        this.limit = length;
        this.rawLimit = length;
        this.checkDir = 1;
    }

    @Override
    public int nextCodePoint() {
        char c;
        while (true) {
            if (this.checkDir > 0) {
                if (this.pos == this.limit) {
                    return -1;
                }
                CharSequence charSequence = this.seq;
                int i = this.pos;
                this.pos = i + 1;
                c = charSequence.charAt(i);
                if (CollationFCD.hasTccc(c) && (CollationFCD.maybeTibetanCompositeVowel(c) || (this.pos != this.limit && CollationFCD.hasLccc(this.seq.charAt(this.pos))))) {
                    this.pos--;
                    nextSegment();
                    CharSequence charSequence2 = this.seq;
                    int i2 = this.pos;
                    this.pos = i2 + 1;
                    c = charSequence2.charAt(i2);
                }
            } else {
                if (this.checkDir == 0 && this.pos != this.limit) {
                    CharSequence charSequence3 = this.seq;
                    int i3 = this.pos;
                    this.pos = i3 + 1;
                    c = charSequence3.charAt(i3);
                    break;
                }
                switchToForward();
            }
        }
        if (Character.isHighSurrogate(c) && this.pos != this.limit) {
            char trail = this.seq.charAt(this.pos);
            if (Character.isLowSurrogate(trail)) {
                this.pos++;
                return Character.toCodePoint(c, trail);
            }
        }
        return c;
    }

    @Override
    public int previousCodePoint() {
        char c;
        while (true) {
            if (this.checkDir < 0) {
                if (this.pos == this.start) {
                    return -1;
                }
                CharSequence charSequence = this.seq;
                int i = this.pos - 1;
                this.pos = i;
                c = charSequence.charAt(i);
                if (CollationFCD.hasLccc(c) && (CollationFCD.maybeTibetanCompositeVowel(c) || (this.pos != this.start && CollationFCD.hasTccc(this.seq.charAt(this.pos - 1))))) {
                    this.pos++;
                    previousSegment();
                    CharSequence charSequence2 = this.seq;
                    int i2 = this.pos - 1;
                    this.pos = i2;
                    c = charSequence2.charAt(i2);
                }
            } else {
                if (this.checkDir == 0 && this.pos != this.start) {
                    CharSequence charSequence3 = this.seq;
                    int i3 = this.pos - 1;
                    this.pos = i3;
                    c = charSequence3.charAt(i3);
                    break;
                }
                switchToBackward();
            }
        }
        if (Character.isLowSurrogate(c) && this.pos != this.start) {
            char lead = this.seq.charAt(this.pos - 1);
            if (Character.isHighSurrogate(lead)) {
                this.pos--;
                return Character.toCodePoint(lead, c);
            }
        }
        return c;
    }

    @Override
    protected long handleNextCE32() {
        char c;
        while (true) {
            if (this.checkDir > 0) {
                if (this.pos == this.limit) {
                    return -4294967104L;
                }
                CharSequence charSequence = this.seq;
                int i = this.pos;
                this.pos = i + 1;
                c = charSequence.charAt(i);
                if (CollationFCD.hasTccc(c) && (CollationFCD.maybeTibetanCompositeVowel(c) || (this.pos != this.limit && CollationFCD.hasLccc(this.seq.charAt(this.pos))))) {
                    this.pos--;
                    nextSegment();
                    CharSequence charSequence2 = this.seq;
                    int i2 = this.pos;
                    this.pos = i2 + 1;
                    c = charSequence2.charAt(i2);
                }
            } else {
                if (this.checkDir == 0 && this.pos != this.limit) {
                    CharSequence charSequence3 = this.seq;
                    int i3 = this.pos;
                    this.pos = i3 + 1;
                    c = charSequence3.charAt(i3);
                    break;
                }
                switchToForward();
            }
        }
        return makeCodePointAndCE32Pair(c, this.trie.getFromU16SingleLead(c));
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
        if (!f55assertionsDisabled) {
            if (!((this.checkDir < 0 && this.seq == this.rawSeq) || (this.checkDir == 0 && this.pos == this.limit))) {
                throw new AssertionError();
            }
        }
        if (this.checkDir < 0) {
            int i = this.pos;
            this.segmentStart = i;
            this.start = i;
            if (this.pos == this.segmentLimit) {
                this.limit = this.rawLimit;
                this.checkDir = 1;
                return;
            } else {
                this.checkDir = 0;
                return;
            }
        }
        if (this.seq != this.rawSeq) {
            this.seq = this.rawSeq;
            int i2 = this.segmentLimit;
            this.segmentStart = i2;
            this.start = i2;
            this.pos = i2;
        }
        this.limit = this.rawLimit;
        this.checkDir = 1;
    }

    private void nextSegment() {
        int q;
        int c;
        if (!f55assertionsDisabled) {
            if (!(this.checkDir > 0 && this.seq == this.rawSeq && this.pos != this.limit)) {
                throw new AssertionError();
            }
        }
        int p = this.pos;
        int prevCC = 0;
        do {
            int q2 = p;
            int c2 = Character.codePointAt(this.seq, p);
            p += Character.charCount(c2);
            int fcd16 = this.nfcImpl.getFCD16(c2);
            int leadCC = fcd16 >> 8;
            if (leadCC == 0 && q2 != this.pos) {
                this.segmentLimit = q2;
                this.limit = q2;
                break;
            }
            if (leadCC != 0 && (prevCC > leadCC || CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                do {
                    q = p;
                    if (p == this.rawLimit) {
                        break;
                    }
                    c = Character.codePointAt(this.seq, p);
                    p += Character.charCount(c);
                } while (this.nfcImpl.getFCD16(c) > 255);
                normalize(this.pos, q);
                this.pos = this.start;
            } else {
                prevCC = fcd16 & 255;
                if (p == this.rawLimit) {
                    break;
                }
            }
        } while (prevCC != 0);
        this.segmentLimit = p;
        this.limit = p;
        if (!f55assertionsDisabled) {
            if (!(this.pos != this.limit)) {
                throw new AssertionError();
            }
        }
        this.checkDir = 0;
    }

    private void switchToBackward() {
        boolean z = true;
        if (!f55assertionsDisabled) {
            if ((this.checkDir <= 0 || this.seq != this.rawSeq) && (this.checkDir != 0 || this.pos != this.start)) {
                z = false;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        if (this.checkDir > 0) {
            int i = this.pos;
            this.segmentLimit = i;
            this.limit = i;
            if (this.pos == this.segmentStart) {
                this.start = 0;
                this.checkDir = -1;
                return;
            } else {
                this.checkDir = 0;
                return;
            }
        }
        if (this.seq != this.rawSeq) {
            this.seq = this.rawSeq;
            int i2 = this.segmentStart;
            this.segmentLimit = i2;
            this.limit = i2;
            this.pos = i2;
        }
        this.start = 0;
        this.checkDir = -1;
    }

    private void previousSegment() {
        int q;
        if (!f55assertionsDisabled) {
            if (!(this.checkDir < 0 && this.seq == this.rawSeq && this.pos != this.start)) {
                throw new AssertionError();
            }
        }
        int p = this.pos;
        int nextCC = 0;
        do {
            int q2 = p;
            int c = Character.codePointBefore(this.seq, p);
            p -= Character.charCount(c);
            int fcd16 = this.nfcImpl.getFCD16(c);
            int trailCC = fcd16 & 255;
            if (trailCC == 0 && q2 != this.pos) {
                this.segmentStart = q2;
                this.start = q2;
                break;
            }
            if (trailCC != 0 && ((nextCC != 0 && trailCC > nextCC) || CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                do {
                    q = p;
                    if (fcd16 <= 255 || p == 0) {
                        break;
                    }
                    int c2 = Character.codePointBefore(this.seq, p);
                    p -= Character.charCount(c2);
                    fcd16 = this.nfcImpl.getFCD16(c2);
                } while (fcd16 != 0);
                normalize(q, this.pos);
                this.pos = this.limit;
            } else {
                nextCC = fcd16 >> 8;
                if (p == 0) {
                    break;
                }
            }
        } while (nextCC != 0);
        this.segmentStart = p;
        this.start = p;
        if (!f55assertionsDisabled) {
            if (!(this.pos != this.start)) {
                throw new AssertionError();
            }
        }
        this.checkDir = 0;
    }

    private void normalize(int from, int to) {
        if (this.normalized == null) {
            this.normalized = new StringBuilder();
        }
        this.nfcImpl.decompose(this.rawSeq, from, to, this.normalized, to - from);
        this.segmentStart = from;
        this.segmentLimit = to;
        this.seq = this.normalized;
        this.start = 0;
        this.limit = this.start + this.normalized.length();
    }
}
