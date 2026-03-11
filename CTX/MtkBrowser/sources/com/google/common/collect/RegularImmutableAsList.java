package com.google.common.collect;

class RegularImmutableAsList<E> extends ImmutableAsList<E> {
    private final ImmutableCollection<E> delegate;
    private final ImmutableList<? extends E> delegateList;

    RegularImmutableAsList(ImmutableCollection<E> immutableCollection, ImmutableList<? extends E> immutableList) {
        this.delegate = immutableCollection;
        this.delegateList = immutableList;
    }

    RegularImmutableAsList(ImmutableCollection<E> immutableCollection, Object[] objArr) {
        this(immutableCollection, ImmutableList.asImmutableList(objArr));
    }

    @Override
    int copyIntoArray(Object[] objArr, int i) {
        return this.delegateList.copyIntoArray(objArr, i);
    }

    @Override
    ImmutableCollection<E> delegateCollection() {
        return this.delegate;
    }

    @Override
    public E get(int i) {
        return this.delegateList.get(i);
    }

    @Override
    public UnmodifiableListIterator<E> listIterator(int i) {
        return this.delegateList.listIterator(i);
    }
}
