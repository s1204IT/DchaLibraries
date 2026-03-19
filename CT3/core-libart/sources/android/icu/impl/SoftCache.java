package android.icu.impl;

import java.util.concurrent.ConcurrentHashMap;

public abstract class SoftCache<K, V, D> extends CacheBase<K, V, D> {
    private ConcurrentHashMap<K, Object> map = new ConcurrentHashMap<>();

    @Override
    public final V getInstance(K k, D d) {
        V v = (V) this.map.get(k);
        if (v != 0) {
            if (!(v instanceof CacheValue)) {
                return v;
            }
            CacheValue cacheValue = (CacheValue) v;
            if (cacheValue.isNull()) {
                return null;
            }
            ?? r2 = cacheValue.get();
            if (r2 != null) {
                return r2;
            }
            return cacheValue.resetIfCleared(createInstance(k, d));
        }
        V vCreateInstance = createInstance(k, d);
        V v2 = (V) this.map.putIfAbsent(k, (vCreateInstance == null || !CacheValue.futureInstancesWillBeStrong()) ? CacheValue.getInstance(vCreateInstance) : vCreateInstance);
        if (v2 == 0) {
            return vCreateInstance;
        }
        if (!(v2 instanceof CacheValue)) {
            return v2;
        }
        return ((CacheValue) v2).resetIfCleared(vCreateInstance);
    }
}
