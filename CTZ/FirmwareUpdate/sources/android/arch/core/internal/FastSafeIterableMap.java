package android.arch.core.internal;

import android.arch.core.internal.SafeIterableMap;
import java.util.HashMap;
import java.util.Map;

public class FastSafeIterableMap<K, V> extends SafeIterableMap<K, V> {
    private HashMap<K, SafeIterableMap.Entry<K, V>> mHashMap = new HashMap<>();

    public Map.Entry<K, V> ceil(K k) {
        if (contains(k)) {
            return this.mHashMap.get(k).mPrevious;
        }
        return null;
    }

    public boolean contains(K k) {
        return this.mHashMap.containsKey(k);
    }

    @Override
    protected SafeIterableMap.Entry<K, V> get(K k) {
        return this.mHashMap.get(k);
    }

    @Override
    public V putIfAbsent(K k, V v) {
        SafeIterableMap.Entry<K, V> entry = get(k);
        if (entry != null) {
            return entry.mValue;
        }
        this.mHashMap.put(k, put(k, v));
        return null;
    }

    @Override
    public V remove(K k) {
        V v = (V) super.remove(k);
        this.mHashMap.remove(k);
        return v;
    }
}
