package com.google.common.collect;

import java.util.ListIterator;

public abstract class ForwardingListIterator<E> extends ForwardingIterator<E> implements ListIterator<E> {
    @Override
    protected abstract ListIterator<E> delegate();

    protected ForwardingListIterator() {
    }

    public void add(E e) {
        delegate().add(e);
    }

    @Override
    public boolean hasPrevious() {
        return delegate().hasPrevious();
    }

    @Override
    public int nextIndex() {
        return delegate().nextIndex();
    }

    @Override
    public E previous() {
        return delegate().previous();
    }

    @Override
    public int previousIndex() {
        return delegate().previousIndex();
    }

    public void set(E e) {
        delegate().set(e);
    }
}
