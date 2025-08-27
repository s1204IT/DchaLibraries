package com.google.common.collect;

import java.io.Serializable;
import java.util.Map;

/* loaded from: classes.dex */
final class ImmutableMapKeySet<K, V> extends ImmutableSet<K> {
    private final ImmutableMap<K, V> map;

    ImmutableMapKeySet(ImmutableMap<K, V> immutableMap) {
        this.map = immutableMap;
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.Set
    public int size() {
        return this.map.size();
    }

    /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
    @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
    public UnmodifiableIterator<K> iterator() {
        return asList().iterator();
    }

    @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean contains(Object obj) {
        return this.map.containsKey(obj);
    }

    @Override // com.google.common.collect.ImmutableCollection
    ImmutableList<K> createAsList() {
        final ImmutableList<Map.Entry<K, V>> immutableListAsList = this.map.entrySet().asList();
        return new ImmutableAsList<K>() { // from class: com.google.common.collect.ImmutableMapKeySet.1
            @Override // java.util.List
            public K get(int i) {
                return (K) ((Map.Entry) immutableListAsList.get(i)).getKey();
            }

            @Override // com.google.common.collect.ImmutableAsList
            ImmutableCollection<K> delegateCollection() {
                return ImmutableMapKeySet.this;
            }
        };
    }

    @Override // com.google.common.collect.ImmutableCollection
    boolean isPartialView() {
        return true;
    }

    @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return new KeySetSerializedForm(this.map);
    }

    private static class KeySetSerializedForm<K> implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableMap<K, ?> map;

        KeySetSerializedForm(ImmutableMap<K, ?> immutableMap) {
            this.map = immutableMap;
        }

        Object readResolve() {
            return this.map.keySet();
        }
    }
}
