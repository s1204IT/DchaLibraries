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

    public LocaleObjectCache(int i, float f, int i2) {
        this._queue = new ReferenceQueue<>();
        this._map = new ConcurrentHashMap<>(i, f, i2);
    }

    public V get(K k) {
        V v;
        cleanStaleEntries();
        CacheEntry<K, V> cacheEntry = this._map.get(k);
        if (cacheEntry != null) {
            v = cacheEntry.get();
        } else {
            v = null;
        }
        if (v == null) {
            K kNormalizeKey = normalizeKey(k);
            V vCreateObject = createObject(kNormalizeKey);
            if (kNormalizeKey == null || vCreateObject == null) {
                return null;
            }
            CacheEntry<K, V> cacheEntry2 = new CacheEntry<>(kNormalizeKey, vCreateObject, this._queue);
            while (v == null) {
                cleanStaleEntries();
                CacheEntry<K, V> cacheEntryPutIfAbsent = this._map.putIfAbsent(kNormalizeKey, cacheEntry2);
                if (cacheEntryPutIfAbsent != null) {
                    v = cacheEntryPutIfAbsent.get();
                } else {
                    return vCreateObject;
                }
            }
            return v;
        }
        return v;
    }

    private void cleanStaleEntries() {
        while (true) {
            CacheEntry cacheEntry = (CacheEntry) this._queue.poll();
            if (cacheEntry != null) {
                this._map.remove(cacheEntry.getKey());
            } else {
                return;
            }
        }
    }

    protected K normalizeKey(K k) {
        return k;
    }

    private static class CacheEntry<K, V> extends SoftReference<V> {
        private K _key;

        CacheEntry(K k, V v, ReferenceQueue<V> referenceQueue) {
            super(v, referenceQueue);
            this._key = k;
        }

        K getKey() {
            return this._key;
        }
    }
}
