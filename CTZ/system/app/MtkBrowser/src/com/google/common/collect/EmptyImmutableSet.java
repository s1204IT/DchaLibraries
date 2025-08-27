package com.google.common.collect;

import java.util.Collection;
import java.util.Set;

/* loaded from: classes.dex */
final class EmptyImmutableSet extends ImmutableSet<Object> {
    static final EmptyImmutableSet INSTANCE = new EmptyImmutableSet();
    private static final long serialVersionUID = 0;

    private EmptyImmutableSet() {
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
    public int size() {
        return 0;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean isEmpty() {
        return true;
    }

    @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean contains(Object obj) {
        return false;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean containsAll(Collection<?> collection) {
        return collection.isEmpty();
    }

    /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
    @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
    public UnmodifiableIterator<Object> iterator() {
        return Iterators.emptyIterator();
    }

    @Override // com.google.common.collect.ImmutableCollection
    boolean isPartialView() {
        return false;
    }

    @Override // com.google.common.collect.ImmutableCollection
    int copyIntoArray(Object[] objArr, int i) {
        return i;
    }

    @Override // com.google.common.collect.ImmutableCollection
    public ImmutableList<Object> asList() {
        return ImmutableList.of();
    }

    @Override // com.google.common.collect.ImmutableSet, java.util.Collection, java.util.Set
    public boolean equals(Object obj) {
        if (obj instanceof Set) {
            return ((Set) obj).isEmpty();
        }
        return false;
    }

    @Override // com.google.common.collect.ImmutableSet, java.util.Collection, java.util.Set
    public final int hashCode() {
        return 0;
    }

    @Override // com.google.common.collect.ImmutableSet
    boolean isHashCodeFast() {
        return true;
    }

    @Override // java.util.AbstractCollection
    public String toString() {
        return "[]";
    }

    Object readResolve() {
        return INSTANCE;
    }
}
