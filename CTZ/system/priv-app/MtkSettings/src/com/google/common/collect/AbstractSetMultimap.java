package com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
abstract class AbstractSetMultimap<K, V> extends AbstractMapBasedMultimap<K, V> implements SetMultimap<K, V> {
    private static final long serialVersionUID = 7431625294878419160L;

    /* JADX DEBUG: Method merged with bridge method: createCollection()Ljava/util/Collection; */
    @Override // com.google.common.collect.AbstractMapBasedMultimap
    abstract Set<V> createCollection();

    /* JADX DEBUG: Method merged with bridge method: entries()Ljava/util/Collection; */
    @Override // com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap
    public Set<Map.Entry<K, V>> entries() {
        return (Set) super.entries();
    }

    @Override // com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    @Override // com.google.common.collect.AbstractMultimap
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
