package com.android.documentsui.base;

@FunctionalInterface
public interface Lookup<K, V> {
    V lookup(K k);
}
