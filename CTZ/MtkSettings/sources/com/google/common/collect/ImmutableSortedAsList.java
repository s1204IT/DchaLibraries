package com.google.common.collect;

import java.util.Comparator;

final class ImmutableSortedAsList<E> extends RegularImmutableAsList<E> implements SortedIterable<E> {
    ImmutableSortedAsList(ImmutableSortedSet<E> immutableSortedSet, ImmutableList<E> immutableList) {
        super(immutableSortedSet, immutableList);
    }

    @Override
    public ImmutableSortedSet<E> delegateCollection() {
        return (ImmutableSortedSet) super.delegateCollection();
    }

    @Override
    public Comparator<? super E> comparator() {
        return delegateCollection().comparator();
    }

    @Override
    public int indexOf(Object obj) {
        int iIndexOf = delegateCollection().indexOf(obj);
        if (iIndexOf < 0 || !get(iIndexOf).equals(obj)) {
            return -1;
        }
        return iIndexOf;
    }

    @Override
    public int lastIndexOf(Object obj) {
        return indexOf(obj);
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    @Override
    ImmutableList<E> subListUnchecked(int i, int i2) {
        return new RegularImmutableSortedSet(super.subListUnchecked(i, i2), comparator()).asList();
    }
}
