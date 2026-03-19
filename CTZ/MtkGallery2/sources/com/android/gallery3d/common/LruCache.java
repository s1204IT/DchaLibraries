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

    public LruCache(final int i) {
        this.mLruMap = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
                return size() > i;
            }
        };
    }

    private static class Entry<K, V> extends WeakReference<V> {
        K mKey;

        public Entry(K k, V v, ReferenceQueue<V> referenceQueue) {
            super(v, referenceQueue);
            this.mKey = k;
        }
    }

    private void cleanUpWeakMap() {
        Entry entry = (Entry) this.mQueue.poll();
        while (entry != null) {
            this.mWeakMap.remove(entry.mKey);
            entry = (Entry) this.mQueue.poll();
        }
    }

    public synchronized boolean containsKey(K k) {
        cleanUpWeakMap();
        return this.mWeakMap.containsKey(k);
    }

    public synchronized V put(K k, V v) {
        Entry<K, V> entryPut;
        cleanUpWeakMap();
        this.mLruMap.put(k, v);
        entryPut = this.mWeakMap.put(k, new Entry<>(k, v, this.mQueue));
        return entryPut == null ? null : entryPut.get();
    }

    public synchronized V get(K k) {
        cleanUpWeakMap();
        V v = this.mLruMap.get(k);
        if (v != null) {
            return v;
        }
        Entry<K, V> entry = this.mWeakMap.get(k);
        return entry == null ? null : entry.get();
    }
}
