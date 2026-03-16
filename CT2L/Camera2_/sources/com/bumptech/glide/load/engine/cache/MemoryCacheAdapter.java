package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.cache.MemoryCache;

public class MemoryCacheAdapter implements MemoryCache {
    private MemoryCache.ResourceRemovedListener listener;

    @Override
    public void setSizeMultiplier(float multiplier) {
    }

    @Override
    public Resource remove(Key key) {
        return null;
    }

    @Override
    public Resource put(Key key, Resource resource) {
        this.listener.onResourceRemoved(resource);
        return null;
    }

    @Override
    public void setResourceRemovedListener(MemoryCache.ResourceRemovedListener listener) {
        this.listener = listener;
    }

    @Override
    public void clearMemory() {
    }

    @Override
    public void trimMemory(int level) {
    }
}
