package com.bumptech.glide.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache<T, Y> {
    private final LinkedHashMap<T, Y> cache = new LinkedHashMap<>(100, 0.75f, true);
    private int currentSize = 0;
    private final int initialMaxSize;
    private int maxSize;

    public LruCache(int size) {
        this.initialMaxSize = size;
        this.maxSize = size;
    }

    public void setSizeMultiplier(float multiplier) {
        if (multiplier < 0.0f) {
            throw new IllegalArgumentException("Multiplier must be >= 0");
        }
        this.maxSize = Math.round(this.initialMaxSize * multiplier);
        evict();
    }

    protected int getSize(Y item) {
        return 1;
    }

    protected void onItemRemoved(T key, Y item) {
    }

    public int getCurrentSize() {
        return this.currentSize;
    }

    public boolean contains(T key) {
        return this.cache.containsKey(key);
    }

    public Y get(T key) {
        return this.cache.get(key);
    }

    public Y put(T key, Y item) {
        int itemSize = getSize(item);
        if (itemSize >= this.maxSize) {
            onItemRemoved(key, item);
            return null;
        }
        Y result = this.cache.put(key, item);
        if (result != item) {
            this.currentSize += getSize(item);
            evict();
            return result;
        }
        return result;
    }

    public Y remove(T key) {
        Y value = this.cache.remove(key);
        if (value != null) {
            this.currentSize -= getSize(value);
        }
        return value;
    }

    public void clearMemory() {
        trimToSize(0);
    }

    protected void trimToSize(int size) {
        while (this.currentSize > size) {
            Map.Entry<T, Y> last = this.cache.entrySet().iterator().next();
            Y toRemove = last.getValue();
            this.currentSize -= getSize(toRemove);
            T key = last.getKey();
            this.cache.remove(key);
            onItemRemoved(key, toRemove);
        }
    }

    private void evict() {
        trimToSize(this.maxSize);
    }
}
