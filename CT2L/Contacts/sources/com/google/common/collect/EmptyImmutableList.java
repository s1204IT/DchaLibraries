package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

final class EmptyImmutableList extends ImmutableList<Object> {
    private static final long serialVersionUID = 0;
    static final EmptyImmutableList INSTANCE = new EmptyImmutableList();
    static final UnmodifiableListIterator<Object> ITERATOR = new UnmodifiableListIterator<Object>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int previousIndex() {
            return -1;
        }
    };
    private static final Object[] EMPTY_ARRAY = new Object[0];

    private EmptyImmutableList() {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public boolean contains(Object target) {
        return false;
    }

    @Override
    public UnmodifiableIterator<Object> iterator() {
        return Iterators.emptyIterator();
    }

    @Override
    public Object[] toArray() {
        return EMPTY_ARRAY;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length > 0) {
            a[0] = null;
        }
        return a;
    }

    @Override
    public Object get(int index) {
        Preconditions.checkElementIndex(index, 0);
        throw new AssertionError("unreachable");
    }

    @Override
    public int indexOf(Object target) {
        return -1;
    }

    @Override
    public int lastIndexOf(Object target) {
        return -1;
    }

    @Override
    public ImmutableList<Object> subList(int fromIndex, int toIndex) {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, 0);
        return this;
    }

    @Override
    public UnmodifiableListIterator<Object> listIterator() {
        return ITERATOR;
    }

    @Override
    public UnmodifiableListIterator<Object> listIterator(int start) {
        Preconditions.checkPositionIndex(start, 0);
        return ITERATOR;
    }

    @Override
    public boolean containsAll(Collection<?> targets) {
        return targets.isEmpty();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof List)) {
            return false;
        }
        List<?> that = (List) object;
        return that.isEmpty();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "[]";
    }

    Object readResolve() {
        return INSTANCE;
    }
}
