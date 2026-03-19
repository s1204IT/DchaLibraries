package java.util;

import java.lang.Enum;

class JumboEnumSet<E extends Enum<E>> extends EnumSet<E> {
    private static final long serialVersionUID = 334349849919042784L;
    private long[] elements;
    private int size;

    static int access$110(JumboEnumSet jumboEnumSet) {
        int i = jumboEnumSet.size;
        jumboEnumSet.size = i - 1;
        return i;
    }

    JumboEnumSet(Class<E> cls, Enum<?>[] enumArr) {
        super(cls, enumArr);
        this.size = 0;
        this.elements = new long[(enumArr.length + 63) >>> 6];
    }

    @Override
    void addRange(E e, E e2) {
        int iOrdinal = e.ordinal() >>> 6;
        int iOrdinal2 = e2.ordinal() >>> 6;
        if (iOrdinal != iOrdinal2) {
            this.elements[iOrdinal] = (-1) << e.ordinal();
            while (true) {
                iOrdinal++;
                if (iOrdinal >= iOrdinal2) {
                    break;
                } else {
                    this.elements[iOrdinal] = -1;
                }
            }
            this.elements[iOrdinal2] = (-1) >>> (63 - e2.ordinal());
        } else {
            this.elements[iOrdinal] = ((-1) >>> ((e.ordinal() - e2.ordinal()) - 1)) << e.ordinal();
        }
        this.size = (e2.ordinal() - e.ordinal()) + 1;
    }

    @Override
    void addAll() {
        for (int i = 0; i < this.elements.length; i++) {
            this.elements[i] = -1;
        }
        long[] jArr = this.elements;
        int length = this.elements.length - 1;
        jArr[length] = jArr[length] >>> (-this.universe.length);
        this.size = this.universe.length;
    }

    @Override
    void complement() {
        for (int i = 0; i < this.elements.length; i++) {
            this.elements[i] = ~this.elements[i];
        }
        long[] jArr = this.elements;
        int length = this.elements.length - 1;
        jArr[length] = jArr[length] & ((-1) >>> (-this.universe.length));
        this.size = this.universe.length - this.size;
    }

    @Override
    public Iterator<E> iterator() {
        return new EnumSetIterator();
    }

    private class EnumSetIterator<E extends Enum<E>> implements Iterator<E> {
        long unseen;
        int unseenIndex = 0;
        long lastReturned = 0;
        int lastReturnedIndex = 0;

        EnumSetIterator() {
            this.unseen = JumboEnumSet.this.elements[0];
        }

        @Override
        public boolean hasNext() {
            while (this.unseen == 0 && this.unseenIndex < JumboEnumSet.this.elements.length - 1) {
                long[] jArr = JumboEnumSet.this.elements;
                int i = this.unseenIndex + 1;
                this.unseenIndex = i;
                this.unseen = jArr[i];
            }
            return this.unseen != 0;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.lastReturned = this.unseen & (-this.unseen);
            this.lastReturnedIndex = this.unseenIndex;
            this.unseen -= this.lastReturned;
            return (E) JumboEnumSet.this.universe[(this.lastReturnedIndex << 6) + Long.numberOfTrailingZeros(this.lastReturned)];
        }

        @Override
        public void remove() {
            if (this.lastReturned != 0) {
                long j = JumboEnumSet.this.elements[this.lastReturnedIndex];
                long[] jArr = JumboEnumSet.this.elements;
                int i = this.lastReturnedIndex;
                jArr[i] = jArr[i] & (~this.lastReturned);
                if (j != JumboEnumSet.this.elements[this.lastReturnedIndex]) {
                    JumboEnumSet.access$110(JumboEnumSet.this);
                }
                this.lastReturned = 0L;
                return;
            }
            throw new IllegalStateException();
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> cls = obj.getClass();
        if (cls != this.elementType && cls.getSuperclass() != this.elementType) {
            return false;
        }
        int iOrdinal = ((Enum) obj).ordinal();
        return (this.elements[iOrdinal >>> 6] & (1 << iOrdinal)) != 0;
    }

