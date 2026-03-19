package com.google.common.collect;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;

abstract class AbstractSortedSetMultimap<K, V> extends AbstractSetMultimap<K, V> implements SortedSetMultimap<K, V> {
    private static final long serialVersionUID = 430848587173315748L;

    @Override
    abstract SortedSet<V> createCollection();

    protected AbstractSortedSetMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    @Override
    SortedSet<V> createUnmodifiableEmptyCollection() {
        if (valueComparator() == null) {
            return Collections.unmodifiableSortedSet(createCollection());
        }
        return ImmutableSortedSet.emptySet(valueComparator());
    }

    @Override
    public SortedSet<V> get(K k) {
        return (SortedSet) super.get((Object) k);
    }

    @Override
    public SortedSet<V> removeAll(Object obj) {
        return (SortedSet) super.removeAll(obj);
    }

    @Override
    public SortedSet<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return (SortedSet) super.replaceValues((Object) k, (Iterable) iterable);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    @Override
    public Collection<V> values() {
        return super.values();
    }
}
