package java.text;

public final class CollationElementIterator {
    public static final int NULLORDER = -1;
    private android.icu.text.CollationElementIterator icuIterator;

    CollationElementIterator(android.icu.text.CollationElementIterator collationElementIterator) {
        this.icuIterator = collationElementIterator;
    }

    public void reset() {
        this.icuIterator.reset();
    }

    public int next() {
        return this.icuIterator.next();
    }

    public int previous() {
        return this.icuIterator.previous();
    }

    public static final int primaryOrder(int i) {
        return android.icu.text.CollationElementIterator.primaryOrder(i);
    }

    public static final short secondaryOrder(int i) {
        return (short) android.icu.text.CollationElementIterator.secondaryOrder(i);
    }

    public static final short tertiaryOrder(int i) {
        return (short) android.icu.text.CollationElementIterator.tertiaryOrder(i);
    }

    public void setOffset(int i) {
        this.icuIterator.setOffset(i);
    }

    public int getOffset() {
        return this.icuIterator.getOffset();
    }

    public int getMaxExpansion(int i) {
        return this.icuIterator.getMaxExpansion(i);
    }

    public void setText(String str) {
        this.icuIterator.setText(str);
    }

    public void setText(CharacterIterator characterIterator) {
        this.icuIterator.setText(characterIterator);
    }
}
