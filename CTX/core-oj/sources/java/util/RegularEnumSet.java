package java.util;

import java.lang.Enum;

class RegularEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private static final long serialVersionUID = 3411599620347842686L;
    private long elements;

    static long access$074(RegularEnumSet regularEnumSet, long j) {
        long j2 = j & regularEnumSet.elements;
        regularEnumSet.elements = j2;
        return j2;
    }

    RegularEnumSet(Class<E> cls, Enum<?>[] enumArr) {
        super(cls, enumArr);
        this.elements = 0L;
    }

    @Override
    void addRange(E e, E e2) {
        this.elements = ((-1) >>> ((e.ordinal() - e2.ordinal()) - 1)) << e.ordinal();
    }

    @Override
    void addAll() {
        if (this.universe.length != 0) {
            this.elements = (-1) >>> (-this.universe.length);
        }
    }

    @Override
    void complement() {
        if (this.universe.length != 0) {
            this.elements = ~this.elements;
            this.elements &= (-1) >>> (-this.universe.length);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new EnumSetIterator();
    }

    private class EnumSetIterator<E extends Enum<E>> implements Iterator<E> {
        long lastReturned = 0;
        long unseen;

        EnumSetIterator() {
            this.unseen = RegularEnumSet.this.elements;
        }

        @Override
        public boolean hasNext() {
            return this.unseen != 0;
        }

        @Override
        public E next() {
            if (this.unseen == 0) {
                throw new NoSuchElementException();
            }
            this.lastReturned = this.unseen & (-this.unseen);
            this.unseen -= this.lastReturned;
            return (E) RegularEnumSet.this.universe[Long.numberOfTrailingZeros(this.lastReturned)];
        }

        @Override
        public void remove() {
            if (this.lastReturned == 0) {
                throw new IllegalStateException();
            }
            RegularEnumSet.access$074(RegularEnumSet.this, ~this.lastReturned);
            this.lastReturned = 0L;
        }
    }

    @Override
    public int size() {
        return Long.bitCount(this.elements);
    }

    @Override
    public boolean isEmpty() {
        return this.elements == 0;
    }

    @Override
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> cls = obj.getClass();
        return (cls == this.elementType || cls.getSuperclass() == this.elementType) && (this.elements & (1 << ((Enum) obj).ordinal())) != 0;
    }

    @Override
    public boolean add(E e) {
        typeCheck(e);
        long j = this.elements;
        this.elements |= 1 << e.ordinal();
        return this.elements != j;
    }

    @Override
    public boolean remove(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> cls = obj.getClass();
        if (cls != this.elementType && cls.getSuperclass() != this.elementType) {
            return false;
        }
        long j = this.elements;
        this.elements &= ~(1 << ((Enum) obj).ordinal());
        return this.elements != j;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if (!(collection instanceof RegularEnumSet)) {
            return super.containsAll(collection);
        }
        RegularEnumSet regularEnumSet = (RegularEnumSet) collection;
        if (regularEnumSet.elementType != this.elementType) {
            return regularEnumSet.isEmpty();
        }
        return (regularEnumSet.elements & (~this.elements)) == 0;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (!(collection instanceof RegularEnumSet)) {
            return super.addAll(collection);
        }
        RegularEnumSet regularEnumSet = (RegularEnumSet) collection;
        if (regularEnumSet.elementType != this.elementType) {
            if (regularEnumSet.isEmpty()) {
                return false;
            }
            throw new ClassCastException(((Object) regularEnumSet.elementType) + " != " + ((Object) this.elementType));
        }
        long j = this.elements;
        this.elements |= regularEnumSet.elements;
        return this.elements != j;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        if (!(collection instanceof RegularEnumSet)) {
            return super.removeAll(collection);
        }
        RegularEnumSet regularEnumSet = (RegularEnumSet) collection;
        if (regularEnumSet.elementType != this.elementType) {
            return false;
        }
        long j = this.elements;
        this.elements &= ~regularEnumSet.elements;
        return this.elements != j;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        if (!(collection instanceof RegularEnumSet)) {
            return super.retainAll(collection);
        }
        RegularEnumSet regularEnumSet = (RegularEnumSet) collection;
        if (regularEnumSet.elementType != this.elementType) {
            boolean z = this.elements != 0;
            this.elements = 0L;
            return z;
        }
        long j = this.elements;
        this.elements &= regularEnumSet.elements;
        return this.elements != j;
    }

    @Override
    public void clear() {
        this.elements = 0L;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RegularEnumSet)) {
            return super.equals(obj);
        }
        RegularEnumSet regularEnumSet = (RegularEnumSet) obj;
        return regularEnumSet.elementType != this.elementType ? this.elements == 0 && regularEnumSet.elements == 0 : regularEnumSet.elements == this.elements;
    }
}
