package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;

/* loaded from: classes.dex */
final class EmptyImmutableSortedMap<K, V> extends ImmutableSortedMap<K, V> {
    private final transient ImmutableSortedSet<K> keySet;

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.ImmutableSortedMap, java.util.NavigableMap
    public /* bridge */ /* synthetic */ NavigableMap headMap(Object obj, boolean z) {
        return headMap((EmptyImmutableSortedMap<K, V>) obj, z);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.google.common.collect.ImmutableSortedMap, java.util.NavigableMap
    public /* bridge */ /* synthetic */ NavigableMap tailMap(Object obj, boolean z) {
        return tailMap((EmptyImmutableSortedMap<K, V>) obj, z);
    }

    EmptyImmutableSortedMap(Comparator<? super K> comparator) {
        this.keySet = ImmutableSortedSet.emptySet(comparator);
    }

    EmptyImmutableSortedMap(Comparator<? super K> comparator, ImmutableSortedMap<K, V> immutableSortedMap) {
        super(immutableSortedMap);
        this.keySet = ImmutableSortedSet.emptySet(comparator);
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public V get(Object obj) {
        return null;
    }

    /* JADX DEBUG: Method merged with bridge method: keySet()Lcom/google/common/collect/ImmutableSet; */
    /* JADX DEBUG: Method merged with bridge method: keySet()Ljava/util/Set; */
    @Override // com.google.common.collect.ImmutableSortedMap, com.google.common.collect.ImmutableMap, java.util.Map
    public ImmutableSortedSet<K> keySet() {
        return this.keySet;
    }

    @Override // com.google.common.collect.ImmutableSortedMap, java.util.Map
    public int size() {
        return 0;
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public boolean isEmpty() {
        return true;
    }

    /* JADX DEBUG: Method merged with bridge method: values()Ljava/util/Collection; */
    @Override // com.google.common.collect.ImmutableSortedMap, com.google.common.collect.ImmutableMap, java.util.Map, java.util.SortedMap
    public ImmutableCollection<V> values() {
        return ImmutableList.of();
    }

    @Override // com.google.common.collect.ImmutableMap
    public String toString() {
        return "{}";
    }

    @Override // com.google.common.collect.ImmutableSortedMap, com.google.common.collect.ImmutableMap
    boolean isPartialView() {
        return false;
    }

    /* JADX DEBUG: Method merged with bridge method: entrySet()Ljava/util/Set; */
    @Override // com.google.common.collect.ImmutableSortedMap, com.google.common.collect.ImmutableMap, java.util.Map
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        return ImmutableSet.of();
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        throw new AssertionError("should never be called");
    }

    @Override // com.google.common.collect.ImmutableSortedMap, java.util.NavigableMap
    public ImmutableSortedMap<K, V> headMap(K k, boolean z) {
        Preconditions.checkNotNull(k);
        return this;
    }

    @Override // com.google.common.collect.ImmutableSortedMap, java.util.NavigableMap
    public ImmutableSortedMap<K, V> tailMap(K k, boolean z) {
        Preconditions.checkNotNull(k);
        return this;
    }

    @Override // com.google.common.collect.ImmutableSortedMap
    ImmutableSortedMap<K, V> createDescendingMap() {
        return new EmptyImmutableSortedMap(Ordering.from(comparator()).reverse(), this);
    }
}
