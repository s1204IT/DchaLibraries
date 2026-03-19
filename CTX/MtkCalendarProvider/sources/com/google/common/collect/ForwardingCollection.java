package com.google.common.collect;

import java.util.Collection;
import java.util.Iterator;

public abstract class ForwardingCollection<E> extends ForwardingObject implements Collection<E> {
    @Override
    protected abstract Collection<E> delegate();

    protected ForwardingCollection() {
    }

    @Override
    public Iterator<E> iterator() {
        return delegate().iterator();
    }

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return delegate().removeAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return delegate().contains(obj);
    }

    @Override
    public boolean add(E e) {
        return delegate().add(e);
    }

    @Override
    public boolean remove(Object obj) {
        return delegate().remove(obj);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return delegate().containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return delegate().addAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return delegate().retainAll(collection);
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public Object[] toArray() {
        return delegate().toArray();
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        return (T[]) delegate().toArray(tArr);
    }
}
