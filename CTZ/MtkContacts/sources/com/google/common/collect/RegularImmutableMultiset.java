package com.google.common.collect;

import com.google.common.collect.Multiset;
import java.util.Map;

class RegularImmutableMultiset<E> extends ImmutableMultiset<E> {
    private final transient ImmutableMap<E, Integer> map;
    private final transient int size;

    RegularImmutableMultiset(ImmutableMap<E, Integer> immutableMap, int i) {
        this.map = immutableMap;
        this.size = i;
    }

    @Override
    boolean isPartialView() {
        return this.map.isPartialView();
    }

    @Override
    public int count(Object obj) {
        Integer num = this.map.get(obj);
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean contains(Object obj) {
        return this.map.containsKey(obj);
    }

    @Override
    public ImmutableSet<E> elementSet() {
        return this.map.keySet();
    }

    @Override
    Multiset.Entry<E> getEntry(int i) {
        Map.Entry<E, Integer> entry = this.map.entrySet().asList().get(i);
        return Multisets.immutableEntry(entry.getKey(), entry.getValue().intValue());
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }
}
