package com.google.common.collect;

import java.util.Map;

final class EmptyImmutableBiMap extends ImmutableBiMap<Object, Object> {
    static final EmptyImmutableBiMap INSTANCE = new EmptyImmutableBiMap();

    private EmptyImmutableBiMap() {
    }

    @Override
    public ImmutableBiMap<Object, Object> inverse() {
        return this;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Object get(Object obj) {
        return null;
    }

    @Override
    public ImmutableSet<Map.Entry<Object, Object>> entrySet() {
        return ImmutableSet.of();
    }

    @Override
    ImmutableSet<Map.Entry<Object, Object>> createEntrySet() {
        throw new AssertionError("should never be called");
    }

    @Override
    public ImmutableSet<Object> keySet() {
        return ImmutableSet.of();
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    Object readResolve() {
        return INSTANCE;
    }
}
