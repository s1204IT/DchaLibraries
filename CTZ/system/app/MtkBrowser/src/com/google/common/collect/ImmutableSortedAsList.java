package com.google.common.collect;

import java.util.Comparator;

/* loaded from: classes.dex */
final class ImmutableSortedAsList<E> extends RegularImmutableAsList<E> implements SortedIterable<E> {
    ImmutableSortedAsList(ImmutableSortedSet<E> immutableSortedSet, ImmutableList<E> immutableList) {
        super(immutableSortedSet, immutableList);
    }

    /* JADX DEBUG: Method merged with bridge method: delegateCollection()Lcom/google/common/collect/ImmutableCollection; */
    @Override // com.google.common.collect.RegularImmutableAsList, com.google.common.collect.ImmutableAsList
    ImmutableSortedSet<E> delegateCollection() {
        return (ImmutableSortedSet) super.delegateCollection();
    }

    @Override // com.google.common.collect.SortedIterable
    public Comparator<? super E> comparator() {
        return delegateCollection().comparator();
    }

    @Override // com.google.common.collect.ImmutableList, java.util.List
    public int indexOf(Object obj) {
        int iIndexOf = delegateCollection().indexOf(obj);
        if (iIndexOf < 0 || !get(iIndexOf).equals(obj)) {
            return -1;
        }
        return iIndexOf;
    }

    @Override // com.google.common.collect.ImmutableList, java.util.List
    public int lastIndexOf(Object obj) {
        return indexOf(obj);
    }

    @Override // com.google.common.collect.ImmutableAsList, com.google.common.collect.ImmutableList, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    @Override // com.google.common.collect.ImmutableList
    ImmutableList<E> subListUnchecked(int i, int i2) {
        return new RegularImmutableSortedSet(super.subListUnchecked(i, i2), comparator()).asList();
    }
}
