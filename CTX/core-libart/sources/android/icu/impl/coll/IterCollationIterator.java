package android.icu.impl.coll;

import android.icu.text.UCharacterIterator;

public class IterCollationIterator extends CollationIterator {
    protected UCharacterIterator iter;

    public IterCollationIterator(CollationData collationData, boolean z, UCharacterIterator uCharacterIterator) {
        super(collationData, z);
        this.iter = uCharacterIterator;
    }

    @Override
    public void resetToOffset(int i) {
        reset();
        this.iter.setIndex(i);
    }

    @Override
    public int getOffset() {
        return this.iter.getIndex();
    }

    @Override
    public int nextCodePoint() {
        return this.iter.nextCodePoint();
    }

    @Override
    public int previousCodePoint() {
        return this.iter.previousCodePoint();
    }

    @Override
    protected long handleNextCE32() {
        int next = this.iter.next();
        if (next < 0) {
            return -4294967104L;
        }
        return makeCodePointAndCE32Pair(next, this.trie.getFromU16SingleLead((char) next));
    }

    @Override
    protected char handleGetTrailSurrogate() {
        int next = this.iter.next();
        if (!isTrailSurrogate(next) && next >= 0) {
            this.iter.previous();
        }
        return (char) next;
    }

    @Override
    protected void forwardNumCodePoints(int i) {
        this.iter.moveCodePointIndex(i);
    }

    @Override
    protected void backwardNumCodePoints(int i) {
        this.iter.moveCodePointIndex(-i);
    }
}
