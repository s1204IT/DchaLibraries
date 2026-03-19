package com.google.common.collect;

import java.util.Comparator;
import java.util.SortedSet;

public abstract class ForwardingSortedSetMultimap<K, V> extends ForwardingSetMultimap<K, V> implements SortedSetMultimap<K, V> {
    @Override
    protected abstract SortedSetMultimap<K, V> delegate();

    protected ForwardingSortedSetMultimap() {
    }

    @Override
    public SortedSet<V> get(K k) {
        return delegate().get((Object) k);
    }

    @Override
    public SortedSet<V> removeAll(Object obj) {
        return delegate().removeAll(obj);
    }

    @Override
    public SortedSet<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return delegate().replaceValues((Object) k, (Iterable) iterable);
    }

    @Override
    public Comparator<? super V> valueComparator() {
        return delegate().valueComparator();
    }
}
