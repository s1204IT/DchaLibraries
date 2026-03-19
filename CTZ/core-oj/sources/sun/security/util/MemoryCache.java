package sun.security.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import sun.security.util.Cache;

class MemoryCache<K, V> extends Cache<K, V> {
    private static final boolean DEBUG = false;
    private static final float LOAD_FACTOR = 0.75f;
    private final Map<K, CacheEntry<K, V>> cacheMap;
    private long lifetime;
    private int maxSize;
    private final ReferenceQueue<V> queue;

    private interface CacheEntry<K, V> {
        K getKey();

        V getValue();

        void invalidate();

        boolean isValid(long j);
    }

    public MemoryCache(boolean z, int i) {
        this(z, i, 0);
    }

    public MemoryCache(boolean z, int i, int i2) {
        this.maxSize = i;
        this.lifetime = i2 * 1000;
        if (z) {
            this.queue = new ReferenceQueue<>();
        } else {
            this.queue = null;
        }
        this.cacheMap = new LinkedHashMap(((int) (i / LOAD_FACTOR)) + 1, LOAD_FACTOR, true);
    }

    private void emptyQueue() {
        CacheEntry<K, V> cacheEntryRemove;
        if (this.queue == null) {
            return;
        }
        this.cacheMap.size();
        while (true) {
            CacheEntry<K, V> cacheEntry = (CacheEntry) this.queue.poll();
            if (cacheEntry != null) {
                K key = cacheEntry.getKey();
                if (key != null && (cacheEntryRemove = this.cacheMap.remove(key)) != null && cacheEntry != cacheEntryRemove) {
                    this.cacheMap.put(key, cacheEntryRemove);
                }
            } else {
                return;
            }
        }
    }

    private void expungeExpiredEntries() {
        emptyQueue();
        if (this.lifetime == 0) {
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        Iterator<CacheEntry<K, V>> it = this.cacheMap.values().iterator();
        while (it.hasNext()) {
            if (!it.next().isValid(jCurrentTimeMillis)) {
                it.remove();
            }
        }
    }

    @Override
    public synchronized int size() {
        expungeExpiredEntries();
        return this.cacheMap.size();
    }

    @Override
    public synchronized void clear() {
        if (this.queue != null) {
            Iterator<CacheEntry<K, V>> it = this.cacheMap.values().iterator();
            while (it.hasNext()) {
                it.next().invalidate();
            }
            while (this.queue.poll() != null) {
            }
        }
        this.cacheMap.clear();
    }

    @Override
    public synchronized void put(K k, V v) {
        emptyQueue();
        long jCurrentTimeMillis = 0;
        if (this.lifetime != 0) {
            jCurrentTimeMillis = this.lifetime + System.currentTimeMillis();
        }
        CacheEntry<K, V> cacheEntryPut = this.cacheMap.put(k, newEntry(k, v, jCurrentTimeMillis, this.queue));
        if (cacheEntryPut != null) {
            cacheEntryPut.invalidate();
            return;
        }
        if (this.maxSize > 0 && this.cacheMap.size() > this.maxSize) {
            expungeExpiredEntries();
            if (this.cacheMap.size() > this.maxSize) {
                Iterator<CacheEntry<K, V>> it = this.cacheMap.values().iterator();
                CacheEntry<K, V> next = it.next();
                it.remove();
                next.invalidate();
            }
        }
    }

    @Override
    public synchronized V get(Object obj) {
        emptyQueue();
        CacheEntry<K, V> cacheEntry = this.cacheMap.get(obj);
        if (cacheEntry == null) {
            return null;
        }
        long jCurrentTimeMillis = 0;
        if (this.lifetime != 0) {
            jCurrentTimeMillis = System.currentTimeMillis();
        }
        if (!cacheEntry.isValid(jCurrentTimeMillis)) {
            this.cacheMap.remove(obj);
            return null;
        }
        return cacheEntry.getValue();
    }

    @Override
    public synchronized void remove(Object obj) {
        emptyQueue();
        CacheEntry<K, V> cacheEntryRemove = this.cacheMap.remove(obj);
        if (cacheEntryRemove != null) {
            cacheEntryRemove.invalidate();
        }
    }

    @Override
    public synchronized void setCapacity(int i) {
        expungeExpiredEntries();
        if (i > 0 && this.cacheMap.size() > i) {
            Iterator<CacheEntry<K, V>> it = this.cacheMap.values().iterator();
            for (int size = this.cacheMap.size() - i; size > 0; size--) {
                CacheEntry<K, V> next = it.next();
                it.remove();
                next.invalidate();
            }
        }
        if (i <= 0) {
            i = 0;
        }
        this.maxSize = i;
    }

    @Override
    public synchronized void setTimeout(int i) {
        emptyQueue();
        this.lifetime = i > 0 ? ((long) i) * 1000 : 0L;
    }

    @Override
    public synchronized void accept(Cache.CacheVisitor<K, V> cacheVisitor) {
        expungeExpiredEntries();
        cacheVisitor.visit(getCachedEntries());
    }

    private Map<K, V> getCachedEntries() {
        HashMap map = new HashMap(this.cacheMap.size());
        for (CacheEntry<K, V> cacheEntry : this.cacheMap.values()) {
            map.put(cacheEntry.getKey(), cacheEntry.getValue());
        }
        return map;
    }

    protected CacheEntry<K, V> newEntry(K k, V v, long j, ReferenceQueue<V> referenceQueue) {
        if (referenceQueue != null) {
            return new SoftCacheEntry(k, v, j, referenceQueue);
        }
        return new HardCacheEntry(k, v, j);
    }

    private static class HardCacheEntry<K, V> implements CacheEntry<K, V> {
        private long expirationTime;
        private K key;
        private V value;

        HardCacheEntry(K k, V v, long j) {
            this.key = k;
            this.value = v;
            this.expirationTime = j;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public boolean isValid(long j) {
            boolean z = j <= this.expirationTime ? true : MemoryCache.DEBUG;
            if (!z) {
                invalidate();
            }
            return z;
        }

        @Override
        public void invalidate() {
            this.key = null;
            this.value = null;
            this.expirationTime = -1L;
        }
    }

    private static class SoftCacheEntry<K, V> extends SoftReference<V> implements CacheEntry<K, V> {
        private long expirationTime;
        private K key;

        SoftCacheEntry(K k, V v, long j, ReferenceQueue<V> referenceQueue) {
            super(v, referenceQueue);
            this.key = k;
            this.expirationTime = j;
        }

        @Override
        public K getKey() {
            return this.key;
        }

        @Override
        public V getValue() {
            return get();
        }

        @Override
        public boolean isValid(long j) {
            boolean z = (j > this.expirationTime || get() == null) ? MemoryCache.DEBUG : true;
            if (!z) {
                invalidate();
            }
            return z;
        }

        @Override
        public void invalidate() {
            clear();
            this.key = null;
            this.expirationTime = -1L;
        }
    }
}
