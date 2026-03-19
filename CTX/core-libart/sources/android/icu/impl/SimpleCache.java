package android.icu.impl;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleCache<K, V> implements ICUCache<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private volatile Reference<Map<K, V>> cacheRef;
    private int capacity;
    private int type;

    public SimpleCache() {
        this.cacheRef = null;
        this.type = 0;
        this.capacity = 16;
    }

    public SimpleCache(int i) {
        this(i, 16);
    }

    public SimpleCache(int i, int i2) {
        this.cacheRef = null;
        this.type = 0;
        this.capacity = 16;
        if (i == 1) {
            this.type = i;
        }
        if (i2 > 0) {
            this.capacity = i2;
        }
    }

    @Override
    public V get(Object obj) {
        Map<K, V> map;
        Reference<Map<K, V>> reference = this.cacheRef;
        if (reference != null && (map = reference.get()) != null) {
            return map.get(obj);
        }
        return null;
    }

    @Override
    public void put(K k, V v) {
        Map<K, V> mapSynchronizedMap;
        Reference<Map<K, V>> softReference;
        Reference<Map<K, V>> reference = this.cacheRef;
        if (reference != null) {
            mapSynchronizedMap = reference.get();
        } else {
            mapSynchronizedMap = null;
        }
        if (mapSynchronizedMap == null) {
            mapSynchronizedMap = Collections.synchronizedMap(new HashMap(this.capacity));
            if (this.type == 1) {
                softReference = new WeakReference<>(mapSynchronizedMap);
            } else {
                softReference = new SoftReference<>(mapSynchronizedMap);
            }
            this.cacheRef = softReference;
        }
        mapSynchronizedMap.put(k, v);
    }

    @Override
    public void clear() {
        this.cacheRef = null;
    }
}
