package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.Map;

final class EmptyImmutableSortedMap<K, V> extends ImmutableSortedMap<K, V> {
    private final transient ImmutableSortedSet<K> keySet;

    EmptyImmutableSortedMap(Comparator<? super K> comparator) {
        this.keySet = ImmutableSortedSet.emptySet(comparator);
    }

    EmptyImmutableSortedMap(Comparator<? super K> comparator, ImmutableSortedMap<K, V> immutableSortedMap) {
        super(immutableSortedMap);
        this.keySet = ImmutableSortedSet.emptySet(comparator);
    }

    @Override
    public V get(Object obj) {
        return null;
    }

    @Override
    public ImmutableSortedSet<K> keySet() {
        return this.keySet;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ImmutableCollection<V> values() {
        return ImmutableList.of();
    }

    @Override
    public String toString() {
        return "{}";
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        return ImmutableSet.of();
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        throw new AssertionError("should never be called");
    }

    @Override
    public ImmutableSortedMap<K, V> headMap(K k, boolean z) {
        Preconditions.checkNotNull(k);
        return this;
    }

    @Override
    public ImmutableSortedMap<K, V> tailMap(K k, boolean z) {
        Preconditions.checkNotNull(k);
        return this;
    }

    @Override
    ImmutableSortedMap<K, V> createDescendingMap() {
        return new EmptyImmutableSortedMap(Ordering.from(comparator()).reverse(), this);
    }
}
