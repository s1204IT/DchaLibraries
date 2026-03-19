package sun.security.util;

import sun.security.util.Cache;

class NullCache<K, V> extends Cache<K, V> {
    static final Cache<Object, Object> INSTANCE = new NullCache();

    private NullCache() {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    public void put(K k, V v) {
    }

    @Override
    public V get(Object obj) {
        return null;
    }

    @Override
    public void remove(Object obj) {
    }

    @Override
    public void setCapacity(int i) {
    }

    @Override
    public void setTimeout(int i) {
    }

    @Override
    public void accept(Cache.CacheVisitor<K, V> cacheVisitor) {
    }
}
