package android.icu.impl;

import java.util.concurrent.ConcurrentHashMap;

public abstract class SoftCache<K, V, D> extends CacheBase<K, V, D> {
    private ConcurrentHashMap<K, Object> map = new ConcurrentHashMap<>();

    @Override
    public final V getInstance(K k, D d) {
        Object cacheValue;
        V v = (V) this.map.get(k);
        if (v != 0) {
            if (!(v instanceof CacheValue)) {
                return v;
            }
            CacheValue cacheValue2 = (CacheValue) v;
            if (cacheValue2.isNull()) {
                return null;
            }
            V v2 = (V) cacheValue2.get();
            if (v2 != null) {
                return v2;
            }
            return (V) cacheValue2.resetIfCleared(createInstance(k, d));
        }
        V vCreateInstance = createInstance(k, d);
        if (vCreateInstance == null || !CacheValue.futureInstancesWillBeStrong()) {
            cacheValue = CacheValue.getInstance(vCreateInstance);
        } else {
            cacheValue = vCreateInstance;
        }
        V v3 = (V) this.map.putIfAbsent(k, cacheValue);
        if (v3 == 0) {
            return vCreateInstance;
        }
        if (!(v3 instanceof CacheValue)) {
            return v3;
        }
        return (V) ((CacheValue) v3).resetIfCleared(vCreateInstance);
    }
}
