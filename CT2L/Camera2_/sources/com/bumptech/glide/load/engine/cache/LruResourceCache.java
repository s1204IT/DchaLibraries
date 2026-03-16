package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.util.LruCache;

public class LruResourceCache extends LruCache<Key, Resource> implements MemoryCache {
    private MemoryCache.ResourceRemovedListener listener;

    @Override
    public Resource put(Key key, Resource resource) {
        return (Resource) super.put(key, resource);
    }

    @Override
    public Resource remove(Key key) {
        return (Resource) super.remove(key);
    }

    public LruResourceCache(int size) {
        super(size);
    }

    @Override
    public void setResourceRemovedListener(MemoryCache.ResourceRemovedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onItemRemoved(Key key, Resource item) {
        if (this.listener != null) {
            this.listener.onResourceRemoved(item);
        }
    }

    @Override
    protected int getSize(Resource item) {
        return item.getSize();
    }

    @Override
    public void trimMemory(int level) {
        if (level >= 60) {
            clearMemory();
        } else if (level >= 40) {
            trimToSize(getCurrentSize() / 2);
        }
    }
}
