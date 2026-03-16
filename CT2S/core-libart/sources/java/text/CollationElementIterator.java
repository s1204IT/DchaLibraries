package java.text;

import libcore.icu.CollationElementIteratorICU;

public final class CollationElementIterator {
    public static final int NULLORDER = -1;
    private CollationElementIteratorICU icuIterator;

    CollationElementIterator(CollationElementIteratorICU iterator) {
        this.icuIterator = iterator;
    }

    public int getMaxExpansion(int order) {
        return this.icuIterator.getMaxExpansion(order);
    }

    public int getOffset() {
        return this.icuIterator.getOffset();
    }

    public int next() {
        return this.icuIterator.next();
    }

    public int previous() {
        return this.icuIterator.previous();
    }

    public static final int primaryOrder(int order) {
        return CollationElementIteratorICU.primaryOrder(order);
    }

    public void reset() {
        this.icuIterator.reset();
    }

    public static final short secondaryOrder(int order) {
        return (short) CollationElementIteratorICU.secondaryOrder(order);
    }

    public void setOffset(int newOffset) {
        this.icuIterator.setOffset(newOffset);
    }

    public void setText(CharacterIterator source) {
        this.icuIterator.setText(source);
    }

    public void setText(String source) {
        this.icuIterator.setText(source);
    }

    public static final short tertiaryOrder(int order) {
        return (short) CollationElementIteratorICU.tertiaryOrder(order);
    }
}
