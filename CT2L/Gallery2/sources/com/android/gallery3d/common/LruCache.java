package com.android.gallery3d.common;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<K, V> {
    private final HashMap<K, V> mLruMap;
    private final HashMap<K, Entry<K, V>> mWeakMap = new HashMap<>();
    private ReferenceQueue<V> mQueue = new ReferenceQueue<>();

    public LruCache(final int capacity) {
        this.mLruMap = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    private static class Entry<K, V> extends WeakReference<V> {
        K mKey;

        public Entry(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.mKey = key;
        }
    }

    private void cleanUpWeakMap() {
        Entry<K, V> entry = (Entry) this.mQueue.poll();
        while (entry != null) {
            this.mWeakMap.remove(entry.mKey);
            entry = (Entry) this.mQueue.poll();
        }
    }

    public synchronized boolean containsKey(K key) {
        cleanUpWeakMap();
        return this.mWeakMap.containsKey(key);
    }

    public synchronized V put(K k, V v) {
        Entry<K, V> entryPut;
        cleanUpWeakMap();
        this.mLruMap.put(k, v);
        entryPut = this.mWeakMap.put(k, new Entry<>(k, v, this.mQueue));
        return entryPut == null ? null : (V) entryPut.get();
    }

    public synchronized V get(K k) {
        V v;
        cleanUpWeakMap();
        v = this.mLruMap.get(k);
        if (v == null) {
            Entry<K, V> entry = this.mWeakMap.get(k);
            v = (V) (entry == null ? null : entry.get());
        }
        return v;
    }
}
