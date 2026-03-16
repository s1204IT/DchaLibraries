package libcore.icu;

import java.text.CharacterIterator;

public final class CollationElementIteratorICU {
    private static final int PRIMARY_ORDER_MASK_ = -65536;
    private static final int PRIMARY_ORDER_SHIFT_ = 16;
    private static final int SECONDARY_ORDER_MASK_ = 65280;
    private static final int SECONDARY_ORDER_SHIFT_ = 8;
    private static final int TERTIARY_ORDER_MASK_ = 255;
    private static final int UNSIGNED_16_BIT_MASK_ = 65535;
    private final long address;

    public void reset() {
        NativeCollation.reset(this.address);
    }

    public int next() {
        return NativeCollation.next(this.address);
    }

    public int previous() {
        return NativeCollation.previous(this.address);
    }

    public int getMaxExpansion(int order) {
        return NativeCollation.getMaxExpansion(this.address, order);
    }

    public void setText(String source) {
        NativeCollation.setText(this.address, source);
    }

    public void setText(CharacterIterator source) {
        NativeCollation.setText(this.address, source.toString());
    }

    public int getOffset() {
        return NativeCollation.getOffset(this.address);
    }

    public void setOffset(int offset) {
        NativeCollation.setOffset(this.address, offset);
    }

    public static int primaryOrder(int order) {
        return ((PRIMARY_ORDER_MASK_ & order) >> 16) & 65535;
    }

    public static int secondaryOrder(int order) {
        return (SECONDARY_ORDER_MASK_ & order) >> 8;
    }

    public static int tertiaryOrder(int order) {
        return order & 255;
    }

    public static CollationElementIteratorICU getInstance(long collatorAddress, String source) {
        long iteratorAddress = NativeCollation.getCollationElementIterator(collatorAddress, source);
        return new CollationElementIteratorICU(iteratorAddress);
    }

    private CollationElementIteratorICU(long address) {
        this.address = address;
    }

    protected void finalize() throws Throwable {
        try {
            NativeCollation.closeElements(this.address);
        } finally {
            super.finalize();
        }
    }
}
