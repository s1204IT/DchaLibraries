package libcore.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class BasicLruCache<K, V> {
    private final LinkedHashMap<K, V> map;
    private final int maxSize;

    public BasicLruCache(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = i;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    public final V get(K k) {
        if (k == null) {
            throw new NullPointerException("key == null");
        }
        synchronized (this) {
            V v = this.map.get(k);
            if (v != null) {
                return v;
            }
            V vCreate = create(k);
            synchronized (this) {
                if (vCreate != null) {
                    try {
                        this.map.put(k, vCreate);
                        trimToSize(this.maxSize);
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
            return vCreate;
        }
    }

    public final synchronized V put(K k, V v) {
        V vPut;
        if (k == null) {
            throw new NullPointerException("key == null");
        }
        if (v == null) {
            throw new NullPointerException("value == null");
        }
        vPut = this.map.put(k, v);
        trimToSize(this.maxSize);
        return vPut;
    }

    private void trimToSize(int i) {
        while (this.map.size() > i) {
            Map.Entry entryEldest = this.map.eldest();
            Object key = entryEldest.getKey();
            Object value = entryEldest.getValue();
            this.map.remove(key);
            entryEvicted(key, value);
        }
    }

    protected void entryEvicted(K k, V v) {
    }

    protected V create(K k) {
        return null;
    }

    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap(this.map);
    }

    public final synchronized void evictAll() {
        trimToSize(0);
    }
}
