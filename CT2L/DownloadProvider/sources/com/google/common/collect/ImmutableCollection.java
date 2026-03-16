package com.google.common.collect;

import java.io.Serializable;
import java.util.Collection;

public abstract class ImmutableCollection<E> implements Serializable, Collection<E> {
    static final ImmutableCollection<Object> EMPTY_IMMUTABLE_COLLECTION = new EmptyImmutableCollection();

    @Override
    public abstract UnmodifiableIterator<E> iterator();

    ImmutableCollection() {
    }

    public Object[] toArray() {
        return ObjectArrays.toArrayImpl(this);
    }

    public <T> T[] toArray(T[] tArr) {
        return (T[]) ObjectArrays.toArrayImpl(this, tArr);
    }

    public boolean contains(Object object) {
        return object != null && Iterators.contains(iterator(), object);
    }

    public boolean containsAll(Collection<?> targets) {
        return Collections2.containsAllImpl(this, targets);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public String toString() {
        return Collections2.toStringImpl(this);
    }

    @Override
    public final boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean addAll(Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean removeAll(Collection<?> oldElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean retainAll(Collection<?> elementsToKeep) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    private static class EmptyImmutableCollection extends ImmutableCollection<Object> {
        private static final Object[] EMPTY_ARRAY = new Object[0];

        private EmptyImmutableCollection() {
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
        public boolean contains(Object object) {
            return false;
        }

        @Override
        public UnmodifiableIterator<Object> iterator() {
            return Iterators.EMPTY_ITERATOR;
        }

        @Override
        public Object[] toArray() {
            return EMPTY_ARRAY;
        }

        @Override
        public <T> T[] toArray(T[] array) {
            if (array.length > 0) {
                array[0] = null;
            }
            return array;
        }
    }

    private static class ArrayImmutableCollection<E> extends ImmutableCollection<E> {
        private final E[] elements;

        ArrayImmutableCollection(E[] elements) {
            this.elements = elements;
        }

        @Override
        public int size() {
            return this.elements.length;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public UnmodifiableIterator<E> iterator() {
            return Iterators.forArray(this.elements);
        }
    }

    private static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final Object[] elements;

        SerializedForm(Object[] elements) {
            this.elements = elements;
        }

        Object readResolve() {
            return this.elements.length == 0 ? ImmutableCollection.EMPTY_IMMUTABLE_COLLECTION : new ArrayImmutableCollection(Platform.clone(this.elements));
        }
    }

    Object writeReplace() {
        return new SerializedForm(toArray());
    }
}
