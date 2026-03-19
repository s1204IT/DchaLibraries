package android.icu.impl.locale;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LocaleObjectCache<K, V> {
    private ConcurrentHashMap<K, CacheEntry<K, V>> _map;
    private ReferenceQueue<V> _queue;

    protected abstract V createObject(K k);

    public LocaleObjectCache() {
        this(16, 0.75f, 16);
    }

    public LocaleObjectCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this._queue = new ReferenceQueue<>();
        this._map = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    }

    public V get(K key) {
        V value = null;
        cleanStaleEntries();
        CacheEntry<K, V> entry = this._map.get(key);
        if (entry != null) {
            value = entry.get();
        }
        if (value == null) {
            K key2 = normalizeKey(key);
            V newVal = createObject(key2);
            if (key2 == null || newVal == null) {
                return null;
            }
            CacheEntry<K, V> newEntry = new CacheEntry<>(key2, newVal, this._queue);
            while (value == null) {
                cleanStaleEntries();
                CacheEntry<K, V> entry2 = this._map.putIfAbsent(key2, newEntry);
                if (entry2 == null) {
                    return newVal;
                }
                value = entry2.get();
            }
            return value;
        }
        return value;
    }

    private void cleanStaleEntries() {
        while (true) {
            CacheEntry<K, V> entry = (CacheEntry) this._queue.poll();
            if (entry == null) {
                return;
            } else {
                this._map.remove(entry.getKey());
            }
        }
    }

    protected K normalizeKey(K key) {
        return key;
    }

    private static class CacheEntry<K, V> extends SoftReference<V> {
        private K _key;

        CacheEntry(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this._key = key;
        }

        K getKey() {
            return this._key;
        }
    }
}
