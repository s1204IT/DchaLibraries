package java.util;

import java.lang.Enum;

final class MiniEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private static final int MAX_ELEMENTS = 64;
    private long bits;
    private final E[] enums;
    private int size;

    MiniEnumSet(Class<E> elementType, E[] enums) {
        super(elementType);
        this.enums = enums;
    }

    private class MiniEnumSetIterator implements Iterator<E> {
        private long currentBits;
        private E last;
        private long mask;

        private MiniEnumSetIterator() {
            this.currentBits = MiniEnumSet.this.bits;
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
            this.last = (E) MiniEnumSet.this.enums[Long.numberOfTrailingZeros(this.mask)];
            this.currentBits &= this.mask ^ (-1);
            this.mask = this.currentBits & (-this.currentBits);
            return this.last;
        }

        @Override
        public void remove() {
            if (this.last == null) {
                throw new IllegalStateException();
            }
            MiniEnumSet.this.remove(this.last);
            this.last = null;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new MiniEnumSetIterator();
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void clear() {
        this.bits = 0L;
        this.size = 0;
    }

    @Override
    public boolean add(E element) {
        this.elementClass.cast(element);
        long oldBits = this.bits;
        long newBits = oldBits | (1 << element.ordinal());
        if (oldBits == newBits) {
            return false;
        }
        this.bits = newBits;
        this.size++;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (collection.isEmpty()) {
            return false;
        }
        if (collection instanceof EnumSet) {
            EnumSet enumSet = (EnumSet) collection;
            enumSet.elementClass.asSubclass(this.elementClass);
            long j = this.bits;
            long j2 = j | ((MiniEnumSet) enumSet).bits;
            this.bits = j2;
            this.size = Long.bitCount(j2);
            return j != j2;
        }
        return super.addAll(collection);
    }

    @Override
    public boolean contains(Object object) {
        if (object == null || !isValidType(object.getClass())) {
            return false;
        }
        Enum<E> element = (Enum) object;
        int ordinal = element.ordinal();
        return (this.bits & (1 << ordinal)) != 0;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if (collection.isEmpty()) {
            return true;
        }
        if (!(collection instanceof MiniEnumSet)) {
            return !(collection instanceof EnumSet) && super.containsAll(collection);
        }
        MiniEnumSet<?> set = (MiniEnumSet) collection;
        long setBits = set.bits;
        return isValidType(set.elementClass) && (this.bits & setBits) == setBits;
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
            MiniEnumSet<E> miniSet = (MiniEnumSet) set;
            long oldBits = this.bits;
            long newBits = oldBits & (miniSet.bits ^ (-1));
            if (oldBits == newBits) {
                return false;
            }
            this.bits = newBits;
            this.size = Long.bitCount(newBits);
            return true;
        }
        return super.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        if (collection instanceof EnumSet) {
            EnumSet<?> set = (EnumSet) collection;
            if (!isValidType(set.elementClass)) {
                if (this.size <= 0) {
                    return false;
                }
                clear();
                return true;
            }
            MiniEnumSet<E> miniSet = (MiniEnumSet) set;
            long oldBits = this.bits;
            long newBits = oldBits & miniSet.bits;
            if (oldBits == newBits) {
                return false;
            }
            this.bits = newBits;
            this.size = Long.bitCount(newBits);
            return true;
        }
        return super.retainAll(collection);
    }

    @Override
    public boolean remove(Object object) {
        if (object == null || !isValidType(object.getClass())) {
            return false;
        }
        Enum<E> element = (Enum) object;
        int ordinal = element.ordinal();
        long oldBits = this.bits;
        long newBits = oldBits & ((1 << ordinal) ^ (-1));
        if (oldBits == newBits) {
            return false;
        }
        this.bits = newBits;
        this.size--;
        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EnumSet)) {
            return super.equals(object);
        }
        EnumSet<?> set = (EnumSet) object;
        return !isValidType(set.elementClass) ? this.size == 0 && set.isEmpty() : this.bits == ((MiniEnumSet) set).bits;
    }

    @Override
    void complement() {
        if (this.enums.length != 0) {
            this.bits ^= -1;
            this.bits &= (-1) >>> (64 - this.enums.length);
            this.size = this.enums.length - this.size;
        }
    }

    @Override
    void setRange(E start, E end) {
        int length = (end.ordinal() - start.ordinal()) + 1;
        long range = ((-1) >>> (64 - length)) << start.ordinal();
        this.bits |= range;
        this.size = Long.bitCount(this.bits);
    }
}