    @Override
    public boolean add(E e) {
        boolean z;
        typeCheck(e);
        int iOrdinal = e.ordinal();
        int i = iOrdinal >>> 6;
        long j = this.elements[i];
        long[] jArr = this.elements;
        jArr[i] = jArr[i] | (1 << iOrdinal);
        if (this.elements[i] == j) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            this.size++;
        }
        return z;
    }

    @Override
    public boolean remove(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        Class<?> cls = obj.getClass();
        if (cls != this.elementType && cls.getSuperclass() != this.elementType) {
            return false;
        }
        int iOrdinal = ((Enum) obj).ordinal();
        int i = iOrdinal >>> 6;
        long j = this.elements[i];
        long[] jArr = this.elements;
        jArr[i] = jArr[i] & (~(1 << iOrdinal));
        if (this.elements[i] != j) {
            z = true;
        }
        if (z) {
            this.size--;
        }
        return z;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if (!(collection instanceof JumboEnumSet)) {
            return super.containsAll(collection);
        }
        JumboEnumSet jumboEnumSet = (JumboEnumSet) collection;
        if (jumboEnumSet.elementType != this.elementType) {
            return jumboEnumSet.isEmpty();
        }
        for (int i = 0; i < this.elements.length; i++) {
            if ((jumboEnumSet.elements[i] & (~this.elements[i])) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        if (!(collection instanceof JumboEnumSet)) {
            return super.addAll(collection);
        }
        JumboEnumSet jumboEnumSet = (JumboEnumSet) collection;
        if (jumboEnumSet.elementType != this.elementType) {
            if (jumboEnumSet.isEmpty()) {
                return false;
            }
            throw new ClassCastException(((Object) jumboEnumSet.elementType) + " != " + ((Object) this.elementType));
        }
        for (int i = 0; i < this.elements.length; i++) {
            long[] jArr = this.elements;
            jArr[i] = jArr[i] | jumboEnumSet.elements[i];
        }
        return recalculateSize();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        if (!(collection instanceof JumboEnumSet)) {
            return super.removeAll(collection);
        }
        JumboEnumSet jumboEnumSet = (JumboEnumSet) collection;
        if (jumboEnumSet.elementType != this.elementType) {
            return false;
        }
        for (int i = 0; i < this.elements.length; i++) {
            long[] jArr = this.elements;
            jArr[i] = jArr[i] & (~jumboEnumSet.elements[i]);
        }
        return recalculateSize();
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        if (!(collection instanceof JumboEnumSet)) {
            return super.retainAll(collection);
        }
        JumboEnumSet jumboEnumSet = (JumboEnumSet) collection;
        if (jumboEnumSet.elementType != this.elementType) {
            boolean z = this.size != 0;
            clear();
            return z;
        }
        for (int i = 0; i < this.elements.length; i++) {
            long[] jArr = this.elements;
            jArr[i] = jArr[i] & jumboEnumSet.elements[i];
        }
        return recalculateSize();
    }

    @Override
    public void clear() {
        Arrays.fill(this.elements, 0L);
        this.size = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JumboEnumSet)) {
            return super.equals(obj);
        }
        JumboEnumSet jumboEnumSet = (JumboEnumSet) obj;
        if (jumboEnumSet.elementType != this.elementType) {
            return this.size == 0 && jumboEnumSet.size == 0;
        }
        return Arrays.equals(jumboEnumSet.elements, this.elements);
    }

    private boolean recalculateSize() {
        int i = this.size;
        this.size = 0;
        for (long j : this.elements) {
            this.size += Long.bitCount(j);
        }
        return this.size != i;
    }

    @Override
    public EnumSet<E> clone() {
        JumboEnumSet jumboEnumSet = (JumboEnumSet) super.clone();
        jumboEnumSet.elements = (long[]) jumboEnumSet.elements.clone();
        return jumboEnumSet;
    }
}
