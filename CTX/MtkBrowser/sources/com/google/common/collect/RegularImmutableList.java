package com.google.common.collect;

import com.google.common.base.Preconditions;

class RegularImmutableList<E> extends ImmutableList<E> {
    private final transient Object[] array;
    private final transient int offset;
    private final transient int size;

    RegularImmutableList(Object[] objArr) {
        this(objArr, 0, objArr.length);
    }

    RegularImmutableList(Object[] objArr, int i, int i2) {
        this.offset = i;
        this.size = i2;
        this.array = objArr;
    }

    @Override
    int copyIntoArray(Object[] objArr, int i) {
        System.arraycopy(this.array, this.offset, objArr, i, this.size);
        return this.size + i;
    }

    @Override
    public E get(int i) {
        Preconditions.checkElementIndex(i, this.size);
        return (E) this.array[this.offset + i];
    }

    @Override
    public int indexOf(Object obj) {
        if (obj == null) {
            return -1;
        }
        for (int i = 0; i < this.size; i++) {
            if (this.array[this.offset + i].equals(obj)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    boolean isPartialView() {
        return this.size != this.array.length;
    }

    @Override
    public int lastIndexOf(Object obj) {
        if (obj == null) {
            return -1;
        }
        for (int i = this.size - 1; i >= 0; i--) {
            if (this.array[this.offset + i].equals(obj)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public UnmodifiableListIterator<E> listIterator(int i) {
        return Iterators.forArray(this.array, this.offset, this.size, i);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    ImmutableList<E> subListUnchecked(int i, int i2) {
        return new RegularImmutableList(this.array, this.offset + i, i2 - i);
    }
}
