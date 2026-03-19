package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset;
import java.util.Collection;
import java.util.Comparator;

final class EmptyImmutableSortedMultiset<E> extends ImmutableSortedMultiset<E> {
    private final ImmutableSortedSet<E> elementSet;

    EmptyImmutableSortedMultiset(Comparator<? super E> comparator) {
        this.elementSet = ImmutableSortedSet.emptySet(comparator);
    }

    @Override
    public Multiset.Entry<E> firstEntry() {
        return null;
    }

    @Override
    public Multiset.Entry<E> lastEntry() {
        return null;
    }

    @Override
    public int count(Object obj) {
        return 0;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return collection.isEmpty();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public ImmutableSortedSet<E> elementSet() {
        return this.elementSet;
    }

    @Override
    Multiset.Entry<E> getEntry(int i) {
        throw new AssertionError("should never be called");
    }

    @Override
    public ImmutableSortedMultiset<E> headMultiset(E e, BoundType boundType) {
        Preconditions.checkNotNull(e);
        Preconditions.checkNotNull(boundType);
        return this;
    }

    @Override
    public ImmutableSortedMultiset<E> tailMultiset(E e, BoundType boundType) {
        Preconditions.checkNotNull(e);
        Preconditions.checkNotNull(boundType);
        return this;
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return Iterators.emptyIterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Multiset) {
            return ((Multiset) obj).isEmpty();
        }
        return false;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    int copyIntoArray(Object[] objArr, int i) {
        return i;
    }

    @Override
    public ImmutableList<E> asList() {
        return ImmutableList.of();
    }
}
