package sun.util.locale;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class LocaleObjectCache<K, V> {
    private ConcurrentMap<K, CacheEntry<K, V>> map;
    private ReferenceQueue<V> queue;

    protected abstract V createObject(K k);

    public LocaleObjectCache() {
        this(16, 0.75f, 16);
    }

    public LocaleObjectCache(int i, float f, int i2) {
        this.queue = new ReferenceQueue<>();
        this.map = new ConcurrentHashMap(i, f, i2);
    }

    public V get(K k) {
        V v;
        cleanStaleEntries();
        CacheEntry<K, V> cacheEntry = this.map.get(k);
        if (cacheEntry != null) {
            v = cacheEntry.get();
        } else {
            v = null;
        }
        if (v == null) {
            V vCreateObject = createObject(k);
            K kNormalizeKey = normalizeKey(k);
            if (kNormalizeKey == null || vCreateObject == null) {
                return null;
            }
            CacheEntry<K, V> cacheEntry2 = new CacheEntry<>(kNormalizeKey, vCreateObject, this.queue);
            CacheEntry<K, V> cacheEntryPutIfAbsent = this.map.putIfAbsent(kNormalizeKey, cacheEntry2);
            if (cacheEntryPutIfAbsent != null) {
                V v2 = cacheEntryPutIfAbsent.get();
                if (v2 == null) {
                    this.map.put(kNormalizeKey, cacheEntry2);
                    return vCreateObject;
                }
                return v2;
            }
            return vCreateObject;
        }
        return v;
    }

    protected V put(K k, V v) {
        CacheEntry<K, V> cacheEntryPut = this.map.put(k, new CacheEntry<>(k, v, this.queue));
        if (cacheEntryPut == null) {
            return null;
        }
        return cacheEntryPut.get();
    }

    private void cleanStaleEntries() {
        while (true) {
            CacheEntry cacheEntry = (CacheEntry) this.queue.poll();
            if (cacheEntry != null) {
                this.map.remove(cacheEntry.getKey());
            } else {
                return;
            }
        }
    }

    protected K normalizeKey(K k) {
        return k;
    }

    private static class CacheEntry<K, V> extends SoftReference<V> {
        private K key;

        CacheEntry(K k, V v, ReferenceQueue<V> referenceQueue) {
            super(v, referenceQueue);
            this.key = k;
        }

        K getKey() {
            return this.key;
        }
    }
}
