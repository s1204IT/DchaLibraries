package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Set;

final class SingletonImmutableSet<E> extends ImmutableSet<E> {
    private transient int cachedHashCode;
    final transient E element;

    SingletonImmutableSet(E e) {
        this.element = (E) Preconditions.checkNotNull(e);
    }

    SingletonImmutableSet(E element, int hashCode) {
        this.element = element;
        this.cachedHashCode = hashCode;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object target) {
        return this.element.equals(target);
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return Iterators.singletonIterator(this.element);
    }

    @Override
    public Object[] toArray() {
        return new Object[]{this.element};
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        if (tArr.length == 0) {
            tArr = (T[]) ObjectArrays.newArray(tArr, 1);
        } else if (tArr.length > 1) {
            tArr[1] = null;
        }
        tArr[0] = this.element;
        return tArr;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Set)) {
            return false;
        }
        Set<?> that = (Set) object;
        return that.size() == 1 && this.element.equals(that.iterator().next());
    }

    @Override
    public final int hashCode() {
        int code = this.cachedHashCode;
        if (code == 0) {
            int code2 = this.element.hashCode();
            this.cachedHashCode = code2;
            return code2;
        }
        return code;
    }

    @Override
    boolean isHashCodeFast() {
        return this.cachedHashCode != 0;
    }

    @Override
    public String toString() {
        String elementToString = this.element.toString();
        return new StringBuilder(elementToString.length() + 2).append('[').append(elementToString).append(']').toString();
    }
}
