package com.android.documentsui.base;

public final class DummyLookup<K, V> implements Lookup<K, V> {
    @Override
    public V lookup(K k) {
        return null;
    }
}
