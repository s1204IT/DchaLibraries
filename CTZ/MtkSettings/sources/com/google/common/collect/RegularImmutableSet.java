package com.google.common.collect;

final class RegularImmutableSet<E> extends ImmutableSet<E> {
    private final Object[] elements;
    private final transient int hashCode;
    private final transient int mask;
    final transient Object[] table;

    RegularImmutableSet(Object[] objArr, int i, Object[] objArr2, int i2) {
        this.elements = objArr;
        this.table = objArr2;
        this.mask = i2;
        this.hashCode = i;
    }

    @Override
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        int iSmear = Hashing.smear(obj.hashCode());
        while (true) {
            Object obj2 = this.table[this.mask & iSmear];
            if (obj2 == null) {
                return false;
            }
            if (!obj2.equals(obj)) {
                iSmear++;
            } else {
                return true;
            }
        }
    }

    @Override
    public int size() {
        return this.elements.length;
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return Iterators.forArray(this.elements);
    }

    @Override
    int copyIntoArray(Object[] objArr, int i) {
        System.arraycopy(this.elements, 0, objArr, i, this.elements.length);
        return i + this.elements.length;
    }

    @Override
    ImmutableList<E> createAsList() {
        return new RegularImmutableAsList(this, this.elements);
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    boolean isHashCodeFast() {
        return true;
    }
}
