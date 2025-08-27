package com.google.common.collect;

import java.util.Collection;
import java.util.Iterator;

/* loaded from: classes.dex */
public abstract class ForwardingCollection<E> extends ForwardingObject implements Collection<E> {
    /* JADX DEBUG: Method merged with bridge method: delegate()Ljava/lang/Object; */
    @Override // com.google.common.collect.ForwardingObject
    protected abstract Collection<E> delegate();

    protected ForwardingCollection() {
    }

    @Override // java.util.Collection, java.lang.Iterable
    public Iterator<E> iterator() {
        return delegate().iterator();
    }

    @Override // java.util.Collection
    public int size() {
        return delegate().size();
    }

    @Override // java.util.Collection
    public boolean removeAll(Collection<?> collection) {
        return delegate().removeAll(collection);
    }

    @Override // java.util.Collection
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override // java.util.Collection
    public boolean contains(Object obj) {
        return delegate().contains(obj);
    }

    @Override // java.util.Collection
    public boolean add(E e) {
        return delegate().add(e);
    }

    @Override // java.util.Collection
    public boolean remove(Object obj) {
        return delegate().remove(obj);
    }

    @Override // java.util.Collection
    public boolean containsAll(Collection<?> collection) {
        return delegate().containsAll(collection);
    }

    @Override // java.util.Collection
    public boolean addAll(Collection<? extends E> collection) {
        return delegate().addAll(collection);
    }

    @Override // java.util.Collection
    public boolean retainAll(Collection<?> collection) {
        return delegate().retainAll(collection);
    }

    @Override // java.util.Collection
    public void clear() {
        delegate().clear();
    }

    @Override // java.util.Collection
    public Object[] toArray() {
        return delegate().toArray();
    }

    @Override // java.util.Collection
    public <T> T[] toArray(T[] tArr) {
        return (T[]) delegate().toArray(tArr);
    }
}
