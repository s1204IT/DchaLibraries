package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableAsList;
import java.util.Comparator;

final class ImmutableSortedAsList<E> extends ImmutableList<E> implements SortedIterable<E> {
    private final transient ImmutableList<E> backingList;
    private final transient ImmutableSortedSet<E> backingSet;

    ImmutableSortedAsList(ImmutableSortedSet<E> backingSet, ImmutableList<E> backingList) {
        this.backingSet = backingSet;
        this.backingList = backingList;
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.backingSet.comparator();
    }

    @Override
    public boolean contains(Object target) {
        return this.backingSet.indexOf(target) >= 0;
    }

    @Override
    public int indexOf(Object target) {
        return this.backingSet.indexOf(target);
    }

    @Override
    public int lastIndexOf(Object target) {
        return this.backingSet.indexOf(target);
    }

    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, size());
        return fromIndex == toIndex ? ImmutableList.of() : new RegularImmutableSortedSet(this.backingList.subList(fromIndex, toIndex), this.backingSet.comparator()).asList();
    }

    @Override
    Object writeReplace() {
        return new ImmutableAsList.SerializedForm(this.backingSet);
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return this.backingList.iterator();
    }

    @Override
    public E get(int index) {
        return this.backingList.get(index);
    }

    @Override
    public UnmodifiableListIterator<E> listIterator() {
        return this.backingList.listIterator();
    }

    @Override
    public UnmodifiableListIterator<E> listIterator(int index) {
        return this.backingList.listIterator(index);
    }

    @Override
    public int size() {
        return this.backingList.size();
    }

    @Override
    public boolean equals(Object obj) {
        return this.backingList.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.backingList.hashCode();
    }

    @Override
    boolean isPartialView() {
        return this.backingList.isPartialView();
    }
}
