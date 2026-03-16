package libcore.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class BasicLruCache<K, V> {
    private final LinkedHashMap<K, V> map;
    private final int maxSize;

    public BasicLruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    public final synchronized V get(K key) {
        V result;
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        V result2 = this.map.get(key);
        if (result2 != null) {
            result = result2;
        } else {
            V result3 = create(key);
            if (result3 != null) {
                this.map.put(key, result3);
                trimToSize(this.maxSize);
            }
            result = result3;
        }
        return result;
    }

    public final synchronized V put(K key, V value) {
        V previous;
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        previous = this.map.put(key, value);
        trimToSize(this.maxSize);
        return previous;
    }

    private void trimToSize(int maxSize) {
        while (this.map.size() > maxSize) {
            Map.Entry<K, V> toEvict = this.map.eldest();
            K key = toEvict.getKey();
            V value = toEvict.getValue();
            this.map.remove(key);
            entryEvicted(key, value);
        }
    }

    protected void entryEvicted(K key, V value) {
    }

    protected V create(K key) {
        return null;
    }

    public final synchronized Map<K, V> snapshot() {
        return new LinkedHashMap(this.map);
    }

    public final synchronized void evictAll() {
        trimToSize(0);
    }
}
