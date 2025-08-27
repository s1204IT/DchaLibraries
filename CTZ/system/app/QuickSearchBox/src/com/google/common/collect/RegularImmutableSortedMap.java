package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.NavigableMap;

/* loaded from: classes.dex */
final class RegularImmutableSortedMap<K, V> extends ImmutableSortedMap<K, V> {
    private final transient RegularImmutableSortedSet<K> keySet;
    private final transient ImmutableList<V> valueList;

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.ImmutableSortedMap, java.util.NavigableMap
    public /* bridge */ /* synthetic */ NavigableMap headMap(Object obj, boolean z) {
        return headMap((RegularImmutableSortedMap<K, V>) obj, z);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.ImmutableSortedMap, java.util.NavigableMap
    public /* bridge */ /* synthetic */ NavigableMap tailMap(Object obj, boolean z) {
        return tailMap((RegularImmutableSortedMap<K, V>) obj, z);
    }

    RegularImmutableSortedMap(RegularImmutableSortedSet<K> regularImmutableSortedSet, ImmutableList<V> immutableList) {
        this.keySet = regularImmutableSortedSet;
        this.valueList = immutableList;
    }

    RegularImmutableSortedMap(RegularImmutableSortedSet<K> regularImmutableSortedSet, ImmutableList<V> immutableList, ImmutableSortedMap<K, V> immutableSortedMap) {
        super(immutableSortedMap);
        this.keySet = regularImmutableSortedSet;
        this.valueList = immutableList;
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new EntrySet();
    }

    private class EntrySet extends ImmutableMapEntrySet<K, V> {
        private EntrySet() {
        }

        /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
        public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
            return asList().iterator();
        }

        @Override // com.google.common.collect.ImmutableCollection
        ImmutableList<Map.Entry<K, V>> createAsList() {
            return new ImmutableAsList<Map.Entry<K, V>>() { // from class: com.google.common.collect.RegularImmutableSortedMap.EntrySet.1
                private final ImmutableList<K> keyList;

                {
                    this.keyList = RegularImmutableSortedMap.this.keySet().asList();
                }

                /* JADX DEBUG: Method merged with bridge method: get(I)Ljava/lang/Object; */
                @Override // java.util.List
                public Map.Entry<K, V> get(int i) {
                    return Maps.immutableEntry(this.keyList.get(i), RegularImmutableSortedMap.this.valueList.get(i));
                }

                @Override // com.google.common.collect.ImmutableAsList
                ImmutableCollection<Map.Entry<K, V>> delegateCollection() {
                    return EntrySet.this;
                }
            };
        }

        @Override // com.google.common.collect.ImmutableMapEntrySet
        ImmutableMap<K, V> map() {
            return RegularImmutableSortedMap.this;
        }
    }

    /* JADX DEBUG: Method merged with bridge method: keySet()Lcom/google/common/collect/ImmutableSet; */
    /* JADX DEBUG: Method merged with bridge method: keySet()Ljava/util/Set; */
    @Override // com.google.common.collect.ImmutableSortedMap, com.google.common.collect.ImmutableMap, java.util.Map
    public ImmutableSortedSet<K> keySet() {
        return this.keySet;
    }

    /* JADX DEBUG: Method merged with bridge method: values()Ljava/util/Collection; */
    @Override // com.google.common.collect.ImmutableSortedMap, com.google.common.collect.ImmutableMap, java.util.Map, java.util.SortedMap
    public ImmutableCollection<V> values() {
        return this.valueList;
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public V get(Object obj) {
        int iIndexOf = this.keySet.indexOf(obj);
        if (iIndexOf == -1) {
            return null;
        }
        return this.valueList.get(iIndexOf);
    }

    private ImmutableSortedMap<K, V> getSubMap(int i, int i2) {
        if (i == 0 && i2 == size()) {
            return this;
        }
        if (i == i2) {
            return emptyMap(comparator());
        }
        return from(this.keySet.getSubSet(i, i2), this.valueList.subList(i, i2));
    }

    @Override // com.google.common.collect.ImmutableSortedMap, java.util.NavigableMap
    public ImmutableSortedMap<K, V> headMap(K k, boolean z) {
        return getSubMap(0, this.keySet.headIndex(Preconditions.checkNotNull(k), z));
    }

    @Override // com.google.common.collect.ImmutableSortedMap, java.util.NavigableMap
    public ImmutableSortedMap<K, V> tailMap(K k, boolean z) {
        return getSubMap(this.keySet.tailIndex(Preconditions.checkNotNull(k), z), size());
    }

    @Override // com.google.common.collect.ImmutableSortedMap
    ImmutableSortedMap<K, V> createDescendingMap() {
        return new RegularImmutableSortedMap((RegularImmutableSortedSet) this.keySet.descendingSet(), this.valueList.reverse(), this);
    }
}
