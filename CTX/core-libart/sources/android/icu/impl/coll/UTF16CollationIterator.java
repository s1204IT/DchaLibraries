package android.icu.impl.coll;

public class UTF16CollationIterator extends CollationIterator {
    static final boolean $assertionsDisabled = false;
    protected int limit;
    protected int pos;
    protected CharSequence seq;
    protected int start;

    public UTF16CollationIterator(CollationData collationData) {
        super(collationData);
    }

    public UTF16CollationIterator(CollationData collationData, boolean z, CharSequence charSequence, int i) {
        super(collationData, z);
        this.seq = charSequence;
        this.start = 0;
        this.pos = i;
        this.limit = charSequence.length();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        UTF16CollationIterator uTF16CollationIterator = (UTF16CollationIterator) obj;
        return this.pos - this.start == uTF16CollationIterator.pos - uTF16CollationIterator.start;
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public void resetToOffset(int i) {
        reset();
        this.pos = this.start + i;
    }

    @Override
    public int getOffset() {
        return this.pos - this.start;
    }

    public void setText(boolean z, CharSequence charSequence, int i) {
        reset(z);
        this.seq = charSequence;
        this.start = 0;
        this.pos = i;
        this.limit = charSequence.length();
    }

    @Override
    public int nextCodePoint() {
        if (this.pos == this.limit) {
            return -1;
        }
        CharSequence charSequence = this.seq;
        int i = this.pos;
        this.pos = i + 1;
        char cCharAt = charSequence.charAt(i);
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
        if (this.pos == this.start) {
            return -1;
        }
        CharSequence charSequence = this.seq;
        int i = this.pos - 1;
        this.pos = i;
        char cCharAt = charSequence.charAt(i);
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
        if (this.pos == this.limit) {
            return -4294967104L;
        }
        CharSequence charSequence = this.seq;
        int i = this.pos;
        this.pos = i + 1;
        char cCharAt = charSequence.charAt(i);
        return makeCodePointAndCE32Pair(cCharAt, this.trie.getFromU16SingleLead(cCharAt));
    }

    @Override
    protected char handleGetTrailSurrogate() {
        if (this.pos == this.limit) {
            return (char) 0;
        }
        char cCharAt = this.seq.charAt(this.pos);
        if (Character.isLowSurrogate(cCharAt)) {
            this.pos++;
        }
        return cCharAt;
    }

    @Override
    protected void forwardNumCodePoints(int i) {
        while (i > 0 && this.pos != this.limit) {
            CharSequence charSequence = this.seq;
            int i2 = this.pos;
            this.pos = i2 + 1;
            i--;
            if (Character.isHighSurrogate(charSequence.charAt(i2)) && this.pos != this.limit && Character.isLowSurrogate(this.seq.charAt(this.pos))) {
                this.pos++;
            }
        }
    }

    @Override
    protected void backwardNumCodePoints(int i) {
        while (i > 0 && this.pos != this.start) {
            CharSequence charSequence = this.seq;
            int i2 = this.pos - 1;
            this.pos = i2;
            i--;
            if (Character.isLowSurrogate(charSequence.charAt(i2)) && this.pos != this.start && Character.isHighSurrogate(this.seq.charAt(this.pos - 1))) {
                this.pos--;
            }
        }
    }
}
