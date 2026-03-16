package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.List;

class RegularImmutableList<E> extends ImmutableList<E> {
    private final transient Object[] array;
    private final transient int offset;
    private final transient int size;

    RegularImmutableList(Object[] array, int offset, int size) {
        this.offset = offset;
        this.size = size;
        this.array = array;
    }

    RegularImmutableList(Object[] array) {
        this(array, 0, array.length);
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    boolean isPartialView() {
        return (this.offset == 0 && this.size == this.array.length) ? false : true;
    }

    @Override
    public boolean contains(Object target) {
        return indexOf(target) != -1;
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return Iterators.forArray(this.array, this.offset, this.size);
    }

    @Override
    public Object[] toArray() {
        Object[] newArray = new Object[size()];
        System.arraycopy(this.array, this.offset, newArray, 0, this.size);
        return newArray;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        if (tArr.length < this.size) {
            tArr = (T[]) ObjectArrays.newArray(tArr, this.size);
        } else if (tArr.length > this.size) {
            tArr[this.size] = null;
        }
        System.arraycopy(this.array, this.offset, tArr, 0, this.size);
        return tArr;
    }

    @Override
    public E get(int i) {
        Preconditions.checkElementIndex(i, this.size);
        return (E) this.array[this.offset + i];
    }

    @Override
    public int indexOf(Object target) {
        if (target != null) {
            for (int i = this.offset; i < this.offset + this.size; i++) {
                if (this.array[i].equals(target)) {
                    return i - this.offset;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object target) {
        if (target != null) {
            for (int i = (this.offset + this.size) - 1; i >= this.offset; i--) {
                if (this.array[i].equals(target)) {
                    return i - this.offset;
                }
            }
        }
        return -1;
    }

    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, this.size);
        return fromIndex == toIndex ? ImmutableList.of() : new RegularImmutableList(this.array, this.offset + fromIndex, toIndex - fromIndex);
    }

    @Override
    public UnmodifiableListIterator<E> listIterator(int start) {
        return new AbstractIndexedListIterator<E>(this.size, start) {
            @Override
            protected E get(int i) {
                return (E) RegularImmutableList.this.array[RegularImmutableList.this.offset + i];
            }
        };
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof List)) {
            return false;
        }
        List<?> that = (List) object;
        if (size() != that.size()) {
            return false;
        }
        int index = this.offset;
        if (object instanceof RegularImmutableList) {
            RegularImmutableList<?> other = (RegularImmutableList) object;
            int i = other.offset;
            while (i < other.offset + other.size) {
                int index2 = index + 1;
                if (!this.array[index].equals(other.array[i])) {
                    return false;
                }
                i++;
                index = index2;
            }
            return true;
        }
        for (Object element : that) {
            int index3 = index + 1;
            if (!this.array[index].equals(element)) {
                return false;
            }
            index = index3;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (int i = this.offset; i < this.offset + this.size; i++) {
            hashCode = (hashCode * 31) + this.array[i].hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = Collections2.newStringBuilderForCollection(size()).append('[').append(this.array[this.offset]);
        for (int i = this.offset + 1; i < this.offset + this.size; i++) {
            sb.append(", ").append(this.array[i]);
        }
        return sb.append(']').toString();
    }
}
