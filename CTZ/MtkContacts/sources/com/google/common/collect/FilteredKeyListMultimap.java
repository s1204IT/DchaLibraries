package com.google.common.collect;

import com.google.common.base.Predicate;
import java.util.List;

final class FilteredKeyListMultimap<K, V> extends FilteredKeyMultimap<K, V> implements ListMultimap<K, V> {
    FilteredKeyListMultimap(ListMultimap<K, V> listMultimap, Predicate<? super K> predicate) {
        super(listMultimap, predicate);
    }

    @Override
    public ListMultimap<K, V> unfiltered() {
        return (ListMultimap) super.unfiltered();
    }

    @Override
    public List<V> get(K k) {
        return (List) super.get((Object) k);
    }

    @Override
    public List<V> removeAll(Object obj) {
        return (List) super.removeAll(obj);
    }

    @Override
    public List<V> replaceValues(K k, Iterable<? extends V> iterable) {
        return (List) super.replaceValues((Object) k, (Iterable) iterable);
    }
}
