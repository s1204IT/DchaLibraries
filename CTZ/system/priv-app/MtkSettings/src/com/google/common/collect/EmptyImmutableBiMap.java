package com.google.common.collect;

import java.util.Map;

/* loaded from: classes.dex */
final class EmptyImmutableBiMap extends ImmutableBiMap<Object, Object> {
    static final EmptyImmutableBiMap INSTANCE = new EmptyImmutableBiMap();

    private EmptyImmutableBiMap() {
    }

    @Override // com.google.common.collect.ImmutableBiMap
    public ImmutableBiMap<Object, Object> inverse() {
        return this;
    }

    @Override // java.util.Map
    public int size() {
        return 0;
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public boolean isEmpty() {
        return true;
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public Object get(Object obj) {
        return null;
    }

    /* JADX DEBUG: Method merged with bridge method: entrySet()Ljava/util/Set; */
    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public ImmutableSet<Map.Entry<Object, Object>> entrySet() {
        return ImmutableSet.of();
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<Map.Entry<Object, Object>> createEntrySet() {
        throw new AssertionError("should never be called");
    }

    /* JADX DEBUG: Method merged with bridge method: keySet()Ljava/util/Set; */
    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public ImmutableSet<Object> keySet() {
        return ImmutableSet.of();
    }

    @Override // com.google.common.collect.ImmutableMap
    boolean isPartialView() {
        return false;
    }

    Object readResolve() {
        return INSTANCE;
    }
}
