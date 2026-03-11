package com.google.common.collect;

import java.util.Collection;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Set;

class EmptyImmutableSortedSet<E> extends ImmutableSortedSet<E> {
    EmptyImmutableSortedSet(Comparator<? super E> comparator) {
        super(comparator);
    }

    @Override
    public ImmutableList<E> asList() {
        return ImmutableList.of();
    }

    @Override
    public boolean contains(Object obj) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return collection.isEmpty();
    }

    @Override
    int copyIntoArray(Object[] objArr, int i) {
        return i;
    }

    @Override
    ImmutableSortedSet<E> createDescendingSet() {
        return new EmptyImmutableSortedSet(Ordering.from(this.comparator).reverse());
    }

    @Override
    public UnmodifiableIterator<E> descendingIterator() {
        return Iterators.emptyIterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Set) {
            return ((Set) obj).isEmpty();
        }
        return false;
    }

    @Override
    public E first() {
        throw new NoSuchElementException();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    ImmutableSortedSet<E> headSetImpl(E e, boolean z) {
        return this;
    }

    @Override
    int indexOf(Object obj) {
        return -1;
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
    public UnmodifiableIterator<E> iterator() {
        return Iterators.emptyIterator();
    }

    @Override
    public E last() {
        throw new NoSuchElementException();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    ImmutableSortedSet<E> subSetImpl(E e, boolean z, E e2, boolean z2) {
        return this;
    }

    @Override
    ImmutableSortedSet<E> tailSetImpl(E e, boolean z) {
        return this;
    }

    @Override
    public String toString() {
        return "[]";
    }
}
