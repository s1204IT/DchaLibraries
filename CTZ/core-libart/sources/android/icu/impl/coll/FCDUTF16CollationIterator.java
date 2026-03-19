package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;

public final class FCDUTF16CollationIterator extends UTF16CollationIterator {
    static final boolean $assertionsDisabled = false;
    private static final int rawStart = 0;
    private int checkDir;
    private final Normalizer2Impl nfcImpl;
    private StringBuilder normalized;
    private int rawLimit;
    private CharSequence rawSeq;
    private int segmentLimit;
    private int segmentStart;

    public FCDUTF16CollationIterator(CollationData collationData) {
        super(collationData);
        this.nfcImpl = collationData.nfcImpl;
    }

    public FCDUTF16CollationIterator(CollationData collationData, boolean z, CharSequence charSequence, int i) {
        super(collationData, z, charSequence, i);
        this.rawSeq = charSequence;
        this.segmentStart = i;
        this.rawLimit = charSequence.length();
        this.nfcImpl = collationData.nfcImpl;
        this.checkDir = 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CollationIterator) || !equals(obj) || !(obj instanceof FCDUTF16CollationIterator)) {
            return false;
        }
        FCDUTF16CollationIterator fCDUTF16CollationIterator = (FCDUTF16CollationIterator) obj;
        if (this.checkDir != fCDUTF16CollationIterator.checkDir) {
            return false;
        }
        if (this.checkDir == 0) {
            if ((this.seq == this.rawSeq) != (fCDUTF16CollationIterator.seq == fCDUTF16CollationIterator.rawSeq)) {
                return false;
            }
        }
        return (this.checkDir != 0 || this.seq == this.rawSeq) ? this.pos - 0 == fCDUTF16CollationIterator.pos - 0 : this.segmentStart - 0 == fCDUTF16CollationIterator.segmentStart - 0 && this.pos - this.start == fCDUTF16CollationIterator.pos - fCDUTF16CollationIterator.start;
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public void resetToOffset(int i) {
        reset();
        this.seq = this.rawSeq;
        int i2 = 0 + i;
        this.pos = i2;
        this.segmentStart = i2;
        this.start = i2;
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
    public void setText(boolean z, CharSequence charSequence, int i) {
        super.setText(z, charSequence, i);
        this.rawSeq = charSequence;
        this.segmentStart = i;
        int length = charSequence.length();
        this.limit = length;
        this.rawLimit = length;
        this.checkDir = 1;
    }

    @Override
    public int nextCodePoint() {
        char cCharAt;
        while (true) {
            if (this.checkDir > 0) {
                if (this.pos == this.limit) {
                    return -1;
                }
                CharSequence charSequence = this.seq;
                int i = this.pos;
                this.pos = i + 1;
                cCharAt = charSequence.charAt(i);
                if (CollationFCD.hasTccc(cCharAt) && (CollationFCD.maybeTibetanCompositeVowel(cCharAt) || (this.pos != this.limit && CollationFCD.hasLccc(this.seq.charAt(this.pos))))) {
                    this.pos--;
                    nextSegment();
                    CharSequence charSequence2 = this.seq;
                    int i2 = this.pos;
                    this.pos = i2 + 1;
                    cCharAt = charSequence2.charAt(i2);
                }
            } else {
                if (this.checkDir == 0 && this.pos != this.limit) {
                    CharSequence charSequence3 = this.seq;
                    int i3 = this.pos;
                    this.pos = i3 + 1;
                    cCharAt = charSequence3.charAt(i3);
                    break;
                }
                switchToForward();
            }
        }
        if (Character.isHighSurrogate(cCharAt) && this.pos != this.limit) {
            char cCharAt2 = this.seq.charAt(this.pos);
            if (Character.isLowSurrogate(cCharAt2)) {
                this.pos++;
                return Character.toCodePoint(cCharAt, cCharAt2);
            }
        }
        return cCharAt;
    }

    @Override
    public int previousCodePoint() {
        char cCharAt;
        while (true) {
            if (this.checkDir < 0) {
                if (this.pos == this.start) {
                    return -1;
                }
                CharSequence charSequence = this.seq;
                int i = this.pos - 1;
                this.pos = i;
                cCharAt = charSequence.charAt(i);
                if (CollationFCD.hasLccc(cCharAt) && (CollationFCD.maybeTibetanCompositeVowel(cCharAt) || (this.pos != this.start && CollationFCD.hasTccc(this.seq.charAt(this.pos - 1))))) {
                    this.pos++;
                    previousSegment();
                    CharSequence charSequence2 = this.seq;
                    int i2 = this.pos - 1;
                    this.pos = i2;
                    cCharAt = charSequence2.charAt(i2);
                }
            } else {
                if (this.checkDir == 0 && this.pos != this.start) {
                    CharSequence charSequence3 = this.seq;
                    int i3 = this.pos - 1;
                    this.pos = i3;
                    cCharAt = charSequence3.charAt(i3);
                    break;
                }
                switchToBackward();
            }
        }
        if (Character.isLowSurrogate(cCharAt) && this.pos != this.start) {
            char cCharAt2 = this.seq.charAt(this.pos - 1);
            if (Character.isHighSurrogate(cCharAt2)) {
                this.pos--;
                return Character.toCodePoint(cCharAt2, cCharAt);
            }
        }
        return cCharAt;
    }

    @Override
    protected long handleNextCE32() {
        char cCharAt;
        while (true) {
            if (this.checkDir > 0) {
                if (this.pos == this.limit) {
                    return -4294967104L;
                }
                CharSequence charSequence = this.seq;
                int i = this.pos;
                this.pos = i + 1;
                cCharAt = charSequence.charAt(i);
                if (CollationFCD.hasTccc(cCharAt) && (CollationFCD.maybeTibetanCompositeVowel(cCharAt) || (this.pos != this.limit && CollationFCD.hasLccc(this.seq.charAt(this.pos))))) {
                    this.pos--;
                    nextSegment();
                    CharSequence charSequence2 = this.seq;
                    int i2 = this.pos;
                    this.pos = i2 + 1;
                    cCharAt = charSequence2.charAt(i2);
                }
            } else {
                if (this.checkDir == 0 && this.pos != this.limit) {
                    CharSequence charSequence3 = this.seq;
                    int i3 = this.pos;
                    this.pos = i3 + 1;
                    cCharAt = charSequence3.charAt(i3);
                    break;
                }
                switchToForward();
            }
        }
        return makeCodePointAndCE32Pair(cCharAt, this.trie.getFromU16SingleLead(cCharAt));
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
        int iCharCount;
        int i = this.pos;
        int i2 = 0;
        while (true) {
            int iCodePointAt = Character.codePointAt(this.seq, i);
            iCharCount = Character.charCount(iCodePointAt) + i;
            int fcd16 = this.nfcImpl.getFCD16(iCodePointAt);
            int i3 = fcd16 >> 8;
            if (i3 == 0 && i != this.pos) {
                this.segmentLimit = i;
                this.limit = i;
                break;
            } else {
                if (i3 != 0 && (i2 > i3 || CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                    break;
                }
                i2 = fcd16 & 255;
                if (iCharCount == this.rawLimit || i2 == 0) {
                    break;
                } else {
                    i = iCharCount;
                }
            }
        }
        while (iCharCount != this.rawLimit) {
            int iCodePointAt2 = Character.codePointAt(this.seq, iCharCount);
            int iCharCount2 = Character.charCount(iCodePointAt2) + iCharCount;
            if (this.nfcImpl.getFCD16(iCodePointAt2) <= 255) {
                break;
            } else {
                iCharCount = iCharCount2;
            }
        }
        normalize(this.pos, iCharCount);
        this.pos = this.start;
        this.checkDir = 0;
    }

    private void switchToBackward() {
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
        int iCharCount;
        int i = this.pos;
        int i2 = 0;
        while (true) {
            int iCodePointBefore = Character.codePointBefore(this.seq, i);
            iCharCount = i - Character.charCount(iCodePointBefore);
            int fcd16 = this.nfcImpl.getFCD16(iCodePointBefore);
            int i3 = fcd16 & 255;
            if (i3 == 0 && i != this.pos) {
                this.segmentStart = i;
                this.start = i;
                break;
            } else {
                if (i3 != 0 && ((i2 != 0 && i3 > i2) || CollationFCD.isFCD16OfTibetanCompositeVowel(fcd16))) {
                    break;
                }
                i2 = fcd16 >> 8;
                if (iCharCount == 0 || i2 == 0) {
                    break;
                } else {
                    i = iCharCount;
                }
            }
        }
        this.segmentStart = iCharCount;
        this.start = iCharCount;
        this.checkDir = 0;
    }

    private void normalize(int i, int i2) {
        if (this.normalized == null) {
            this.normalized = new StringBuilder();
        }
        this.nfcImpl.decompose(this.rawSeq, i, i2, this.normalized, i2 - i);
        this.segmentStart = i;
        this.segmentLimit = i2;
        this.seq = this.normalized;
        this.start = 0;
        this.limit = this.start + this.normalized.length();
    }
}
