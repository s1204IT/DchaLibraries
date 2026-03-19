package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

abstract class AbstractMultiset<E> extends AbstractCollection<E> implements Multiset<E> {
    private transient Set<E> elementSet;
    private transient Set<Multiset.Entry<E>> entrySet;

    abstract int distinctElements();

    abstract Iterator<Multiset.Entry<E>> entryIterator();

    AbstractMultiset() {
    }

    @Override
    public int size() {
        return Multisets.sizeImpl(this);
    }

    @Override
    public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return count(obj) > 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Multisets.iteratorImpl(this);
    }

    public int count(Object obj) {
        for (Multiset.Entry<E> entry : entrySet()) {
            if (Objects.equal(entry.getElement(), obj)) {
                return entry.getCount();
            }
        }
        return 0;
    }

    @Override
    public boolean add(E e) {
        add(e, 1);
        return true;
    }

    public int add(E e, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object obj) {
        return remove(obj, 1) > 0;
    }

    public int remove(Object obj, int i) {
        throw new UnsupportedOperationException();
    }

    public int setCount(E e, int i) {
        return Multisets.setCountImpl(this, e, i);
    }

    @Override
    public boolean setCount(E e, int i, int i2) {
        return Multisets.setCountImpl(this, e, i, i2);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return Multisets.addAllImpl(this, collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return Multisets.removeAllImpl(this, collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return Multisets.retainAllImpl(this, collection);
    }

    @Override
    public void clear() {
        Iterators.clear(entryIterator());
    }

    @Override
    public Set<E> elementSet() {
        Set<E> set = this.elementSet;
        if (set == null) {
            Set<E> setCreateElementSet = createElementSet();
            this.elementSet = setCreateElementSet;
            return setCreateElementSet;
        }
        return set;
    }

    Set<E> createElementSet() {
        return new ElementSet();
    }

    class ElementSet extends Multisets.ElementSet<E> {
        ElementSet() {
        }

        @Override
        Multiset<E> multiset() {
            return AbstractMultiset.this;
        }
    }

    public Set<Multiset.Entry<E>> entrySet() {
        Set<Multiset.Entry<E>> set = this.entrySet;
        if (set == null) {
            Set<Multiset.Entry<E>> setCreateEntrySet = createEntrySet();
            this.entrySet = setCreateEntrySet;
            return setCreateEntrySet;
        }
        return set;
    }

    class EntrySet extends Multisets.EntrySet<E> {
        EntrySet() {
        }

        @Override
        Multiset<E> multiset() {
            return AbstractMultiset.this;
        }

        @Override
        public Iterator<Multiset.Entry<E>> iterator() {
            return AbstractMultiset.this.entryIterator();
        }

        @Override
        public int size() {
            return AbstractMultiset.this.distinctElements();
        }
    }

    Set<Multiset.Entry<E>> createEntrySet() {
        return new EntrySet();
    }

    @Override
    public boolean equals(Object obj) {
        return Multisets.equalsImpl(this, obj);
    }

    @Override
    public int hashCode() {
        return entrySet().hashCode();
    }

    @Override
    public String toString() {
        return entrySet().toString();
    }
}
