package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.cache.DiskCache;
import java.io.InputStream;

public class DiskCacheAdapter implements DiskCache {
    @Override
    public InputStream get(Key key) {
        return null;
    }

    @Override
    public void put(Key key, DiskCache.Writer writer) {
    }

    @Override
    public void delete(Key key) {
    }
}
