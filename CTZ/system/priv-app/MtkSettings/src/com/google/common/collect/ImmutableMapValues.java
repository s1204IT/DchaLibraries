package com.google.common.collect;

import java.io.Serializable;
import java.util.Map;

/* loaded from: classes.dex */
final class ImmutableMapValues<K, V> extends ImmutableCollection<V> {
    private final ImmutableMap<K, V> map;

    ImmutableMapValues(ImmutableMap<K, V> immutableMap) {
        this.map = immutableMap;
    }

    @Override // java.util.AbstractCollection, java.util.Collection
    public int size() {
        return this.map.size();
    }

    /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
    @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
    public UnmodifiableIterator<V> iterator() {
        return Maps.valueIterator((UnmodifiableIterator) this.map.entrySet().iterator());
    }

    @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean contains(Object obj) {
        return obj != null && Iterators.contains(iterator(), obj);
    }

    @Override // com.google.common.collect.ImmutableCollection
    boolean isPartialView() {
        return true;
    }

    @Override // com.google.common.collect.ImmutableCollection
    ImmutableList<V> createAsList() {
        final ImmutableList<Map.Entry<K, V>> immutableListAsList = this.map.entrySet().asList();
        return new ImmutableAsList<V>() { // from class: com.google.common.collect.ImmutableMapValues.1
            @Override // java.util.List
            public V get(int i) {
                return (V) ((Map.Entry) immutableListAsList.get(i)).getValue();
            }

            @Override // com.google.common.collect.ImmutableAsList
            ImmutableCollection<V> delegateCollection() {
                return ImmutableMapValues.this;
            }
        };
    }

    @Override // com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return new SerializedForm(this.map);
    }

    private static class SerializedForm<V> implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableMap<?, V> map;

        SerializedForm(ImmutableMap<?, V> immutableMap) {
            this.map = immutableMap;
        }

        Object readResolve() {
            return this.map.values();
        }
    }
}
