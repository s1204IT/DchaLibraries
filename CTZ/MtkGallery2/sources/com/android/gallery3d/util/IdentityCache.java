package com.android.gallery3d.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class IdentityCache<K, V> {
    private final HashMap<K, Entry<K, V>> mWeakMap = new HashMap<>();
    private ReferenceQueue<V> mQueue = new ReferenceQueue<>();

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

    public synchronized V put(K k, V v) {
        Entry<K, V> entryPut;
        cleanUpWeakMap();
        entryPut = this.mWeakMap.put(k, new Entry<>(k, v, this.mQueue));
        return entryPut == null ? null : entryPut.get();
    }

    public synchronized V get(K k) {
        Entry<K, V> entry;
        cleanUpWeakMap();
        entry = this.mWeakMap.get(k);
        return entry == null ? null : entry.get();
    }
}
