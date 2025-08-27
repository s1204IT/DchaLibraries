package com.google.common.collect;

import java.util.concurrent.ConcurrentMap;

/* loaded from: classes.dex */
public abstract class ForwardingConcurrentMap<K, V> extends ForwardingMap<K, V> implements ConcurrentMap<K, V> {
    /* JADX DEBUG: Method merged with bridge method: delegate()Ljava/lang/Object; */
    /* JADX DEBUG: Method merged with bridge method: delegate()Ljava/util/Map; */
    @Override // com.google.common.collect.ForwardingMap, com.google.common.collect.ForwardingObject
    protected abstract ConcurrentMap<K, V> delegate();

    protected ForwardingConcurrentMap() {
    }

    @Override // java.util.Map, java.util.concurrent.ConcurrentMap
    public V putIfAbsent(K k, V v) {
        return delegate().putIfAbsent(k, v);
    }

    @Override // java.util.Map, java.util.concurrent.ConcurrentMap
    public boolean remove(Object obj, Object obj2) {
        return delegate().remove(obj, obj2);
    }

    @Override // java.util.Map, java.util.concurrent.ConcurrentMap
    public V replace(K k, V v) {
        return delegate().replace(k, v);
    }

    @Override // java.util.Map, java.util.concurrent.ConcurrentMap
    public boolean replace(K k, V v, V v2) {
        return delegate().replace(k, v, v2);
    }
}
