package com.google.common.collect;

import java.util.Collection;
import java.util.Set;

final class EmptyImmutableSet extends ImmutableSet<Object> {
    static final EmptyImmutableSet INSTANCE = new EmptyImmutableSet();
    private static final long serialVersionUID = 0;

    private EmptyImmutableSet() {
    }

    @Override
    public ImmutableList<Object> asList() {
        return ImmutableList.of();
    }

    @Override
    public boolean contains(Object obj) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return collection.isEmpty();
    }

    @Override
    int copyIntoArray(Object[] objArr, int i) {
        return i;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Set) {
            return ((Set) obj).isEmpty();
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    boolean isHashCodeFast() {
        return true;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public UnmodifiableIterator<Object> iterator() {
        return Iterators.emptyIterator();
    }

    Object readResolve() {
        return INSTANCE;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return "[]";
    }
}
