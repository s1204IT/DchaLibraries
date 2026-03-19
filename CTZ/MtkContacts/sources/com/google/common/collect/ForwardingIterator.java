package com.google.common.collect;

import java.util.Iterator;

public abstract class ForwardingIterator<T> extends ForwardingObject implements Iterator<T> {
    @Override
    protected abstract Iterator<T> delegate();

    protected ForwardingIterator() {
    }

    @Override
    public boolean hasNext() {
        return delegate().hasNext();
    }

    public T next() {
        return delegate().next();
    }

    public void remove() {
        delegate().remove();
    }
}
