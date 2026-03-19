package com.google.protobuf;

import com.google.protobuf.Internal;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

abstract class AbstractProtobufList<E> extends AbstractList<E> implements Internal.ProtobufList<E> {
    protected static final int DEFAULT_CAPACITY = 10;
    private boolean isMutable = true;

    AbstractProtobufList() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        if (!(obj instanceof RandomAccess)) {
            return super.equals(obj);
        }
        List list = (List) obj;
        int size = size();
        if (size != list.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!get(i).equals(list.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int size = size();
        int iHashCode = 1;
        for (int i = 0; i < size; i++) {
            iHashCode = get(i).hashCode() + (31 * iHashCode);
        }
        return iHashCode;
    }

    @Override
    public boolean add(E e) {
        ensureIsMutable();
        return super.add(e);
    }

    @Override
    public void add(int i, E e) {
        ensureIsMutable();
        super.add(i, e);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        ensureIsMutable();
        return super.addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> collection) {
        ensureIsMutable();
        return super.addAll(i, collection);
    }

    @Override
    public void clear() {
        ensureIsMutable();
        super.clear();
    }

    @Override
    public boolean isModifiable() {
        return this.isMutable;
    }

    @Override
    public final void makeImmutable() {
        this.isMutable = false;
    }

    @Override
    public E remove(int i) {
        ensureIsMutable();
        return (E) super.remove(i);
    }

    @Override
    public boolean remove(Object obj) {
        ensureIsMutable();
        return super.remove(obj);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        ensureIsMutable();
        return super.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        ensureIsMutable();
        return super.retainAll(collection);
    }

    @Override
    public E set(int i, E e) {
        ensureIsMutable();
        return (E) super.set(i, e);
    }

    protected void ensureIsMutable() {
        if (!this.isMutable) {
            throw new UnsupportedOperationException();
        }
    }
}
