package com.google.common.collect;

import java.util.Map;

final class EmptyImmutableBiMap extends ImmutableBiMap<Object, Object> {
    static final EmptyImmutableBiMap INSTANCE = new EmptyImmutableBiMap();

    private EmptyImmutableBiMap() {
    }

    @Override
    ImmutableSet<Map.Entry<Object, Object>> createEntrySet() {
        throw new AssertionError("should never be called");
    }

    @Override
    public ImmutableSet<Map.Entry<Object, Object>> entrySet() {
        return ImmutableSet.of();
    }

    @Override
    public Object get(Object obj) {
        return null;
    }

    @Override
    public ImmutableBiMap<Object, Object> inverse() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public ImmutableSet<Object> keySet() {
        return ImmutableSet.of();
    }

    Object readResolve() {
        return INSTANCE;
    }

    @Override
    public int size() {
        return 0;
    }
}
