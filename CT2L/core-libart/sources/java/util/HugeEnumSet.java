package java.util;

import java.lang.Enum;

final class HugeEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private static final int BIT_IN_LONG = 64;
    private long[] bits;
    private final E[] enums;
    private int size;

    HugeEnumSet(Class<E> elementType, E[] enums) {
        super(elementType);
        this.enums = enums;
        this.bits = new long[((enums.length + 64) - 1) / 64];
    }

    private class HugeEnumSetIterator implements Iterator<E> {
        private long currentBits;
        private int index;
        private E last;
        private long mask;

        private HugeEnumSetIterator() {
            this.currentBits = HugeEnumSet.this.bits[0];
            computeNextElement();
        }

        void computeNextElement() {
            while (this.currentBits == 0) {
                int i = this.index + 1;
                this.index = i;
                if (i < HugeEnumSet.this.bits.length) {
                    this.currentBits = HugeEnumSet.this.bits[this.index];
                } else {
                    this.mask = 0L;
                    return;
                }
            }
            this.mask = this.currentBits & (-this.currentBits);
        }

        @Override
        public boolean hasNext() {
            return this.mask != 0;
        }

        @Override
        public E next() {
            if (this.mask == 0) {
                throw new NoSuchElementException();
            }
            this.last = (E) HugeEnumSet.this.enums[Long.numberOfTrailingZeros(this.mask) + (this.index * 64)];
            this.currentBits &= this.mask ^ (-1);
            computeNextElement();
            return this.last;
        }

        @Override
        public void remove() {
            if (this.last == null) {
                throw new IllegalStateException();
            }
            HugeEnumSet.this.remove(this.last);
            this.last = null;
        }
    }

    @Override
    public boolean add(E element) {
        this.elementClass.cast(element);
        int ordinal = element.ordinal();
        int index = ordinal / 64;
        int inBits = ordinal % 64;
        long oldBits = this.bits[index];
        long newBits = oldBits | (1 << inBits);
        if (oldBits == newBits) {
            return false;
        }
        this.bits[index] = newBits;
        this.size++;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (collection.isEmpty() || collection == this) {
            return false;
        }
        if (collection instanceof EnumSet) {
            EnumSet enumSet = (EnumSet) collection;
            enumSet.elementClass.asSubclass(this.elementClass);
            HugeEnumSet hugeEnumSet = (HugeEnumSet) enumSet;
            boolean z = false;
            for (int i = 0; i < this.bits.length; i++) {
                long j = this.bits[i];
                long j2 = j | hugeEnumSet.bits[i];
                if (j != j2) {
                    this.bits[i] = j2;
                    this.size += Long.bitCount(j2) - Long.bitCount(j);
                    z = true;
                }
            }
            return z;
        }
        return super.addAll(collection);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void clear() {
        Arrays.fill(this.bits, 0L);
        this.size = 0;
    }

    @Override
    protected void complement() {
        this.size = 0;
        int length = this.bits.length;
        for (int i = 0; i < length; i++) {
            long b = this.bits[i] ^ (-1);
            if (i == length - 1) {
                b &= (-1) >>> (64 - (this.enums.length % 64));
            }
            this.size += Long.bitCount(b);
            this.bits[i] = b;
        }
    }

    @Override
    public boolean contains(Object object) {
        if (object == null || !isValidType(object.getClass())) {
            return false;
        }
        int ordinal = ((Enum) object).ordinal();
        int index = ordinal / 64;
        int inBits = ordinal % 64;
        return (this.bits[index] & (1 << inBits)) != 0;
    }

    @Override
    public HugeEnumSet<E> mo1clone() {
        HugeEnumSet<E> set = (HugeEnumSet) super.mo1clone();
        set.bits = (long[]) this.bits.clone();
        return set;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if (collection.isEmpty()) {
            return true;
        }
        if (collection instanceof HugeEnumSet) {
            HugeEnumSet<?> set = (HugeEnumSet) collection;
            if (isValidType(set.elementClass)) {
                for (int i = 0; i < this.bits.length; i++) {
                    long setBits = set.bits[i];
                    if ((this.bits[i] & setBits) != setBits) {
                        return false;
                    }
                }
                return true;
            }
        }
        return !(collection instanceof EnumSet) && super.containsAll(collection);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (!isValidType(object.getClass())) {
            return super.equals(object);
        }
        return Arrays.equals(this.bits, ((HugeEnumSet) object).bits);
    }

    @Override
    public Iterator<E> iterator() {
        return new HugeEnumSetIterator();
    }

    @Override
    public boolean remove(Object object) {
        if (object == null || !isValidType(object.getClass())) {
            return false;
        }
        int ordinal = ((Enum) object).ordinal();
        int index = ordinal / 64;
        int inBits = ordinal % 64;
        long oldBits = this.bits[index];
        long newBits = oldBits & ((1 << inBits) ^ (-1));
        if (oldBits == newBits) {
            return false;
        }
        this.bits[index] = newBits;
        this.size--;
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        if (collection.isEmpty()) {
            return false;
        }
        if (collection instanceof EnumSet) {
            EnumSet<?> set = (EnumSet) collection;
            if (!isValidType(set.elementClass)) {
                return false;
            }
            HugeEnumSet<E> hugeSet = (HugeEnumSet) set;
            boolean changed = false;
            for (int i = 0; i < this.bits.length; i++) {
                long oldBits = this.bits[i];
                long newBits = oldBits & (hugeSet.bits[i] ^ (-1));
                if (oldBits != newBits) {
                    this.bits[i] = newBits;
                    this.size += Long.bitCount(newBits) - Long.bitCount(oldBits);
                    changed = true;
                }
            }
            return changed;
        }
        boolean changed2 = super.removeAll(collection);
        return changed2;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        if (collection instanceof EnumSet) {
            EnumSet<?> set = (EnumSet) collection;
            if (!isValidType(set.elementClass)) {
                if (this.size > 0) {
                    clear();
                    return true;
                }
                return false;
            }
            HugeEnumSet<E> hugeSet = (HugeEnumSet) set;
            boolean changed = false;
            for (int i = 0; i < this.bits.length; i++) {
                long oldBits = this.bits[i];
                long newBits = oldBits & hugeSet.bits[i];
                if (oldBits != newBits) {
                    this.bits[i] = newBits;
                    this.size += Long.bitCount(newBits) - Long.bitCount(oldBits);
                    changed = true;
                }
            }
            return changed;
        }
        boolean changed2 = super.retainAll(collection);
        return changed2;
    }

    @Override
    void setRange(E start, E end) {
        int startOrdinal = start.ordinal();
        int startIndex = startOrdinal / 64;
        int startInBits = startOrdinal % 64;
        int endOrdinal = end.ordinal();
        int endIndex = endOrdinal / 64;
        int endInBits = endOrdinal % 64;
        if (startIndex == endIndex) {
            long range = ((-1) >>> (64 - ((endInBits - startInBits) + 1))) << startInBits;
            this.size -= Long.bitCount(this.bits[startIndex]);
            long[] jArr = this.bits;
            jArr[startIndex] = jArr[startIndex] | range;
            this.size += Long.bitCount(this.bits[startIndex]);
            return;
        }
        long range2 = ((-1) >>> startInBits) << startInBits;
        this.size -= Long.bitCount(this.bits[startIndex]);
        long[] jArr2 = this.bits;
        jArr2[startIndex] = jArr2[startIndex] | range2;
        this.size += Long.bitCount(this.bits[startIndex]);
        long range3 = (-1) >>> (64 - (endInBits + 1));
        this.size -= Long.bitCount(this.bits[endIndex]);
        long[] jArr3 = this.bits;
        jArr3[endIndex] = jArr3[endIndex] | range3;
        this.size += Long.bitCount(this.bits[endIndex]);
        for (int i = startIndex + 1; i <= endIndex - 1; i++) {
            this.size -= Long.bitCount(this.bits[i]);
            this.bits[i] = -1;
            this.size += Long.bitCount(this.bits[i]);
        }
    }
}
